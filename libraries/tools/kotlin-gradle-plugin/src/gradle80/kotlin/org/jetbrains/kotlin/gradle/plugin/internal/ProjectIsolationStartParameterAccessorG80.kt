/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.internal.StartParameterInternal
import org.gradle.api.invocation.Gradle

internal class ProjectIsolationStartParameterAccessorG80(
    private val gradle: Gradle
) : ProjectIsolationStartParameterAccessor {
    override val isProjectIsolationEnabled: Boolean by lazy {
        (gradle.startParameter as StartParameterInternal).isolatedProjects.get()
    }
    override val isProjectIsolationRequested: Boolean
        get() = isProjectIsolationEnabled

    internal class Factory : ProjectIsolationStartParameterAccessor.Factory {
        override fun getInstance(project: Project): ProjectIsolationStartParameterAccessor {
            return ProjectIsolationStartParameterAccessorG80(project.gradle)
        }
    }
}
