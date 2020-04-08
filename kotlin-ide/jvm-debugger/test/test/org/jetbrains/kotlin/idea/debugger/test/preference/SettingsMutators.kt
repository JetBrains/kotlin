/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.preference

import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JvmCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.debugger.DebuggerUtils
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings
import org.jetbrains.kotlin.idea.debugger.ToggleKotlinVariablesState
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys.DISABLE_KOTLIN_INTERNAL_CLASSES
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys.IS_FILTER_FOR_STDLIB_ALREADY_ADDED
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys.RENDER_DELEGATED_PROPERTIES
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys.SKIP_CLASSLOADERS
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys.SKIP_CONSTRUCTORS
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys.SKIP_GETTERS
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys.SKIP_SYNTHETIC_METHODS
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys.TRACING_FILTERS_ENABLED
import kotlin.reflect.KMutableProperty1

internal val SettingsMutators: List<SettingsMutator<*>> = listOf(
    DebuggerSettingsMutator(SKIP_SYNTHETIC_METHODS, DebuggerSettings::SKIP_SYNTHETIC_METHODS),
    DebuggerSettingsMutator(SKIP_CONSTRUCTORS, DebuggerSettings::SKIP_CONSTRUCTORS),
    DebuggerSettingsMutator(SKIP_CLASSLOADERS, DebuggerSettings::SKIP_CLASSLOADERS),
    DebuggerSettingsMutator(TRACING_FILTERS_ENABLED, DebuggerSettings::TRACING_FILTERS_ENABLED),
    DebuggerSettingsMutator(SKIP_GETTERS, DebuggerSettings::SKIP_GETTERS),
    KotlinSettingsMutator(DISABLE_KOTLIN_INTERNAL_CLASSES, KotlinDebuggerSettings::DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES),
    KotlinSettingsMutator(RENDER_DELEGATED_PROPERTIES, KotlinDebuggerSettings::DEBUG_RENDER_DELEGATED_PROPERTIES),
    KotlinSettingsMutator(IS_FILTER_FOR_STDLIB_ALREADY_ADDED, KotlinDebuggerSettings::DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED),
    KotlinVariablesModeSettingsMutator,
    JvmTargetSettingsMutator,
    ForceRankingSettingsMutator
)

private class DebuggerSettingsMutator<T : Any>(
    key: DebuggerPreferenceKey<T>,
    private val prop: KMutableProperty1<DebuggerSettings, T>
) : SettingsMutator<T>(key) {
    override fun setValue(value: T, project: Project): T {
        val debuggerSettings = DebuggerSettings.getInstance()
        val oldValue = prop.get(debuggerSettings)
        prop.set(debuggerSettings, value)
        return oldValue
    }
}

private class KotlinSettingsMutator<T : Any>(
    key: DebuggerPreferenceKey<T>,
    private val prop: KMutableProperty1<KotlinDebuggerSettings, T>
) : SettingsMutator<T>(key) {
    override fun setValue(value: T, project: Project): T {
        val debuggerSettings = KotlinDebuggerSettings.getInstance()
        val oldValue = prop.get(debuggerSettings)
        prop.set(debuggerSettings, value)
        return oldValue
    }
}

private object KotlinVariablesModeSettingsMutator : SettingsMutator<Boolean>(DebuggerPreferenceKeys.SHOW_KOTLIN_VARIABLES) {
    override fun setValue(value: Boolean, project: Project): Boolean {
        val service = ToggleKotlinVariablesState.getService()
        val oldValue = service.kotlinVariableView
        service.kotlinVariableView = value
        return oldValue
    }
}

private object JvmTargetSettingsMutator : SettingsMutator<String>(DebuggerPreferenceKeys.JVM_TARGET) {
    override fun setValue(value: String, project: Project): String {
        var oldValue: String? = null
        Kotlin2JvmCompilerArgumentsHolder.getInstance(project).update {
            oldValue = jvmTarget
            jvmTarget = value.takeIf { it.isNotEmpty() }
        }
        return oldValue ?: ""
    }
}

private object ForceRankingSettingsMutator : SettingsMutator<Boolean>(DebuggerPreferenceKeys.FORCE_RANKING) {
    override fun setValue(value: Boolean, project: Project): Boolean {
        val oldValue = DebuggerUtils.forceRanking
        DebuggerUtils.forceRanking = value
        return oldValue
    }
}