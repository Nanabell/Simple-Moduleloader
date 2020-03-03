package com.nanabell.quickstart

import com.nanabell.quickstart.phase.LoadingStatus
import com.nanabell.quickstart.phase.ModulePhase
import kotlin.reflect.KClass

data class ModuleMeta (
    val moduleClass: KClass<out Module>,
    val id: String,
    val name: String,
    val description: String,
    var status: LoadingStatus,
    val required: Boolean,
    val softDependencies: List<String>,
    val dependencies: List<String>,
    val parents: MutableSet<String> = mutableSetOf()
) {

    var phase: ModulePhase = ModulePhase.DISCOVERED


    constructor(moduleClass: KClass<out Module>, annotation: RegisterModule) : this(
        moduleClass,
        annotation.id,
        annotation.name,
        annotation.description,
        LoadingStatus.ENABLED,
        annotation.required,
        annotation.softDependencies.toList(),
        annotation.dependencies.toList()
    )
}