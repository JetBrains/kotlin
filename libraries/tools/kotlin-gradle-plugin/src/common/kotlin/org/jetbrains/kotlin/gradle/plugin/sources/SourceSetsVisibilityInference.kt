/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.CompilationSourceSetUtil
import org.jetbrains.kotlin.gradle.plugin.mpp.associateWithClosure

fun getSourceSetsFromAssociatedCompilations(fromCompilation: KotlinCompilation<*>): Map<KotlinCompilation<*>, Set<KotlinSourceSet>> =
    fromCompilation.associateWithClosure.associate { it to it.allKotlinSourceSets }

fun getVisibleSourceSetsFromAssociateCompilations(
    project: Project,
    sourceSet: KotlinSourceSet
): List<KotlinSourceSet> = getVisibleSourceSetsFromAssociateCompilations(
    CompilationSourceSetUtil.compilationsBySourceSets(project).getValue(sourceSet)
)

internal fun getVisibleSourceSetsFromAssociateCompilations(
    participatesInCompilations: Set<KotlinCompilation<*>>
): List<KotlinSourceSet> {
    val visibleInCompilations = participatesInCompilations.map {
        val sourceSetsInAssociatedCompilations = getSourceSetsFromAssociatedCompilations(it)
        when (sourceSetsInAssociatedCompilations.size) {
            0 -> emptySet()
            1 -> sourceSetsInAssociatedCompilations.values.single()
            else -> mutableSetOf<KotlinSourceSet>().apply {
                for ((_, sourceSets) in sourceSetsInAssociatedCompilations) {
                    addAll(sourceSets)
                }
            }
        }
    }

    // Intersect the sets of source sets from the compilations:
    return when (visibleInCompilations.size) {
        0 -> emptySet()
        1 -> visibleInCompilations.single()
        else -> visibleInCompilations.first().toMutableSet().apply {
            visibleInCompilations.subList(1, visibleInCompilations.size).forEach { retainAll(it) }
        }
    }.toList()
}

class UnsatisfiedSourceSetVisibilityException(
    val sourceSet: KotlinSourceSet,
    val compilations: Set<KotlinCompilation<*>>,
    val visibleSourceSets: List<KotlinSourceSet>,
    val requiredButNotVisible: Set<KotlinSourceSet>
) : GradleException() {

    override val message: String?
        get() = buildString {
            fun singularOrPlural(collection: Collection<*>, singular: String, plural: String = singular + "s") =
                if (collection.size == 1) singular else plural

            fun compilationWithTarget(compilation: KotlinCompilation<*>) = "${compilation.name} (target ${compilation.target.name})"

            append(
                "The source set ${sourceSet.name} requires visibility of the " +
                        singularOrPlural(requiredButNotVisible, "source set", "source sets:") + " " +
                        "${requiredButNotVisible.joinToString { it.name }}. " +
                        "This requirement was not satisfied.\n\n"
            )

            append("${sourceSet.name} takes part in the ${singularOrPlural(compilations, "compilation")}:\n")

            fun appendCompilationRecursively(compilation: KotlinCompilation<*>, depth: Int) {
                val isAssociatedCompilation = depth > 0

                val sourceSetsInAssociatedCompilations =
                    getSourceSetsFromAssociatedCompilations(compilation)
                val allKotlinSourceSets = compilation.allKotlinSourceSets

                val indent = "  ".repeat(depth + 1)

                val prefix = if (isAssociatedCompilation)
                    "$indent- ${"indirectly ".takeIf { depth > 1 }.orEmpty()}associated with"
                else
                    "$indent-"

                append("$prefix ${compilationWithTarget(compilation)}")

                append(
                    if (isAssociatedCompilation)
                        ", which compiles " +
                                singularOrPlural(allKotlinSourceSets, "source set ", "source sets: ") +
                                allKotlinSourceSets.joinToString { it.name } +
                                "\n"
                    else "\n"
                )

                compilation.associateWith.forEach { appendCompilationRecursively(it, depth + 1) }

                if (!isAssociatedCompilation) {
                    val missingRequiredSourceSets = requiredButNotVisible.filter { missingSourceSet ->
                        sourceSetsInAssociatedCompilations.values.none { missingSourceSet in it }
                    }

                    if (missingRequiredSourceSets.isEmpty()) {
                        append("${indent}The compilation ${compilationWithTarget(compilation)} requires no changes.\n")
                    } else {
                        append(
                            "${indent}To ensure the required visibility, the compilation " + compilationWithTarget(compilation) +
                                    " must have a direct or indirect associate that compiles the source " +
                                    singularOrPlural(missingRequiredSourceSets, "set ", "sets: ") +
                                    missingRequiredSourceSets.joinToString { it.name } + "\n"
                        )
                    }
                }
            }

            compilations.forEach {
                appendCompilationRecursively(it, 0)
                append("\n")
            }
        }
}

fun checkSourceSetVisibilityRequirements(
    project: Project
) = checkSourceSetVisibilityRequirements(
    project.kotlinExtension.sourceSets,
    CompilationSourceSetUtil.compilationsBySourceSets(project)
)

internal fun checkSourceSetVisibilityRequirements(
    sourceSets: Iterable<KotlinSourceSet>,
    compilationsBySourceSet: Map<KotlinSourceSet, Set<KotlinCompilation<*>>>
) {
    sourceSets.forEach { sourceSet ->
        val requiredVisibility = sourceSet.requiresVisibilityOf
        val inferredVisibility =
            getVisibleSourceSetsFromAssociateCompilations(compilationsBySourceSet[sourceSet].orEmpty())

        val requiredButNotVisible = requiredVisibility - inferredVisibility - sourceSet.withDependsOnClosure

        if (requiredButNotVisible.isNotEmpty()) {
            val compilations = compilationsBySourceSet.getValue(sourceSet)

            throw UnsatisfiedSourceSetVisibilityException(
                sourceSet,
                compilations,
                inferredVisibility,
                requiredButNotVisible
            )
        }
    }
}
