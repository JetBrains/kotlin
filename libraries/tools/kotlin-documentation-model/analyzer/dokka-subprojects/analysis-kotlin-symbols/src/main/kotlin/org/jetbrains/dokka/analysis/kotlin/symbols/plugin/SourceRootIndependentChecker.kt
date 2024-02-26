/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.validity.PreGenerationChecker
import org.jetbrains.dokka.validity.PreGenerationCheckerOutput
import java.io.File

/**
 * It checks that different source sets should have disjoint source roots and samples.
 *
 *
 * K2's analysis API does not support having two different [org.jetbrains.kotlin.analysis.project.structure.KtSourceModule] with the same file system directory or intersecting files, it throws the error "Modules are inconsistent".
 *
 * @see org.jetbrains.kotlin.analysis.project.structure.KtModule.contentScope
 * @see org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider.getModule
 */
internal class SourceRootIndependentChecker(
    private val context: DokkaContext
) : PreGenerationChecker {
    override fun invoke(): PreGenerationCheckerOutput {
        val messages = mutableListOf<String>()
        val sourceSets = context.configuration.sourceSets

        for (i in sourceSets.indices) {
            for (j in i + 1 until sourceSets.size) {
                // check source roots
                val sourceRoot1 = sourceSets[i].sourceRoots.normalize()
                val sourceRoot2 = sourceSets[j].sourceRoots.normalize()
                val intersection = intersect(sourceRoot1, sourceRoot2)
                if (intersection.isNotEmpty()) {
                    messages += "Source sets '${sourceSets[i].displayName}' and '${sourceSets[j].displayName}' have the common source roots: ${intersection.joinToString()}. Every Kotlin source file should belong to only one source set (module)."
                }
            }
        }
        return PreGenerationCheckerOutput(messages.isEmpty(), messages)
    }


    private fun Set<File>.normalize() = mapTo(mutableSetOf()) { it.normalize() }
    private fun intersect(normalizedPaths: Set<File>, normalizedPaths2: Set<File>): Set<File> {
        val result = mutableSetOf<File>()
        for (p1 in normalizedPaths) {
            for (p2 in normalizedPaths2) {
                if (p1.startsWith(p2) || p2.startsWith(p1)) {
                    result.add(p1)
                    result.add(p2)
                }
            }
        }
        return result
    }

}