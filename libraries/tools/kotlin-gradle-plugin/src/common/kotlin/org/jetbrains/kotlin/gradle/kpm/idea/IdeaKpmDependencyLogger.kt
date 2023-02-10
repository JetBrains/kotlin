/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.idea.kpm.IdeaKpmDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.path

private const val IDEA_KOTLIN_LOG_DEPENDENCIES = "idea.kotlin.log.dependencies"

internal object IdeaKpmDependencyLogger : IdeaKpmDependencyEffect {
    override fun invoke(
        fragment: GradleKpmFragment, dependencies: Set<IdeaKpmDependency>
    ) {
        if (!fragment.project.hasProperty(IDEA_KOTLIN_LOG_DEPENDENCIES)) return
        val fragmentPathRegex = fragment.project.property(IDEA_KOTLIN_LOG_DEPENDENCIES).toString()
        if (!fragment.path.matches(Regex(fragmentPathRegex))) return

        val message = buildString {
            appendLine("Resolved dependencies for ${fragment.path}")
            dependencies.forEach { dependency -> appendLine("> $dependency") }
            appendLine()
        }

        fragment.project.logger.quiet(message)
    }
}
