package com.nanabell.quickstart.config.adapter

import com.nanabell.quickstart.config.AdaptableConfigProvider
import com.nanabell.quickstart.config.node.NodeProvider
import com.nanabell.quickstart.util.AdapterNotAttachedException
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode

abstract class AbstractConfigAdapter<C> : ConfigAdapter<C> {

    private var module: String? = null
    private var parent: AdaptableConfigProvider? = null
    private var nodeProvider: NodeProvider? = null
    private var header: String? = null

    override fun attachProvider(parent: AdaptableConfigProvider, module: String, nodeProvider: NodeProvider, header: String?) {
        this.module = module
        this.parent = parent
        this.nodeProvider = nodeProvider
        this.header = header
    }

    override fun detachProvider() {
        this.module = null
        this.parent = null
        this.nodeProvider = null
        this.header = null
    }

    fun isAttached(): Boolean = parent != null

    override fun generateDefaultConfig(): ConfigurationNode {
        return generateDefault(getNodeProvider().provideNode(getModule()))
    }

    override fun getConfig(): C? {
        return retrieveFromConfigurationNode(getNodeProvider().provideNode(getModule()))
    }

    override fun saveConfig(config: C) {
        val node = insertIntoConfigurationNode(getNodeProvider().emptyNode(), config)
        if (node is CommentedConfigurationNode && header != null) {
            node.setComment(getHeader())
        }

        getNodeProvider().consumeNode(node, getModule())
    }

    override fun refreshConfig() {
        saveConfig(getConfigOrDefault())
    }

    private fun getModule(): String = module ?: throw throw AdapterNotAttachedException(this::class)
    private fun getParent(): AdaptableConfigProvider = parent ?: throw throw AdapterNotAttachedException(this::class)
    private fun getNodeProvider(): NodeProvider = nodeProvider ?: throw throw AdapterNotAttachedException(this::class)
    private fun getHeader(): String = header ?: throw throw AdapterNotAttachedException(this::class)
}
