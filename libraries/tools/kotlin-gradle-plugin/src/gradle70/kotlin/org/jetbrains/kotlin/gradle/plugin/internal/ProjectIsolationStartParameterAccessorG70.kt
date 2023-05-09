/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.invocation.Gradle

internal class ProjectIsolationStartParameterAccessorG70() : ProjectIsolationStartParameterAccessor {
    override val isProjectIsolationEnabled: Boolean
        get() = false

    internal class Factory : ProjectIsolationStartParameterAccessor.Factory {
        override fun getInstance(gradle: Gradle): ProjectIsolationStartParameterAccessor {
            return ProjectIsolationStartParameterAccessorG70()
        }
    }
}
