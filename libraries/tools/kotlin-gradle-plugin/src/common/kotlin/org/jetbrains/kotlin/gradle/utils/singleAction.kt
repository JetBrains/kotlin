/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import java.util.*

/**
 * Base implementation of an actions executor that executes them once.
 */
internal abstract class SingleAction {
    private val performedActions = WeakHashMap<Project, MutableSet<String>>()

    /**
     * Calculates a part of a key that is used to determine whether an action from [run] was already executed
     */
    protected abstract fun selectKey(project: Project): Project

    /**
     * Runs an [action] once per key value which is being calculated as a combination of a [selectKey] value and an [actionId]
     *
     * Warning: if KGP is loaded multiple times by different classloaders, actions with the same [actionId] may be executed more than once
     */
    fun run(project: Project, actionId: String, action: () -> Unit) {
        val performedActions = performedActions.computeIfAbsent(selectKey(project)) { mutableSetOf() }
        if (performedActions.add(actionId)) {
            action()
        }
    }
}

/**
 * Object that allows to run actions once per build
 *
 * Warning: if KGP is loaded multiple times by different classloaders, actions with the same id may be executed more than once
 */
internal object SingleActionPerBuild : SingleAction() {
    override fun selectKey(project: Project): Project = project.rootProject
}

/**
 * Object that allows to run actions once per project
 *
 * Warning: if KGP is loaded multiple times by different classloaders, actions with the same id may be executed more than once
 */
internal object SingleActionPerProject : SingleAction() {
    override fun selectKey(project: Project) = project
}
