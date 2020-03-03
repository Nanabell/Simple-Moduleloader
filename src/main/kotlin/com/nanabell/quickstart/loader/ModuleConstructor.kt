package com.nanabell.quickstart.loader

import com.nanabell.quickstart.Module
import com.nanabell.quickstart.util.ModuleConstructionException
import kotlin.reflect.KClass

interface ModuleConstructor {

    /**
     * Create a new Instance of the provided module class
     *
     * @param clazz Class of the module to be constructed
     * @return The constructed [Module]
     * @throws ModuleConstructionException If the construction has failed
     */
    @Throws(ModuleConstructionException::class)
    fun createInstance(clazz: KClass<out Module>): Module

}