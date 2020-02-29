package com.nanabell.quickstart

annotation class RegisterModule(

    val id: String,

    val name: String,

    val description: String = "",

    val softDependencies: Array<String> = [],

    val dependencies: Array<String> = [],

    val required: Boolean = false

)
