package com.nanabell.quickstart

import com.nanabell.quickstart.config.ModuleConfig
import com.nanabell.quickstart.config.adapter.AbstractConfigAdapter
import com.nanabell.quickstart.config.adapter.ConfigAdapter
import com.nanabell.quickstart.loader.ConfigConstructor
import com.nanabell.quickstart.loader.DefaultConfigConstructor

interface ConfigModule<C : ModuleConfig> : Module {

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

}