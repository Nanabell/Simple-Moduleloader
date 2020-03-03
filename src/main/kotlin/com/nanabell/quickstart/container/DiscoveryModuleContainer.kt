package com.nanabell.quickstart.container

import com.google.common.collect.ImmutableSet
import com.nanabell.quickstart.Module
import com.nanabell.quickstart.ModuleMeta
import com.nanabell.quickstart.config.AdaptableConfigProvider
import com.nanabell.quickstart.loader.DefaultModuleConstructor
import com.nanabell.quickstart.loader.ModuleConstructor
import com.nanabell.quickstart.strategy.GoogleDiscoverStrategy
import com.nanabell.quickstart.strategy.DiscoverStrategy
import com.nanabell.quickstart.strategy.ResolveStrategy
import com.nanabell.quickstart.util.ModuleConstructionException
import com.nanabell.quickstart.util.ModuleDiscoveryException
import org.slf4j.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("unused")
class DiscoveryModuleContainer private constructor(
    configProvider: AdaptableConfigProvider,
    logger: Logger,
    onPreEnable: () -> Unit,
    onEnable: () -> Unit,
    onPostEnable: () -> Unit,
    moduleConfigKey: String,
    moduleConfigHeader: String?,
    resolveStrategy: ResolveStrategy,
    private val classLoader: ClassLoader,
    private val basePackage: String,
    private val constructor: ModuleConstructor,
    private val discoverStrategy: DiscoverStrategy
) : ModuleContainer(
    configProvider,
    logger,
    onPreEnable,
    onEnable,
    onPostEnable,
    resolveStrategy,
    moduleConfigKey,
    moduleConfigHeader
) {

    private val loadedClasses: MutableSet<KClass<*>> = HashSet()

    @Suppress("UNCHECKED_CAST")
    override fun discoverModules(): Set<KClass<out Module>> {
        loadedClasses.addAll(this.discoverStrategy.discover(this.basePackage, this.classLoader))

        val modules = loadedClasses.filter { clazz -> clazz.isSubclassOf(Module::class) }
            .map { it as KClass<out Module> }

        if (modules.isEmpty()) {
            throw ModuleDiscoveryException("No Modules were found!")
        }

        return modules.toSet()
    }

    @Throws(ModuleConstructionException::class)
    override fun constructModule(meta: ModuleMeta): Module {
        return this.constructor.createInstance(meta.moduleClass)
    }

    fun getLoadedClasses(): Set<KClass<*>> {
        return ImmutableSet.copyOf(loadedClasses)
    }

    companion object {
        fun builder() = Builder()
    }

    @Suppress("unused")
    class Builder : ModuleContainer.Builder<DiscoveryModuleContainer, Builder>() {

        private lateinit var packageToScan: String
        private lateinit var classLoader: ClassLoader

        private var constructor: ModuleConstructor = DefaultModuleConstructor()
        private var discoverStrategy: DiscoverStrategy = GoogleDiscoverStrategy()


        fun setPackageToScan(packageToScan: String): Builder {
            this.packageToScan = packageToScan

            return getThis()
        }

        fun setClassloader(classLoader: ClassLoader): Builder {
            this.classLoader = classLoader

            return getThis()
        }

        fun setModuleConstructor(constructor: ModuleConstructor): Builder {
            this.constructor = constructor

            return getThis()
        }

        fun setDiscoverStrategy(discoverStrategy: DiscoverStrategy): Builder {
            this.discoverStrategy = discoverStrategy

            return getThis()
        }

        override fun getThis(): Builder = this

        override fun build(): DiscoveryModuleContainer {
            if (!::classLoader.isInitialized)
                classLoader = javaClass.classLoader

            return DiscoveryModuleContainer(
                configProvider,
                logger,
                onPreEnable,
                onEnable,
                onPostEnable,
                moduleConfigKey,
                moduleConfigHeader,
                resolveStrategy,
                classLoader,
                packageToScan,
                constructor,
                discoverStrategy
            )
        }
    }
}