package com.nanabell.quickstart.config.adapter

import com.google.common.reflect.TypeToken
import com.nanabell.quickstart.AbstractModule
import com.nanabell.quickstart.config.ModuleConfig
import com.nanabell.quickstart.loader.ConfigConstructor
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

@Suppress("UnstableApiUsage", "UNCHECKED_CAST")
class ModuleConfigAdapter<C : ModuleConfig>(
        module: AbstractModule<C>,
        private val constructor: ConfigConstructor
) : AbstractConfigAdapter<C>() {

    private val default: C = createInstance(module::class)
    private val typeToken: TypeToken<C> = TypeToken.of(default::class.java as Class<C>)

    override fun getConfigOrDefault(): C {
        return try {
            getConfig() ?: default
        } catch (e: Exception) {
            e.printStackTrace() // TODO: Get the logger to here if this keeps happening often and use that instead
            default
        }
    }

    override fun generateDefault(node: ConfigurationNode): ConfigurationNode {
        return try {
            node.setValue(typeToken, default)
        } catch (e: ObjectMappingException) {
            TODO("Should this really just return default?. Imo if default fails we cant construct module as configs are not available")
        }
    }

    override fun retrieveFromConfigurationNode(node: ConfigurationNode): C? {
        return try {
            node.getValue(typeToken)
        } catch (e: ObjectMappingException) {
            TODO("Add Logging")
        }
    }

    override fun insertIntoConfigurationNode(node: ConfigurationNode, config: C): ConfigurationNode {
        return try {
            node.setValue(typeToken, config)
        } catch (e: ObjectMappingException) {
            TODO("Add Logging")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createInstance(clazz: KClass<out AbstractModule<C>>): C {
        if (!AbstractModule::class.isSuperclassOf(clazz))
            throw IllegalArgumentException("Class $clazz is not a Subclass of ${AbstractModule::class}!")

        val type = clazz.java.genericSuperclass
        if (type is ParameterizedType) {
            val target = type.actualTypeArguments.map { (it as Class<C>).kotlin }.firstOrNull()
                    ?: throw IllegalStateException("Class $clazz does not have any TypeArguments!")

            return constructor.createInstance(target)
        }

        throw IllegalStateException("Class $clazz is not a ParameterizedType!")
    }

}
