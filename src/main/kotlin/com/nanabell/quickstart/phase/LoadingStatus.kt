package com.nanabell.quickstart.phase

enum class LoadingStatus {

    /**
     * Default state, indicated that a module is enabled and should be loaded.
     */
    ENABLED,

    /**
     * Disables the module completely from being constructed, any depended modules will also not be constructed
     */
    DISABLED,

    /**
     * Indicates that a module should always be loaded regardless of disable requests
     */
    FORCE_LOAD

}
