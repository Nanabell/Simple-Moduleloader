package com.nanabell.quickstart.config.node

import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.ConfigurationOptions
import ninja.leaping.configurate.loader.ConfigurationLoader
import java.io.IOException

class NodeProvider(
        private val loader: ConfigurationLoader<*>,
        private val optionsTransformer: (ConfigurationOptions) -> ConfigurationOptions,
        private var rootNode: ConfigurationNode
) {

    fun getRootNode(): ConfigurationNode {
        return rootNode
    }

    @Throws(IOException::class)
    fun reload() {
        rootNode = loader.load(optionsTransformer.invoke(loader.defaultOptions))
    }

    @Throws(IOException::class)
    fun save() {
        loader.save(rootNode)
    }

    fun provideNode(moduleName: String): ConfigurationNode {
        return emptyNode().setValue(rootNode.getNode(moduleName.toLowerCase()))
    }

    fun consumeNode(node: ConfigurationNode, moduleName: String) {
        node.getNode(moduleName.toLowerCase()).value = node
    }

    fun emptyNode(): ConfigurationNode {
        return loader.createEmptyNode(optionsTransformer.invoke(loader.defaultOptions))
    }
}