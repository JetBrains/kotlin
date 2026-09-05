/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation

import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.validity.PreGenerationChecker
import org.jetbrains.dokka.validity.PreGenerationCheckerOutput

internal class SourceSetIdUniquenessChecker(
    private val context: DokkaContext
) : PreGenerationChecker {
    override fun invoke(): PreGenerationCheckerOutput {
        val sourceSets = context.configuration.sourceSets
        val messages = mutableListOf<String>()
        for (i in sourceSets.indices) {
            for (j in i + 1 until sourceSets.size) {
                val id1 = sourceSets[i].sourceSetID
                val id2 = sourceSets[j].sourceSetID
                if (id1 == id2) {
                    messages += "Source sets '${sourceSets[i].displayName}' and '${sourceSets[j].displayName}' have the same `sourceSetID=${id1}`. Every source set should have unique sourceSetID."
                }
            }
        }
        return PreGenerationCheckerOutput(messages.isEmpty(), messages)
    }
}
