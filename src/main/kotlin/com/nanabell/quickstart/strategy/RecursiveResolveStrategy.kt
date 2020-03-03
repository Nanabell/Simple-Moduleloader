package com.nanabell.quickstart.strategy

import com.nanabell.quickstart.ModuleMeta
import com.nanabell.quickstart.util.CircularDependencyException
import com.nanabell.quickstart.util.DependencyNotFoundException
import java.util.*

class RecursiveResolveStrategy : ResolveStrategy {

    override fun resolveModuleDependencies(discovered: List<ModuleMeta>): LinkedList<ModuleMeta> {
        val result = LinkedList<ModuleMeta>()

        discovered.forEach { resolveDependencyStep(discovered, it, mutableSetOf(), result) }
        return result
    }

    /**
     * Recursive call to walk from the bottom of the Tree back up
     *
     * @param discovered Map of all discovered modules
     * @param meta Meta for the current module in question
     * @param visited Set containing all module ids we've already visited
     *
     * @throws DependencyNotFoundException If a Dependency of a Module could not be found in the [discovered] map
     * @throws CircularDependencyException If a dependency self references over other dependencies
     */
    @Throws(DependencyNotFoundException::class, CircularDependencyException::class)
    private fun resolveDependencyStep(
        discovered: List<ModuleMeta>,
        meta: ModuleMeta,
        visited: MutableSet<String>,
        result: LinkedList<ModuleMeta>
    ) {
        if (!visited.contains(meta.id)) {
            visited.add(meta.id)

            meta.dependencies.forEach { key ->
                val dependency = discovered.firstOrNull { it.id == key }
                    ?: throw DependencyNotFoundException(meta.moduleClass, key)

                resolveDependencyStep(discovered, dependency, visited, result)
            }

            meta.softDependencies.forEach { key ->
                val softDependency = discovered.firstOrNull { it.id == key }
                    ?: throw DependencyNotFoundException(meta.moduleClass, key)

                resolveDependencyStep(discovered, softDependency, visited, result)
            }

            result.add(meta)
        } else {
            if (!result.any { it.id == meta.id })
                throw CircularDependencyException(meta.moduleClass)
        }
    }
}
