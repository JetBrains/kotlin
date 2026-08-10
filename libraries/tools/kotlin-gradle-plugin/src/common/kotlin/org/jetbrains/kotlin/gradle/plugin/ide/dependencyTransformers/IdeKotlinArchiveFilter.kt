/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyTransformers

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.UnsupportedKotlinArchiveUsage
import org.jetbrains.kotlin.gradle.plugin.diagnostics.isKarOrKarXZFile
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyTransformer

/**
 * Filters (and reports) .kar or .kar.xz files being resolved directly.
 * This can happen when the current tooling used does not support the .kar file publications.
 * .kar files are (likely) supported from 2.5 forward
 */
internal object IdeKotlinArchiveFilter : IdeDependencyTransformer {
    override fun transform(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>): Set<IdeaKotlinDependency> {
        return dependencies.filterTo(LinkedHashSet(dependencies.size)) filter@{ dependency ->
            if (dependency is IdeaKotlinResolvedBinaryDependency) {
                val karFiles = dependency.classpath.filter { file -> file.isKarOrKarXZFile() }
                if (karFiles.isNotEmpty()) {
                    sourceSet.project.reportDiagnosticOncePerBuild(
                        UnsupportedKotlinArchiveUsage(karFiles, dependency.coordinates?.displayString)
                    )

                    return@filter false
                }
            }

            true
        }
    }
}
