/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("ClassName")

package org.jetbrains.kotlin.idea.debugger.test.preference

import java.lang.reflect.ParameterizedType
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaType

class DebuggerPreferenceKey<T : Any>(val name: String, val type: Class<*>, val defaultValue: T)

private inline fun <reified T : Any> debuggerPreferenceKey(defaultValue: T): ReadOnlyProperty<Any, DebuggerPreferenceKey<T>> {
    val clazz = T::class.java

    return object : ReadOnlyProperty<Any, DebuggerPreferenceKey<T>> {
        override fun getValue(thisRef: Any, property: KProperty<*>) = DebuggerPreferenceKey(property.name, clazz, defaultValue)
    }
}

internal object DebuggerPreferenceKeys {
    val SKIP_SYNTHETIC_METHODS by debuggerPreferenceKey(true)
    val SKIP_CONSTRUCTORS: DebuggerPreferenceKey<Boolean> by debuggerPreferenceKey(false)
    val SKIP_CLASSLOADERS by debuggerPreferenceKey(true)
    val TRACING_FILTERS_ENABLED by debuggerPreferenceKey(true)
    val SKIP_GETTERS by debuggerPreferenceKey(false)

    val DISABLE_KOTLIN_INTERNAL_CLASSES by debuggerPreferenceKey(false)
    val RENDER_DELEGATED_PROPERTIES by debuggerPreferenceKey(false)
    val IS_FILTER_FOR_STDLIB_ALREADY_ADDED by debuggerPreferenceKey(false)

    val FORCE_RANKING by debuggerPreferenceKey(false)

    val PRINT_FRAME by debuggerPreferenceKey(false)
    val SHOW_KOTLIN_VARIABLES by debuggerPreferenceKey(false)
    val DESCRIPTOR_VIEW_OPTIONS by debuggerPreferenceKey("FULL")

    val ATTACH_LIBRARY by debuggerPreferenceKey(emptyList<String>())

    val SKIP by debuggerPreferenceKey(emptyList<String>())
    val WATCH_FIELD_ACCESS by debuggerPreferenceKey(true)
    val WATCH_FIELD_MODIFICATION by debuggerPreferenceKey(true)
    val WATCH_FIELD_INITIALISATION by debuggerPreferenceKey(false)

    val JVM_TARGET by debuggerPreferenceKey("1.8")

    val values: List<DebuggerPreferenceKey<*>> by lazy {
        DebuggerPreferenceKeys::class.declaredMemberProperties
            .filter { (it.returnType.javaType as? ParameterizedType)?.rawType == DebuggerPreferenceKey::class.java }
            .map { it.get(DebuggerPreferenceKeys) as DebuggerPreferenceKey<*> }
    }
}