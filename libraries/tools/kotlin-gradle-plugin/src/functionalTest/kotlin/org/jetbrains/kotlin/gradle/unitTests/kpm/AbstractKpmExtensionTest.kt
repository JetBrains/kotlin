/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.kpm

import org.gradle.api.Project
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmProject
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.util.addBuildEventsListenerRegistryMock

abstract class AbstractKpmExtensionTest {
    protected val project: ProjectInternal = (ProjectBuilder.builder().build() as ProjectInternal).also { project ->
        project.gradle.startParameter.dependencyVerificationMode = DependencyVerificationMode.OFF
    }

    protected val kotlin: KotlinPm20ProjectExtension by lazy { project.applyKpmPlugin() }
}

fun Project.applyKpmPlugin(configure: KotlinPm20ProjectExtension.() -> Unit = {}): KotlinPm20ProjectExtension {
    addBuildEventsListenerRegistryMock(project)
    apply<KotlinPm20PluginWrapper>()
    return (extensions.getByName("kotlin") as KotlinPm20ProjectExtension).also(configure)
}

fun KotlinPm20ProjectExtension.buildIdeaKpmProjectModel(): IdeaKpmProject {
    return ideaKpmProjectModelBuilder.buildIdeaKpmProject()
}
