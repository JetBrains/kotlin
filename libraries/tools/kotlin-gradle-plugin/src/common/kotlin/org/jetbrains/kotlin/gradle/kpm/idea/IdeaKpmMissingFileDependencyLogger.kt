/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.path
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

internal object IdeaKpmMissingFileDependencyLogger : IdeaKpmDependencyEffect {
    override fun invoke(fragment: GradleKpmFragment, dependencies: Set<IdeaKpmDependency>) {
        dependencies.filterIsInstance<IdeaKpmResolvedBinaryDependency>()
            .filter { !it.binaryFile.exists() }
            .ifNotEmpty { fragment.project.logger.warn(buildWarningMessage(fragment, this)) }
    }

    private fun buildWarningMessage(fragment: GradleKpmFragment, dependencies: List<IdeaKpmResolvedBinaryDependency>) = buildString {
        append("[WARNING] ${fragment.path}: Missing dependency files")
        dependencies.forEach { dependency ->
            appendLine(dependency.binaryFile)
        }
    }
}
