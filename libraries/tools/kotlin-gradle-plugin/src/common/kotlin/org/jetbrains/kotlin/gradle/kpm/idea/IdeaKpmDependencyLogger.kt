/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.path

internal object IdeaKpmDependencyLogger : IdeaKpmDependencyEffect {
    override fun invoke(
        fragment: GradleKpmFragment, dependencies: Set<IdeaKpmDependency>
    ) {
        val fragmentPathRegex = fragment.project.properties["idea.kotlin.log.dependencies"]?.toString() ?: return
        if (!fragment.path.matches(Regex(fragmentPathRegex))) return

        val message = buildString {
            appendLine("Resolved dependencies for ${fragment.path}")
            dependencies.forEach { dependency -> appendLine("> $dependency") }
            appendLine()
        }

        fragment.project.logger.quiet(message)
    }
}
