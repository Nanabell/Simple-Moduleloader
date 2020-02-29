package com.nanabell.quickstart.loader

import com.nanabell.quickstart.config.ModuleConfig
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class DefaultConfigConstructor : ConfigConstructor {

    override fun <C : ModuleConfig> createInstance(clazz: KClass<C>): C {
        return try {
            clazz.createInstance()
        } catch (e: Exception) {
            TODO("Add custom exception")
        }
    }

}
