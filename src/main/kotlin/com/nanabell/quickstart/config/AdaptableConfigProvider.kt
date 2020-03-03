package com.nanabell.quickstart.config

import com.nanabell.quickstart.ModuleMeta
import com.nanabell.quickstart.config.adapter.AbstractConfigAdapter
import com.nanabell.quickstart.config.adapter.ModuleStatusConfigAdapter
import com.nanabell.quickstart.util.ModuleAlreadyAttachedException
import ninja.leaping.configurate.loader.ConfigurationLoader
import java.io.IOException

interface AdaptableConfigProvider {

    /**
     * Attach the ModuleConfigAdapter into the current [AdaptableConfigProvider]
     * Implementations may ignore this if they do not have a ModuleConfig
     */
    fun attachModuleStatusConfig(modules: List<ModuleMeta>, moduleName: String, header: String)

    /**
     * Retrieve the [ModuleStatusConfigAdapter] if the implementation supports this
     *
     * @return [ModuleStatusConfigAdapter] or null if not implemented
     */
    fun getModuleStatusConfig(): ModuleStatusConfigAdapter?

    /**
     * Attach a configAdapter to the current ConfigProvider
     *
     * @param moduleName Unique Id of the Module
     * @param adapter Adapter that will be attached to the provider
     * @param header Optional Header String for the Module Section in the Config file
     *
     * @throws ModuleAlreadyAttachedException If a Module with the same [moduleName] was already attached
     */
    @Throws(ModuleAlreadyAttachedException::class)
    fun attachConfigAdapter(moduleName: String, adapter: AbstractConfigAdapter<*>, header: String?)

    /**
     * Remove a configAdapter from the current AdaptableConfigProvider.
     * This method will do nothing if the requested module is not attached
     *
     * @param moduleName Unique Id of the Module
     */
    fun detachConfigAdapter(moduleName: String)

    /**
     * Retrieve the [AbstractConfigAdapter] for the requested Module id
     *
     * @param module Unique Id of the module
     * @return [AbstractConfigAdapter] for module or null if not found
     */
    fun <C : ModuleConfig> getConfigAdapterForModule(module: String): AbstractConfigAdapter<C>?

    /**
     * Reload the [AdaptableConfigProvider] but do not change any Module status.
     * Any Subsequent calls to Config retrieval will result in the new values but existing Config Objects will not be updated
     *
     * @throws [IOException] If the underlying [ConfigurationLoader] encounters an IOException
     */
    @Throws(IOException::class)
    fun reload()

    /**
     * Reload the [AdaptableConfigProvider] and Refresh all Modules forcing reloading the new Config Status
     *
     * @throws [IOException] If the underlying [ConfigurationLoader] encounters an IOException
     */
    @Throws(IOException::class)
    fun refresh()

    /**
     * Save the current in Memory Module configurations to disk
     * @param refresh reload add ConfigurationAdapters with the new vales?
     * @throws [IOException] If the underlying [ConfigurationLoader] encounters an IOException
     */
    @Throws(IOException::class)
    fun save(refresh: Boolean)

    /**
     * Create the default configurations for all attached adapters & merge them with the existing configuration.
     * Save the result of that to disk
     *
     * @throws [IOException] If the underlying [ConfigurationLoader] encounters an IOException
     */
    @Throws(IOException::class)
    fun createDefaultConfigs()

}
