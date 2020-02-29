package com.nanabell.quickstart.config.adapter

import com.nanabell.quickstart.config.AdaptableConfigProvider
import com.nanabell.quickstart.config.node.NodeProvider
import ninja.leaping.configurate.ConfigurationNode

/**
 * An Adapter between the config file [C] and the underlying [ConfigurationNode]s
 *
 * Responsible for reading and writing to the [ConfigurationNode] and
 * (De)-Serializing Configuration Objects [C]
 */
interface ConfigAdapter<C> {

    fun attachProvider(parent: AdaptableConfigProvider, module: String, nodeProvider: NodeProvider, header: String?)

    fun detachProvider()

    /**
     * Generate a [ConfigurationNode] populated with the layout of a default configuration
     */
    fun generateDefaultConfig(): ConfigurationNode

    /**
     * Return the config [C] or null if not found
     */
    fun getConfig(): C?

    /**
     * Return the config [C] or the default config
     */
    fun getConfigOrDefault(): C

    /**
     * Save the config [config]
     */
    fun saveConfig(config: C)

    /**
     * Refresh the in memory loaded config with the one on the filesystem
     */
    fun refreshConfig()

    /**
     * Generate the default Configuration Node
     */
    fun generateDefault(node: ConfigurationNode): ConfigurationNode

    /**
     * Retrieve the Config [C] from the [ConfigurationNode]
     */
    fun retrieveFromConfigurationNode(node: ConfigurationNode): C?

    /**
     * Insert the Config [C] into the [ConfigurationNode]
     */
    fun insertIntoConfigurationNode(node: ConfigurationNode, config: C): ConfigurationNode
}