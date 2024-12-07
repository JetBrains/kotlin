/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.utils.projectStoredProperty

interface IdeCompilerArgumentsResolver {
    fun resolveCompilerArguments(any: Any): List<String>?

    companion object {
        internal val logger: Logger = Logging.getLogger(IdeMultiplatformImport::class.java)

        @JvmStatic
        fun instance(project: Project): IdeCompilerArgumentsResolver {
            return project.kotlinIdeCompilerArgumentsResolver
        }
    }
}

internal val Project.kotlinIdeCompilerArgumentsResolver: IdeCompilerArgumentsResolver by projectStoredProperty {
    IdeCompilerArgumentsResolverImpl()
}
