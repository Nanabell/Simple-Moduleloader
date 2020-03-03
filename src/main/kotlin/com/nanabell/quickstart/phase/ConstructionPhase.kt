package com.nanabell.quickstart.phase

import com.nanabell.quickstart.container.ModuleContainer

enum class ConstructionPhase {

    /**
     * A [ModuleContainer] has been constructed and is ready to start discovering modules.
     */
    INITIALIZED,

    /**
     * The [ModuleContainer] is scanning the specified package for all accessible classes.
     */
    DISCOVERING,

    /**
     *  The [ModuleContainer] has discovered all Modules, but nothing has been constructed or enabled at this point.
     */
    DISCOVERED,

    /**
     * The [ModuleContainer] is enabling the discovered modules.
     */
    ENABLING,

    /**
     * The [ModuleContainer] has enabled all modules.
     */
    ENABLED,

    /**
     * The [ModuleContainer] has encountered an unrecoverable error and has canceled loading the modules.
     * Any Enabled modules will be flagged as [ModulePhase.ERRORED]
     */
    ERRORED;

    fun next(): ConstructionPhase {
        val values = enumValues<ConstructionPhase>()
        return values[(ordinal + 1) % values.size]
    }
}
