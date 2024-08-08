/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project

internal class ProjectIsolationStartParameterAccessorG70() : ProjectIsolationStartParameterAccessor {
    override val isProjectIsolationEnabled: Boolean
        get() = false
    override val isProjectIsolationRequested: Boolean
        get() = isProjectIsolationEnabled

    internal class Factory : ProjectIsolationStartParameterAccessor.Factory {
        override fun getInstance(project: Project): ProjectIsolationStartParameterAccessor {
            return ProjectIsolationStartParameterAccessorG70()
        }
    }
}
