/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.Project
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.addBuildEventsListenerRegistryMock
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinProjectModel
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension

abstract class AbstractKpmExtensionTest {
    protected val project: ProjectInternal = (ProjectBuilder.builder().build() as ProjectInternal).also { project ->
        project.gradle.startParameter.dependencyVerificationMode = DependencyVerificationMode.OFF
    }

    protected val kotlin: KotlinPm20ProjectExtension by lazy { project.applyKpmPlugin() }
}

fun Project.applyKpmPlugin(configure: KotlinPm20ProjectExtension.() -> Unit = {}): KotlinPm20ProjectExtension {
    addBuildEventsListenerRegistryMock(project)
    plugins.apply("org.jetbrains.kotlin.multiplatform.pm20")
    return (extensions.getByName("kotlin") as KotlinPm20ProjectExtension).also(configure)
}

fun KotlinPm20ProjectExtension.buildIdeaKotlinProjectModel(): IdeaKotlinProjectModel {
    return ideaKotlinProjectModelBuilder.buildIdeaKotlinProjectModel()
}
