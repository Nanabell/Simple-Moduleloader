package com.nanabell.quickstart.strategy

import com.nanabell.quickstart.ModuleMeta
import com.nanabell.quickstart.util.DependencyException
import java.util.*

interface ResolveStrategy {

    @Throws(DependencyException::class)
    fun resolveModuleDependencies(discovered: List<ModuleMeta>): LinkedList<ModuleMeta>

}
