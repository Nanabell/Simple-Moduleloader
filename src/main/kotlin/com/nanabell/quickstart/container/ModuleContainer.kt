package com.nanabell.quickstart.container

import com.nanabell.quickstart.*
import com.nanabell.quickstart.config.AdaptableConfigProvider
import com.nanabell.quickstart.config.SystemConfigProvider
import com.nanabell.quickstart.phase.ConstructionPhase
import com.nanabell.quickstart.phase.LoadingStatus
import com.nanabell.quickstart.phase.ModulePhase
import com.nanabell.quickstart.strategy.RecursiveResolveStrategy
import com.nanabell.quickstart.strategy.ResolveStrategy
import com.nanabell.quickstart.util.*
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.ConfigurationOptions
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Suppress("unused")
abstract class ModuleContainer protected constructor(
    private val configProvider: AdaptableConfigProvider,
    private val logger: Logger,
    private val onPreEnable: () -> Unit,
    private val onEnable: () -> Unit,
    private val onPostEnable: () -> Unit,
    private val resolveStrategy: ResolveStrategy,
    private val moduleConfigKey: String,
    private val moduleConfigHeader: String?
) {

    private val modules: LinkedHashMap<ModuleMeta, Module> = LinkedHashMap()
    private val moduleMetas: LinkedList<ModuleMeta> = LinkedList()
    private val disabledModules: MutableSet<String> = HashSet()

    private var currentPhase = ConstructionPhase.INITIALIZED

    /**
     * Start Discovering Modules, load meta information,
     * Resolve Dependency Order and load The ModuleConfigHeader
     *
     * @throws ModuleDiscoveryException If anything went wrong while discovering
     */
    @Throws(ModuleDiscoveryException::class)
    fun startDiscover() {
        try {
            checkPhase(ConstructionPhase.INITIALIZED)
            val modules = discoverModules()

            // Load Meta Information from RegisterModule Annotation
            val discovered: MutableList<ModuleMeta> = mutableListOf()
            for (module in modules) {
                val registerModule = module.findAnnotation<RegisterModule>()
                if (registerModule == null) {
                    logger.warn("Module Class $module is Missing the @${RegisterModule::class.simpleName} Annotation and will be ignored!")
                    continue
                }

                // Check for Unique Module Id
                val meta = ModuleMeta(module, registerModule)
                if (discovered.any { it.id == meta.id }) {
                    return exitWithError(ModuleAlreadyRegistered(meta.moduleClass, meta.id))
                }

                discovered.add(meta)
            }

            // Resolve parents
            discovered.forEach { meta ->
                meta.dependencies.forEach {dependency ->
                    val child = discovered.firstOrNull { it.id == dependency }  ?: throw DependencyNotFoundException(meta.moduleClass, dependency)
                    child.parents.add(meta.id)
                }
            }

            // Resolve Dependency Build Order
            moduleMetas.addAll(resolveStrategy.resolveModuleDependencies(discovered))

            // Build Header Config Adapter
            val moduleSpecs = this.moduleMetas.filter { !it.required }
            configProvider.attachModuleStatusConfig(
                moduleSpecs,
                this.moduleConfigKey,
                this.moduleConfigHeader ?: "ENABLED; DISABLED; FORCE_ENABLED"
            )

            configProvider.getModuleStatusConfig()?.getConfig()?.forEach {
                val meta = getDiscoveredUnchecked(it.key)
                meta.status = it.value
            }

            cascadeDisable()
            checkPhase(ConstructionPhase.DISCOVERING)
        } catch (e: ModuleDiscoveryException) {
            throw e
        } catch (e: Exception) {
            throw ModuleDiscoveryException(e)
        }
    }

    /**
     * Get a set of all discovered Modules.
     * It is up to the implementation to determinate what classes should be included
     *
     * @throws ModuleDiscoveryException If discovery fails
     * @return Set of ModuleClasses
     */
    @Throws(ModuleDiscoveryException::class)
    protected abstract fun discoverModules(): Set<KClass<out Module>>

    /**
     * Construct all modules and enable them in the discovered Order.
     *
     * **Once this method has been called modules can no longer be removed or disabled**
     *
     * @param failOnError Cancel loading if a single module fails? Else this will disable all dependent Modules as well and throw an Error if all Modules are disabled
     * @throws ModuleDiscoveryException SubExceptions if anything critical went wrong and we cant construct the ModuleContainer
     */
    @Throws(ModuleDiscoveryException::class)
    fun loadModules(failOnError: Boolean) {
        try {
            checkPhase(ConstructionPhase.DISCOVERED)

            // Construct Modules
            moduleMetas.filter { it.status != LoadingStatus.DISABLED }.forEach {

                try {
                    modules[it] = constructModule(it)
                    it.phase = ModulePhase.CONSTRUCTED
                } catch (e: ModuleConstructionException) {
                    moduleError(it, failOnError, e)
                }
            }

            // Construction has finished
            if (modules.isEmpty()) {
                return exitWithError(NoModulesConstructedException())
            }

            // Load Module Configs
            modules.filter { it.value is ConfigModule<*> }.forEach { (meta, module) ->
                try {
                    attachConfig(meta.id, module as ConfigModule<*>)
                } catch (e: ModuleAlreadyAttachedException) {
                    moduleError(meta, failOnError, e)
                }
            }

            configProvider.createDefaultConfigs()

            // Module PreEnable
            onPreEnable.invoke()
            for ((meta, module) in modules.entries) {

                // If module is errored simply skip it
                if (meta.phase == ModulePhase.ERRORED || meta.status == LoadingStatus.DISABLED)
                    continue

                try {
                    module.preEnable()
                } catch (e: ModuleLoadingException) {
                    moduleError(meta, failOnError, e)
                }
            }

            // Check External Dependencies
            cascadeDisable()
            modules.forEach { (meta, module) ->

                // If module is errored simply skip it
                if (meta.phase == ModulePhase.ERRORED || meta.status == LoadingStatus.DISABLED)
                    return@forEach

                try {
                    module.checkExternalDependencies()
                } catch (e: MissingDependencyException) {
                    moduleError(meta, failOnError, e)
                }
            }

            // Module Enable
            cascadeDisable()
            onEnable.invoke()
            for ((meta, module) in modules.entries) {

                // If module is errored simply skip it
                if (meta.phase == ModulePhase.ERRORED || meta.status == LoadingStatus.DISABLED)
                    continue

                try {
                    module.onEnable()
                    meta.phase = ModulePhase.ENABLED
                } catch (e: ModuleLoadingException) {
                    moduleError(meta, failOnError, e)
                }
            }

            cascadeDisable()
            onPostEnable.invoke()
            for ((meta, module) in modules.entries) {

                // If module is errored simply skip it
                if (meta.phase == ModulePhase.ERRORED || meta.status == LoadingStatus.DISABLED)
                    continue

                try {
                    module.postEnable()
                } catch (e: ModuleLoadingException) {
                    moduleError(meta, failOnError, e)
                }
            }

            if (moduleMetas.none { it.phase == ModulePhase.ENABLED }) {
                return exitWithError(NoModulesReadyException())
            }

            currentPhase.next()
        } catch (e: ModuleDiscoveryException) {
            throw e
        } catch (e: Exception) {
            return exitWithError(ModuleDiscoveryException(e))
        }
    }

    /**
     * Walk the [moduleMetas] in reverse order and cascade disable and modules where any dependency is marked as Disabled
     *
     * This will update the Meta information in the [moduleMetas] Map
     * as well as add the Module ids to the [disabledModules] Set
     */
    private fun cascadeDisable() {
        val starters = moduleMetas.filter { it.dependencies.isEmpty() }
        starters.forEach {
            disableNextStep(it)
        }
    }

    private fun disableNextStep(meta: ModuleMeta) {

        if (meta.status != LoadingStatus.DISABLED) {
            meta.dependencies.forEach {
                val dependency = getDiscoveredUnchecked(it)

                if (dependency.status == LoadingStatus.DISABLED) {
                    if (meta.required)
                        return exitWithError(IllegalModuleDisableException(meta.id))

                    meta.status = LoadingStatus.DISABLED
                    disabledModules.add(meta.id)

                    return@forEach
                }
            }
        }

        meta.parents.forEach {
            val parent = getDiscoveredUnchecked(it)
            disableNextStep(parent)
        }
    }

    /**
     * Create a new Instance of the Module Class
     * its up to the implementation to figure out how to best construct the Module
     *
     * @throws ModuleConstructionException If the construction has failed
     * @return Constructed Module
     */
    @Throws(ModuleConstructionException::class)
    protected abstract fun constructModule(meta: ModuleMeta): Module

    @Throws(IOException::class)
    fun refreshSystemConfig() {
        configProvider.save(true)
    }

    @Throws(IOException::class)
    fun reloadSystemConfig() {
        configProvider.reload()
    }

    private fun attachConfig(name: String, module: ConfigModule<*>) {
        configProvider.attachConfigAdapter(name, module.getConfigAdapter(), null)
    }

    private fun detachConfig(name: String) {
        configProvider.detachConfigAdapter(name)
    }


    private fun setDisabled(meta: ModuleMeta) {
        meta.status = LoadingStatus.DISABLED
        modules.remove(meta)
    }

    private fun setErrored(meta: ModuleMeta) {
        meta.phase = ModulePhase.ERRORED
        modules.remove(meta)
    }

    private fun getDiscoveredUnchecked(key: String): ModuleMeta {
        return moduleMetas.first { it.id == key }
    }

    private fun checkPhase(requirement: ConstructionPhase) {
        if (currentPhase != requirement)
            throw IllegalStateException("Phase Check failed! Expected: $requirement, Actual: $currentPhase")

        currentPhase = currentPhase.next()
    }

    private fun exitWithError(e: ModuleDiscoveryException) {
        currentPhase = ConstructionPhase.ERRORED
        logger.error(e.message, e)

        throw e
    }

    private fun moduleError(meta: ModuleMeta, failOnError: Boolean, e: Exception) {
        setDisabled(meta)
        setErrored(meta)

        if (failOnError)
            throw e
    }


    abstract class Builder<R : ModuleContainer, T : Builder<R, T>> {

        private lateinit var configurationLoader: ConfigurationLoader<out ConfigurationNode>
        private var configTransformer: (ConfigurationOptions) -> ConfigurationOptions = { x -> x }

        protected lateinit var logger: Logger
        protected lateinit var moduleConfigKey: String
        protected lateinit var configProvider: AdaptableConfigProvider
        protected var resolveStrategy: ResolveStrategy = RecursiveResolveStrategy()
        protected var onPreEnable: () -> Unit = {}
        protected var onEnable: () -> Unit = {}
        protected var onPostEnable: () -> Unit = {}

        protected var moduleConfigHeader: String? = null

        protected abstract fun getThis(): T

        fun setConfigurationLoader(configurationLoader: ConfigurationLoader<out ConfigurationNode>): T {
            this.configurationLoader = configurationLoader

            return getThis()
        }

        fun setConfigurationOptionsTransformer(configTransformer: (ConfigurationOptions) -> ConfigurationOptions): T {
            this.configTransformer = configTransformer

            return getThis()
        }

        fun setResolveStrategy(resolveStrategy: ResolveStrategy): T {
            this.resolveStrategy = resolveStrategy

            return getThis()
        }

        fun setAdaptableConfigProvider(configProvider: AdaptableConfigProvider) {
            this.configProvider = configProvider
        }

        fun setLogger(logger: Logger): T {
            this.logger = logger

            return getThis()
        }

        fun setOnPreEnable(onPreEnable: () -> Unit): T {
            this.onPreEnable = onPreEnable

            return getThis()
        }

        fun setOnEnable(onEnable: () -> Unit): T {
            this.onEnable = onEnable

            return getThis()
        }

        fun setOnPostEnable(onPostEnable: () -> Unit): T {
            this.onPostEnable = onPostEnable

            return getThis()
        }

        fun setModuleConfigKey(moduleConfigKey: String): T {
            this.moduleConfigKey = moduleConfigKey

            return getThis()
        }

        fun setModuleConfigHeader(moduleConfigHeader: String): T {
            this.moduleConfigHeader = moduleConfigHeader

            return getThis()
        }

        protected abstract fun build(): R

        @Throws(Exception::class)
        fun build(startDiscover: Boolean = false): R {
            checkBuild()

            val build = build()
            if (startDiscover)
                build.startDiscover()

            return build
        }

        private fun checkBuild() {
            if (!::configurationLoader.isInitialized && !::configProvider.isInitialized)
                throw ModuleDiscoveryException("ConfigurationLoader or ConfigProvider must be set!")

            if (!::moduleConfigKey.isInitialized)
                throw ModuleDiscoveryException("Module Config Key must be set!")

            if (!::logger.isInitialized)
                logger = LoggerFactory.getLogger(this::class.java)

            if (!::configProvider.isInitialized)
                configProvider = SystemConfigProvider(configurationLoader, configTransformer)
        }
    }
}
