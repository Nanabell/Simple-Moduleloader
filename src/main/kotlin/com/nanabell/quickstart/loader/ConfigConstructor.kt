package com.nanabell.quickstart.loader

import com.nanabell.quickstart.config.ModuleConfig
import com.nanabell.quickstart.util.ModuleConstructionException
import kotlin.reflect.KClass

interface ConfigConstructor {

    /**
     * Create a new Instance of the provided Module Config class
     *
     * @param clazz Class of the module to be constructed
     * @return The constructed [ModuleConfig]
     * @throws ModuleConstructionException If the construction has failed
     */
    @Throws(ModuleConstructionException::class)
    fun <C : ModuleConfig> createInstance(clazz: KClass<C>): C

}