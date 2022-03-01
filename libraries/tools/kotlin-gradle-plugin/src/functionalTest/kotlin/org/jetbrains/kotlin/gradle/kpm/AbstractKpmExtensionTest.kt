/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.addBuildEventsListenerRegistryMock
import kotlin.test.BeforeTest

abstract class AbstractKpmExtensionTest {
    protected val project: ProjectInternal = ProjectBuilder.builder().build() as ProjectInternal
    protected lateinit var kotlin: KpmExtension
        private set

    @BeforeTest
    open fun setup() {
        kotlin = project.applyKpmPlugin()
    }

}

fun Project.applyKpmPlugin(): KpmExtension {
    addBuildEventsListenerRegistryMock(project)
    plugins.apply("org.jetbrains.kotlin.multiplatform.pm20")
    return extensions.getByName("kotlin") as KpmExtension
}

