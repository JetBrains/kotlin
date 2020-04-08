/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.preference

import com.intellij.openapi.project.Project

internal abstract class SettingsMutator<T : Any>(val key: DebuggerPreferenceKey<T>) {
    abstract fun setValue(value: T, project: Project): T

    open fun revertValue(value: T, project: Project) {
        setValue(value, project)
    }
}

internal fun <T : Any> SettingsMutator<T>.setValue(preferences: DebuggerPreferences): OldValueStorage<T> {
    val project = preferences.project
    return OldValueStorage(this, project, setValue(preferences[key], project))
}

internal class OldValueStorage<T : Any>(
    private val mutator: SettingsMutator<T>,
    private val project: Project,
    private val oldValue: T
) {
    fun revertValue() = mutator.revertValue(oldValue, project)
}

internal class OldValuesStorage(private val oldValues: List<OldValueStorage<*>>) {
    fun revertValues() = oldValues.forEach { it.revertValue() }
}

internal fun List<SettingsMutator<*>>.mutate(preferences: DebuggerPreferences): OldValuesStorage {
    return OldValuesStorage(map { it.setValue(preferences) })
}