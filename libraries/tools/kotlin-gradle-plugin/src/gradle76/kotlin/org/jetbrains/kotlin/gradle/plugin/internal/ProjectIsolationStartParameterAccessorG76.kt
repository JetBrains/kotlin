/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.internal.StartParameterInternal
import org.gradle.api.invocation.Gradle

internal class ProjectIsolationStartParameterAccessorG76(
    private val gradle: Gradle
) : ProjectIsolationStartParameterAccessor {
    override val isProjectIsolationEnabled: Boolean by lazy {
        (gradle.startParameter as StartParameterInternal).isolatedProjects.get()
    }

    internal class Factory : ProjectIsolationStartParameterAccessor.Factory {
        override fun getInstance(gradle: Gradle): ProjectIsolationStartParameterAccessor {
            return ProjectIsolationStartParameterAccessorG76(gradle)
        }
    }
}
