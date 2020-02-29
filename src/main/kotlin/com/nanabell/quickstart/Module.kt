package com.nanabell.quickstart

import com.nanabell.quickstart.config.ModuleConfig
import com.nanabell.quickstart.config.adapter.AbstractConfigAdapter
import com.nanabell.quickstart.config.adapter.ConfigAdapter
import com.nanabell.quickstart.loader.ConfigConstructor
import com.nanabell.quickstart.loader.DefaultConfigConstructor
import com.nanabell.quickstart.util.MissingDependencyException
import com.nanabell.quickstart.util.ModuleLoadingException

interface Module<C : ModuleConfig> {

    /**
     * Returns the Config from the underlying [ConfigAdapter].
     * If no config exits this will return null
     *
     * @return Config or null if no config exists
     */
    fun getConfig(): C?

    /**
     * Returns the Config from the underlying [ConfigAdapter].
     * If no config exits this will return the default config
     *
     * @return Config or the default config
     */
    fun getConfigOrDefault(): C

    /**
     * Performs external checks to ensure the module has everything it requires loaded.
     *
     * @throws MissingDependencyException if the module cannot reach the external dependencies
     */
    @Throws(MissingDependencyException::class)
    fun checkExternalDependencies() {
    }

    /**
     * Called before the enable phase.
     * At this stage no configs have been loaded yet.
     *
     * Use this phase to construct any immutable Singletons required by the module
     * @throws ModuleLoadingException if anything went wrong while preparing
     */
    @Throws(ModuleLoadingException::class)
    fun preEnable()

    /**
     * Main enable phase for the module.
     * Config files for the module have been loaded and are available for consumption.
     *
     * Modules should use this phase to construct any resources necessary for operation
     * @throws ModuleLoadingException if anything went wrong while loading main resources
     */
    @Throws(ModuleLoadingException::class)
    fun onEnable()

    /**
     * Ran after all modules have finished enabling.
     *
     * Use this phase to tear down any resource that were necessary for module construction
     * @throws ModuleLoadingException if anything went wrong while deconstructing temporary resources
     */
    @Throws(ModuleLoadingException::class)
    fun postEnable()

    /**
     * Indicates module is no longer required.
     * Tear down any Singleton resources.
     *
     * Modules are not expected to serve any requests after they have been disabled.
     * **Note** errors thrown in [onDisable] are silently discarded!
     */
    fun onDisable() {
    }

    /**
     * Constructor responsible for creating default Config.
     * By default this is a [DefaultConfigConstructor] that will attempt to create Configs with a no args constructor
     *
     * @return ConfigConstructor implementation
     */
    fun getConfigConstructor(): ConfigConstructor {
        return DefaultConfigConstructor()
    }

    /**
     * Adapter Bridge between the Module & the Config layer.
     * Necessary for loading & saving configs at startup.
     *
     * Implementations can use the [AbstractModule] to have this handled automatically
     *
     * @return AbstractConfigAdapter<ModuleConfig>
     */
    fun getConfigAdapter(): AbstractConfigAdapter<C>
}
