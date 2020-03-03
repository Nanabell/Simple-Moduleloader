package com.nanabell.quickstart.strategy

import com.google.common.reflect.ClassPath
import kotlin.reflect.KClass

class GoogleDiscoverStrategy : DiscoverStrategy {

    @Suppress("UnstableApiUsage")
    override fun discover(rootPackage: String, classLoader: ClassLoader): Collection<KClass<*>> {
        val classInfos = ClassPath.from(classLoader).getTopLevelClassesRecursive(rootPackage)

        return classInfos.map { it.load().kotlin }
    }

}
