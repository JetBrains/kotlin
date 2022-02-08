/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import java.util.WeakHashMap

internal abstract class SingleAction {
    private val performedActions = WeakHashMap<Project, MutableSet<String>>()

    protected abstract fun selectKey(project: Project): Project

    fun run(project: Project, actionId: String, action: () -> Unit) {
        val performedActions = performedActions.computeIfAbsent(selectKey(project)) { mutableSetOf() }
        if (performedActions.add(actionId)) {
            action()
        }
    }
}

internal object SingleActionPerBuild : SingleAction() {
    override fun selectKey(project: Project): Project = project.rootProject
}

internal object SingleActionPerProject : SingleAction() {
    override fun selectKey(project: Project) = project
}

internal object SingleWarningPerBuild {
    private const val ACTION_ID_SHOW_WARNING = "show-warning:"

    fun show(project: Project, warningText: String) = SingleActionPerBuild.run(project, ACTION_ID_SHOW_WARNING + warningText) {
        project.logger.warn(warningText)
    }

    fun deprecation(project: Project, context: String, target: String, replacement: String?) {
        val replacementMessage = replacement?.let { " Please, use '$replacement' instead." } ?: ""
        show(project, "Warning: $context '$target' is deprecated and will be removed in next major releases.$replacementMessage\n")
    }
}
