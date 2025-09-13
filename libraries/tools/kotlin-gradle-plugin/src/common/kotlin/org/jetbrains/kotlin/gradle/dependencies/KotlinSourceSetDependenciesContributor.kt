/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencies

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal interface KotlinSourceSetDependenciesContributor<T: KotlinSourceSetDependencies> {
    suspend operator fun invoke(sourceSet: InternalKotlinSourceSet): List<T>?

    companion object {
        val extensionPoint = KotlinGradlePluginExtensionPoint<KotlinSourceSetDependenciesContributor<*>>()

        private val cache = mutableMapOf<InternalKotlinSourceSet, List<KotlinSourceSetDependencies>>()
        suspend fun getOrEvaluate(sourceSet: InternalKotlinSourceSet): List<KotlinSourceSetDependencies> =
            cache.getOrPut(sourceSet) {
                val res = mutableListOf<KotlinSourceSetDependencies>()
                extensionPoint[sourceSet.internal.project].forEach { contributor ->
                    val contribution = contributor(sourceSet) ?: emptyList()
                    res.addAll(contribution)
                }
                res
            }
    }
}

internal fun <T: KotlinSourceSetDependencies> KotlinSourceSetDependenciesContributor(
    code: suspend (InternalKotlinSourceSet) -> List<T>?
) = object : KotlinSourceSetDependenciesContributor<T> {
    override suspend fun invoke(sourceSet: InternalKotlinSourceSet): List<T>? = code(sourceSet)
}


internal suspend fun KotlinSourceSet.dependencies(): List<KotlinSourceSetDependencies> =
    KotlinSourceSetDependenciesContributor.getOrEvaluate(internal)

sealed interface KotlinSourceSetDependencies {
    val files: FileCollection
}