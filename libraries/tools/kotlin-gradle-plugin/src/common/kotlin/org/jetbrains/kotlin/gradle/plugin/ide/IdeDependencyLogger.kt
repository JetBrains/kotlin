/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getOrNull

internal object IdeDependencyLogger : IdeDependencyEffect {

    private const val propertyKey = "IdeDependencyLogger.log"

    override fun invoke(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>) {
        val log = sourceSet.project.extraProperties.getOrNull(propertyKey)?.toString() ?: return
        val coordinates = IdeaKotlinSourceCoordinates(sourceSet)
        if (log.isEmpty() || Regex(log).matches(coordinates.toString())) {
            IdeMultiplatformImport.logger.quiet(
                "\n$coordinates resolved:\n" + dependencies.mapNotNull { it.coordinates }
                    .joinToString("\n") { it.toString().prependIndent("    ") } +
                        "\n"
            )
        }
    }
}
