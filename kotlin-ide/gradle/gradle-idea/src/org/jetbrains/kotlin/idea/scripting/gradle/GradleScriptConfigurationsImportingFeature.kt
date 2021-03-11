/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.ProjectManager
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.configuration.ExperimentalFeature
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager

object GradleScriptConfigurationsImportingFeature : ExperimentalFeature() {
    override val title: String
        get() = KotlinIdeaGradleBundle.message("gradle.script.configurations.importing.feature")

    override fun shouldBeShown(): Boolean = true

    override var isEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                ProjectManager.getInstance().openProjects.forEach {
                    GradleBuildRootsManager.getInstance(it).enabled = field
                }
            }
        }
}