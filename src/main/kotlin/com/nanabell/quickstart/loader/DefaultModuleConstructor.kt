package com.nanabell.quickstart.loader

import com.nanabell.quickstart.Module
import com.nanabell.quickstart.util.ModuleConstructionException
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class DefaultModuleConstructor : ModuleConstructor {

    override fun createInstance(clazz: KClass<out Module>): Module {
        return try {
            clazz.createInstance()
        } catch (e: Throwable) {
            throw ModuleConstructionException(clazz, e)
        }
    }

}
