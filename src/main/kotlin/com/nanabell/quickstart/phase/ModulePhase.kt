package com.nanabell.quickstart.phase

import com.nanabell.quickstart.loader.ModuleConstructor
import com.nanabell.quickstart.strategy.DiscoverStrategy

enum class ModulePhase {

    /**
     * Module has been found by a [DiscoverStrategy].
     */
    DISCOVERED,

    /**
     * Module has been constructed by a [ModuleConstructor].
     */
    CONSTRUCTED,

    /**
     * Module has been Enabled.
     */
    ENABLED,

    /**
     * Module has been Disabled.
     */
    DISABLED,

    /**
     * An error occurred during any phase and the module has been discarded
     */
    ERRORED

}
