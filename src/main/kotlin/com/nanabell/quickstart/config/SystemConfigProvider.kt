package com.nanabell.quickstart.config

import com.nanabell.quickstart.ModuleMeta
import com.nanabell.quickstart.config.adapter.AbstractConfigAdapter
import com.nanabell.quickstart.config.adapter.ConfigAdapter
import com.nanabell.quickstart.config.adapter.ModuleStatusConfigAdapter
import com.nanabell.quickstart.config.node.NodeProvider
import com.nanabell.quickstart.util.ModuleAlreadyAttachedException
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.ConfigurationOptions
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader

class SystemConfigProvider<N : ConfigurationNode, L : ConfigurationLoader<out N>>(
        loader: L,
        optionsTransformer: (ConfigurationOptions) -> ConfigurationOptions
) : AdaptableConfigProvider {

    private val nodeProvider: NodeProvider = NodeProvider(loader, optionsTransformer, loader.load(optionsTransformer.invoke(loader.defaultOptions)))

    private val moduleAdapters: MutableMap<String, ConfigAdapter<*>> = HashMap()
    private lateinit var moduleConfig: ModuleStatusConfigAdapter

    override fun attachModuleStatusConfig(modules: Map<String, ModuleMeta>, moduleName: String, header: String) {
        val moduleConfig = ModuleStatusConfigAdapter(modules.keys, modules.values.map { it.id to it.description.ifEmpty { null } }.toMap(), header)

        moduleConfig.attachProvider(this, moduleName.toLowerCase(), nodeProvider, header)
        this.moduleConfig = moduleConfig
    }

    override fun getModuleStatusConfig(): ModuleStatusConfigAdapter? {
        return this.moduleConfig
    }

    override fun attachConfigAdapter(moduleName: String, adapter: AbstractConfigAdapter<*>, header: String?) {
        if (moduleAdapters.containsKey(moduleName))
            throw ModuleAlreadyAttachedException(moduleName)

        adapter.attachProvider(this, moduleName.toLowerCase(), nodeProvider, header)
        moduleAdapters[moduleName.toLowerCase()] = adapter
    }

    override fun detachConfigAdapter(moduleName: String) {
        moduleAdapters.remove(moduleName.toLowerCase())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <C : ModuleConfig> getConfigAdapterForModule(module: String): AbstractConfigAdapter<C>? {
        val adapter = moduleAdapters[module.toLowerCase()] ?: return null

        return adapter as AbstractConfigAdapter<C>
    }

    override fun reload() {
        nodeProvider.reload()
    }

    override fun refresh() {
        reload().also { refreshConfiguration() }
    }

    override fun save(refresh: Boolean) {
        nodeProvider.save()

        if (refresh) refreshConfiguration()
    }

    private fun refreshConfiguration() {
        moduleAdapters.values.forEach { it.refreshConfig()}
    }

    override fun createDefaultConfigs() {
        val root: CommentedConfigurationNode = SimpleCommentedConfigurationNode.root()

        defaultModuleStatusConfig(root)
        moduleAdapters.forEach { (key, value) ->

            var current = value.generateDefaultConfig()
            if (current.parent != null)
                current = current.parent!!

            root.getNode(key.toLowerCase()).value = current
        }

        nodeProvider.getRootNode().mergeValuesFrom(root)
        save(false)
    }

    private fun defaultModuleStatusConfig(root: ConfigurationNode) {
        var current =  moduleConfig.generateDefaultConfig()
        if (current.parent != null)
            current = current.parent!!

        root.getNode(moduleConfig.getModuleKey()).value = current
    }

}
