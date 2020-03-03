package com.nanabell.quickstart.config.adapter

import com.google.common.reflect.TypeToken
import com.nanabell.quickstart.ConfigurableModule
import com.nanabell.quickstart.config.ModuleConfig
import com.nanabell.quickstart.loader.ConfigConstructor
import com.nanabell.quickstart.util.ConfigMappingException
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

@Suppress("UnstableApiUsage", "UNCHECKED_CAST")
class ModuleConfigAdapter<C : ModuleConfig>(
    module: ConfigurableModule<C>,
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
            throw ConfigMappingException(e)
        }
    }

    override fun retrieveFromConfigurationNode(node: ConfigurationNode): C? {
        return try {
            node.getValue(typeToken)
        } catch (e: ObjectMappingException) {
            throw ConfigMappingException(e)
        }
    }

    override fun insertIntoConfigurationNode(node: ConfigurationNode, config: C): ConfigurationNode {
        return try {
            node.setValue(typeToken, config)
        } catch (e: ObjectMappingException) {
            throw ConfigMappingException(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createInstance(clazz: KClass<out ConfigurableModule<C>>): C {
        if (!ConfigurableModule::class.isSuperclassOf(clazz))
            throw IllegalArgumentException("Class $clazz is not a Subclass of ${ConfigurableModule::class}!")

        val type = clazz.java.genericSuperclass
        if (type is ParameterizedType) {
            val target = type.actualTypeArguments.map { (it as Class<C>).kotlin }.firstOrNull()
                    ?: throw IllegalStateException("Class $clazz does not have any TypeArguments!")

            return constructor.createInstance(target)
        }

        throw IllegalStateException("Class $clazz is not a ParameterizedType!")
    }

}
