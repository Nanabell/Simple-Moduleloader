package com.nanabell.quickstart.config.adapter

import com.google.common.reflect.TypeToken
import com.nanabell.quickstart.LoadingStatus
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException

@Suppress("UnstableApiUsage")
class ModuleStatusConfigAdapter(
    private val defaults: Set<String>,
    private val descriptions: Map<String, String>,
    private val header: String
) : AbstractConfigAdapter<HashMap<String, LoadingStatus>>() {

    private val defaultMap = defaults.map { it to LoadingStatus.ENABLED }.toMap() as HashMap<String, LoadingStatus>
    private val typeToken: TypeToken<HashMap<String, LoadingStatus>> = object : TypeToken<HashMap<String, LoadingStatus>>() {}

    override fun getConfigOrDefault(): HashMap<String, LoadingStatus> {
        return getConfig() ?: defaultMap
    }

    override fun generateDefault(node: ConfigurationNode): ConfigurationNode {
        defaults.forEach {
            node.getNode(it).value = LoadingStatus.ENABLED

            val comment = descriptions[it]
            if (node is CommentedConfigurationNode && comment != null) {
                node.getNode(it).setComment(comment)
            }
        }

        if (node is CommentedConfigurationNode) {
            node.setComment(header)
        }

        return node
    }

    override fun retrieveFromConfigurationNode(node: ConfigurationNode): HashMap<String, LoadingStatus> {
        return try {
            node.getValue(typeToken, defaultMap)
        } catch (e: ObjectMappingException) {
            TODO("Add Logging")
        }
    }

    override fun insertIntoConfigurationNode(node: ConfigurationNode,config: HashMap<String, LoadingStatus>): ConfigurationNode {
        return try {
            node.setValue(typeToken, config)
        } catch (e: ObjectMappingException) {
            TODO("Add Logging")
        }
    }

    fun getModuleKey(): String {
        return getModule()
    }
}
