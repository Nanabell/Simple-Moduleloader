package com.nanabell.quickstart

import com.nanabell.quickstart.config.AdaptableConfigProvider
import com.nanabell.quickstart.config.SystemConfigProvider
import com.nanabell.quickstart.phase.ConstructionPhase
import com.nanabell.quickstart.phase.ModulePhase
import com.nanabell.quickstart.util.*
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.ConfigurationOptions
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

abstract class ModuleContainer protected constructor(
    private val configProvider: AdaptableConfigProvider,
    private val logger: Logger,
    private val onPreEnable: () -> Unit,
    private val onEnable: () -> Unit,
    private val onPostEnable: () -> Unit,
    private val moduleConfigKey: String,
    private val moduleConfigHeader: String?
) {

    private val modules: LinkedHashMap<String, Module<*>> = LinkedHashMap()
    private val moduleMetas: LinkedHashMap<String, ModuleMeta<*>> = LinkedHashMap()
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
            val discovered: MutableMap<String, ModuleMeta<*>> = mutableMapOf()
            for (module in modules) {
                val registerModule = module.findAnnotation<RegisterModule>()
                if (registerModule == null) {
                    logger.warn("Module Class $module is Missing the @${RegisterModule::class.simpleName} Annotation and will be ignored!")
                    continue
                }

                // Check for Unique Module Id
                val meta = ModuleMeta(module, registerModule)
                if (discovered.containsKey(meta.id)) {
                    return exitWithError(ModuleAlreadyRegistered(meta.moduleClass, meta.id))
                }

                discovered[meta.id] = meta
            }

            // Resolve parents
            discovered.values.forEach { meta ->
                meta.dependencies.forEach {
                    val child = discovered[it] ?: throw DependencyNotFoundException(meta.moduleClass, it)
                    child.parents.add(meta.id)
                }
            }

            // Resolve Dependency Build Order
            resolveDependencyOrder(discovered)

            // Build Header Config Adapter
            val moduleSpecs = this.moduleMetas.filter { !it.value.required }
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
    protected abstract fun discoverModules(): Set<KClass<out Module<*>>>

    /**
     * Walk the dependency Tree for every module in the [discovered] map
     * and add them to [moduleMetas] in order of dependency.
     *
     * @param discovered Map of all discovered modules
     *
     * @throws DependencyNotFoundException If a Dependency of a Module could not be found in the [discovered] map
     * @throws CircularDependencyException If a dependency self references over other dependencies
     */
    @Throws(DependencyNotFoundException::class, CircularDependencyException::class)
    private fun resolveDependencyOrder(discovered: Map<String, ModuleMeta<*>>) {
        discovered.forEach {
            resolveDependencyStep(discovered, it.value, mutableSetOf())
        }
    }

    /**
     * Recursive call to walk from the bottom of the Tree back up
     * adding dependencies to [moduleMetas] as we go level back up
     *
     * @param discovered Map of all discovered modules
     * @param meta Meta for the current module in question
     * @param visited Set containing all module ids we've already visited
     *
     * @throws DependencyNotFoundException If a Dependency of a Module could not be found in the [discovered] map
     * @throws CircularDependencyException If a dependency self references over other dependencies
     */
    @Throws(DependencyNotFoundException::class, CircularDependencyException::class)
    private fun resolveDependencyStep(
        discovered: Map<String, ModuleMeta<*>>,
        meta: ModuleMeta<*>,
        visited: MutableSet<String>
    ) {
        if (!visited.contains(meta.id)) {
            visited.add(meta.id)

            meta.softDependencies.forEach {
                val softDependency = discovered[it] ?: throw DependencyNotFoundException(meta.moduleClass, it)
                resolveDependencyStep(discovered, softDependency, visited)
            }

            meta.dependencies.forEach {
                val dependency = discovered[it] ?: throw DependencyNotFoundException(meta.moduleClass, it)
                resolveDependencyStep(discovered, dependency, visited)
            }

            this.moduleMetas[meta.id] = meta
        } else {
            if (!moduleMetas.containsKey(meta.id))
                throw CircularDependencyException(meta.moduleClass)
        }
    }

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
            moduleMetas.filter { it.value.status != LoadingStatus.DISABLED }.values.forEach {

                try {
                    modules[it.id] = constructModule(it)
                    it.phase = ModulePhase.CONSTRUCTED
                } catch (e: ModuleConstructionException) {
                    moduleError(it.id, failOnError, e)
                }
            }

            // Construction has finished
            if (modules.isEmpty()) {
                return exitWithError(NoModulesConstructedException())
            }

            // Load Module Configs
            modules.forEach {
                try {
                    attachConfig(it.key, it.value)
                } catch (e: ModuleAlreadyAttachedException) {
                    moduleError(it.key, failOnError, e)
                }
            }

            configProvider.createDefaultConfigs()

            // Module PreEnable
            onPreEnable.invoke()
            for (key in modules.keys) {
                val meta = getDiscoveredUnchecked(key)

                // If module is errored simply skip it
                if (meta.phase == ModulePhase.ERRORED || meta.status == LoadingStatus.DISABLED)
                    continue

                try {
                    getModuleUnchecked(key).preEnable()
                } catch (e: ModuleLoadingException) {
                    moduleError(key, failOnError, e)
                }
            }

            // Check External Dependencies
            cascadeDisable()
            modules.forEach {
                val meta = getDiscoveredUnchecked(it.key)

                // If module is errored simply skip it
                if (meta.phase == ModulePhase.ERRORED || meta.status == LoadingStatus.DISABLED)
                    return@forEach

                try {
                    it.value.checkExternalDependencies()
                } catch (e: MissingDependencyException) {
                    moduleError(it.key, failOnError, e)
                }
            }

            // Module Enable
            cascadeDisable()
            onEnable.invoke()
            for (key in modules.keys) {
                val meta = getDiscoveredUnchecked(key)

                // If module is errored simply skip it
                if (meta.phase == ModulePhase.ERRORED || meta.status == LoadingStatus.DISABLED)
                    continue

                try {
                    getModuleUnchecked(key).onEnable()
                    meta.phase = ModulePhase.ENABLED
                } catch (e: ModuleLoadingException) {
                    moduleError(key, failOnError, e)
                }
            }

            cascadeDisable()
            onPostEnable.invoke()
            for (key in modules.keys) {
                val meta = getDiscoveredUnchecked(key)

                // If module is errored simply skip it
                if (meta.phase == ModulePhase.ERRORED || meta.status == LoadingStatus.DISABLED)
                    continue

                try {
                    getModuleUnchecked(key).postEnable()
                } catch (e: ModuleLoadingException) {
                    moduleError(key, failOnError, e)
                }
            }

            if (moduleMetas.filter { it.value.phase == ModulePhase.ENABLED }.isEmpty()) {
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
        val starters = moduleMetas.values.filter { it.dependencies.isEmpty() }
        starters.forEach {
            disableNextStep(it)
        }
    }

    private fun disableNextStep(meta: ModuleMeta<*>) {

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
    protected abstract fun constructModule(meta: ModuleMeta<*>): Module<*>

    @Throws(IOException::class)
    fun refreshSystemConfig() {
        configProvider.save(true)
    }

    @Throws(IOException::class)
    fun reloadSystemConfig() {
        configProvider.reload()
    }

    private fun attachConfig(name: String, module: Module<*>) {
        configProvider.attachConfigAdapter(name, module.getConfigAdapter(), null)
    }

    private fun detachConfig(name: String) {
        configProvider.detachConfigAdapter(name)
    }


    private fun setDisabled(key: String) {
        moduleMetas[key]?.status = LoadingStatus.DISABLED
        modules.remove(key)
    }

    private fun setErrored(key: String) {
        moduleMetas[key]?.phase = ModulePhase.ERRORED
        modules.remove(key)
    }

    private fun getDiscoveredUnchecked(key: String): ModuleMeta<*> {
        return moduleMetas[key]
            ?: throw IllegalAccessException("Attempted to Access $key from discoveredModules while key does not exist!")
    }

    private fun getModuleUnchecked(key: String): Module<*> {
        return modules[key]
            ?: throw IllegalAccessException("Attempted to Access $key from discoveredModules while key does not exist!")
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

    private fun moduleError(key: String, failOnError: Boolean, e: Exception) {
        setDisabled(key)
        setErrored(key)

        if (failOnError)
            throw e
    }


    abstract class Builder<R : ModuleContainer, T : Builder<R, T>> {

        protected lateinit var logger: Logger
        protected lateinit var moduleConfigKey: String
        protected lateinit var configurationLoader: ConfigurationLoader<out ConfigurationNode>
        protected lateinit var configProvider: AdaptableConfigProvider

        protected var configTransformer: (ConfigurationOptions) -> ConfigurationOptions = { x -> x }
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
