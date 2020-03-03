package com.nanabell.quickstart.strategy

import kotlin.reflect.KClass

interface DiscoverStrategy {

    /**
     * Discover accessible classes using the specified [ClassLoader]
     * in the [rootPackage] or any sub package
     *
     * @param rootPackage Top level package name to traverse from
     * @param classLoader [ClassLoader] to use while traversing
     *
     * @return Set of accessible classes withing the package bounds
     */
    fun discover(rootPackage: String, classLoader: ClassLoader): Collection<KClass<*>>

}