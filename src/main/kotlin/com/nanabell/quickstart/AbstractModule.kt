package com.nanabell.quickstart

import com.nanabell.quickstart.config.ModuleConfig
import com.nanabell.quickstart.config.adapter.AbstractConfigAdapter
import com.nanabell.quickstart.config.adapter.ModuleConfigAdapter
import com.nanabell.quickstart.loader.ConfigConstructor

abstract class AbstractModule<C : ModuleConfig> : Module<C> {

    private val configAdapter: ModuleConfigAdapter<C> by lazy { ModuleConfigAdapter(this, getConfigConstructor()) }

    final override fun getConfig(): C {
        return getConfigAdapter().getConfig()
    }

    final override fun getConfigConstructor(): ConfigConstructor {
        return super.getConfigConstructor()
    }

    final override fun getConfigAdapter(): AbstractConfigAdapter<C> {
        return configAdapter
    }
}