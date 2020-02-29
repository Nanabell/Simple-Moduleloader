# Simple-Moduleloader
[![Download](https://api.bintray.com/packages/nanabell/Sponge-Minecraft/simple-moduleloader/images/download.svg?version=v0.5.0) ](https://bintray.com/nanabell/Sponge-Minecraft/simple-moduleloader/v0.5.0/link)

Simple Moduleloader is a Module Loading System very heavily inspired by [QuickStartModuleLoader](https://github.com/NucleusPowered/QuickStartModuleLoader)  
but with the focus on simplicity in the Implementation but with few compromises.  

I've tried to make getting started with this System very easy, 
but it should also be very extensible for those that need the customization

## Before you begin!
Please note that I (like most of my projects) have written this library in [Kotlin](https://kotlinlang.org/).  
I am not providing the kotlin runtime within this library. you will have to provide this yourself.

Include the kotlin-stdlib &  kotlin-reflect into your plugin (example using gradle [shadow](https://github.com/johnrengelman/shadow) plugin)  
groovy `.gradle`
```groovy
dependencies {
    shadow group: 'org.jetbrains.kotlin', name: 'kotlin-stdlib', version: '1.3.61'
    shadow group: 'org.jetbrains.kotlin', name: 'kotlin-reflect', version: '1.3.61'
}
```


Kotlin `.kts`
```kotlin
dependencies {
    shadow("org.jetbrains.kotlin:stdlib-jdk8:1.3.61")
    shadow("org.jetbrains.kotlin:kotlin-reflect:1.3.61")
}
```

While i can imagine that using Kotlin is quite unpopular simply due to the additional size it brings with it.I don't have any plans to port this to pure java.  
If you want to use a pure java version which is very small,
use the original [module-loader](https://github.com/NucleusPowered/QuickStartModuleLoader) by the Nucleus team.

## Getting Started

To get started with Modules you need some module classes. A Module requires a few things:
- Module Config Class that implements `com.nanabell.quickstart.config.ModuleConfig` 
- Implement `com.nanabell.quickstart.Module<ModuleConfig>`
- Have `@com.nanabell.quickstart.RegisterModule` Annotation

Currently, every Module must have a Config Class which will be (De)-Serialized. with the `configurate` library.

Instead of implementing the `Module` Interface directly you can use the `AbstractModule<ModuleConfig>` abstract class.  
This class handles bridging the Module Class with the ConfigAdapter.

#### Let's start by creating the ModuleConfig Class:
```kotlin
import com.nanabell.quickstart.config.ModuleConfig
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
data class DemoConfig(

    @Setting("my-value", comment = "This is a Demo Config entry")
    val mySerializedProperty: String = "Default Value"

) : ModuleConfig
```
As you can see apart from the `ModuleConfig` annotation this is a default Configurate POJO Class

#### Next up is the actual Module Class.
This Class  is the main entry point for the Module.
```kotlin
import com.nanabell.quickstart.AbstractModule

@RegisterModule(id = "unique-id", name = "Display Name", description = "Free Description", softDependencies = ["soft-dep1", "soft-dep2"], dependencies = ["requried-dep"], required = false /*if true cant be disabled*/)
class DemoModule : AbstractModule<DemoConfig>() {

    override fun preEnable() {
        TODO("Not yet implemented")
    }

    override fun onEnable() {
        TODO("Not yet implemented")
    }

    override fun postEnable() {
        TODO("Not yet implemented")
    }

}
```
Here we use the `AbstractModule` so we don't have to implement methods like `getConfig()` or `getConfigAdapter()`

Every Module has to implement the 3 main Methods and has 2 optional ones.
- getConfigConstructor() - `Optional` Allows to override the default no-arg ConfigConstructor used to create a instance of the ModuleConfig.
- preEnable() - Called just after the Config files have been attached to the module. Use this phase to Construct any Essential components, like Services etc.
- checkExternalDependencies() - `Optional`. Use this to connect to any external resources. Throwing a `MissingDependencyException` will indicate that the Module cannot load due to missing external dependencies.
- onEnable() - The Main Phase for every Module. Construct any runtime components here.
- postEnable() - Cleanup phase, use this to remove any resources only needed to construct the module.

That's everything for the Module.

### The actual Module Loader
Just defining Modules won't do much as we still need a Instance of the Actual ModuleLoader.  
While this could be done at any time i suggest to use the `GamePreInitializationEvent` & `GameInitializationEvent` respectively.

#### GamePreInitializationEvent: Creating Container and discovering Modules

Create a new Event Listener for the `GamePreInitializationEvent` to construct the ModuleContainer
```kotlin
@Listener
fun preInit(event: GamePreInitializationEvent) {
    try {
        val moduleConfig = HoconConfigurationLoader.builder()
                .setPath(configDir.resolve("my-config-filename.conf"))
                .build()

        moduleContainer = DiscoveryModuleContainer.builder()
                .setConfigurationLoader(moduleConfig)
                .setModuleConfigKey("-modules")
                .setPackageToScan(javaClass.`package`.name + ".module")
                .setLogger(logger)
                .build(true)
    } catch (e: ModuleDiscoveryException) {
        logger.error("Plugin PreInitialization failed!", e)
    }
}
```
Here I use the `HoconConfigurationLoader` with the default options and use the file `my-config-filename.conf`  
- setConfigurationLoader() - `Required` Sets the ConfigurationLoader used to load & save the Configs.
- setModuleConfigKey() - `Required` Name of the Node where the `ENABLE/DISABLE` configs will be
- setPackageToScan() - `Required` Name of the root package to scan from. In my case this is the Package where the Plugin is located + `.module` e.g = `com.myplugin.module.**`
- setLogger() - `Optional` define a customer logger else it will create one with `LoggerFactory.getLogger(this::class.java)`
- build() - Actually Build the ModuleContainer, optionally pass true to instantly start discovering Modules else call `moduleContainer.startDiscover()`  

There are more options, but those are for more specific requirement i won't go into here.  
**Note:** The ModuleContainer will error if it can't find any Modules in the Discovering Phase and will be unusable after that

#### GameInitializationEvent: Loading the Modules
Last step to success is actually loading the modules that have been discovered.  

```kotlin
@Listener
fun onInit(event: GameInitializationEvent) {
    try {
        moduleContainer.loadModules(true)
    } catch (e: ModuleException) {
        logger.error("Plugin Initialization failed!", e)
    }
}
```
This will actually load the Modules and construct them.
The Boolean argument indicates if the ModuleLoader should cancel as soon as one Module throws an Error.  
If set to false it will continue with the rest of the modules which do **not** depend on the crashed Module.  
**Note:** The ModuleContainer will error if all Modules are disabled for one or the other reason and there are actually no Modules left to construct.

#### Loading Order
If a Module errors or is marked Disabled it will stop loading:
- Construct Modules
- Load ModulesStatusConfigs
- PreEnable phase
- checkExternalDependencies
- OnEnable phase
- PostEnable phase
- CreateDefaultConfigs

# Credits
This whole Project has been very heavily based upon [QuickStartModuleLoader](https://github.com/NucleusPowered/QuickStartModuleLoader) by the [NucleusPowered](https://github.com/NucleusPowered) team
