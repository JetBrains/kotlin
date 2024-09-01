/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties

/**
 * Base implementation of an actions executor that executes them once.
 */
internal abstract class SingleAction {

    protected abstract val propertyKey: String
    protected abstract fun extraProperties(project: Project): ExtraPropertiesExtension

    /**
     * Runs an [action] once per [actionId] key value.
     *
     * Check the concrete implementation description for how often this action is expected to run.
     */
    fun run(
        project: Project,
        actionId: String,
        action: () -> Unit
    ) {
        val extraProperties = extraProperties(project)
        val performedActions = extraProperties.getOrPut(propertyKey) {
            PerformedActions()
        }
        if (performedActions.actionsIds.add(actionId)) {
            action()
            extraProperties.set(propertyKey, performedActions)
        }
    }

    class PerformedActions(
        val actionsIds: MutableSet<String> = mutableSetOf()
    )
}

/**
 * Object that allows to run actions once per build
 *
 * Warning:
  * - if the build has an included build, the action can be executed for each included build
 */
internal object SingleActionPerBuild : SingleAction() {
    override val propertyKey = SingleActionPerBuild::class.java.name

    override fun extraProperties(project: Project): ExtraPropertiesExtension = project.rootProject.extraProperties
}

/**
 * Object that allows to run actions once per project.
 */
internal object SingleActionPerProject : SingleAction() {
    override val propertyKey: String = SingleActionPerProject::class.java.name

    override fun extraProperties(project: Project): ExtraPropertiesExtension = project.extraProperties
}
