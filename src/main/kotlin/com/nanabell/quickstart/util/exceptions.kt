package com.nanabell.quickstart.util

import com.nanabell.quickstart.Module
import com.nanabell.quickstart.config.ModuleConfig
import com.nanabell.quickstart.config.adapter.AbstractConfigAdapter
import kotlin.reflect.KClass

open class ModuleDiscoveryException : Exception {
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
}

open class ModuleException : ModuleDiscoveryException {
    constructor(clazz: KClass<out Module>, message: String) : super("$message, $clazz")
    constructor(message: String, throwable: Throwable): super(message, throwable)
    constructor(message: String): super(message)
}

// Discovery
class ModuleAlreadyRegistered(clazz: KClass<out Module>, id: String) : ModuleException("Unable to register $clazz, Module id '$id' is already registered!")

// Discovery -> Dependency
open class DependencyException(message: String) : ModuleException(message)
class DependencyNotFoundException(clazz: KClass<out Module>, dependency: String) : DependencyException("Dependency $dependency of Module $clazz does not exists!")
class CircularDependencyException(clazz: KClass<out Module>) : DependencyException("Class $clazz has caused a circular dependencies!")

// Loading
class IllegalModuleDisableException(key: String) : ModuleException("Attempted to disable required Module $key!")

// Loading -> Construction
class ModuleConstructionException(clazz: KClass<out Module>, throwable: Throwable): ModuleException("Failed to construct Module $clazz!", throwable)
class ModuleConfigConstructionException(clazz: KClass<out ModuleConfig>, throwable: Throwable): ModuleException("Failed to construct ModuleConfig $clazz!", throwable)
class NoModulesConstructedException : ModuleException("No Modules to load after Construction Phase")

// Loading -> Config Attach
class AdapterNotAttachedException(clazz: KClass<out AbstractConfigAdapter<*>>) : ModuleException("The Class $clazz has no AdaptableConfigProvider attached!")
class ModuleAlreadyAttachedException(moduleName: String) : ModuleException("The Module $moduleName is already attached!")
class ConfigMappingException(throwable: Throwable) : ModuleException("Module Mapping failed!", throwable)

// Loading -> Pre, Enable, Post
class MissingDependencyException(module: Module, message: String) : ModuleException(module::class, message)
class ModuleLoadingException(module: Module, message: String) : ModuleException(module::class, message)
class NoModulesReadyException : ModuleException("No Modules are Enabled After Enable Phase!")