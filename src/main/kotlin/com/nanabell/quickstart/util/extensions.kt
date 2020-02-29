package com.nanabell.quickstart.util

import kotlin.reflect.KClass

val KClass<*>.isInterface get() = java.isInterface