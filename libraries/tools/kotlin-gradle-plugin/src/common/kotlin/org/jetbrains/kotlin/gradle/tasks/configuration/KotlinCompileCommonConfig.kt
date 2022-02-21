/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.AbstractKotlinFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.refinesClosure
import org.jetbrains.kotlin.gradle.plugin.sources.dependsOnClosure
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import java.io.File

internal class KotlinCompileCommonConfig(
    private val compilation: KotlinCompilationData<*>,
) : AbstractKotlinCompileConfig<KotlinCompileCommon>(compilation) {
    init {
        configureTask { task ->
            task.expectActualLinker.value(
                providers.provider {
                    (compilation as? KotlinCommonCompilation)?.isKlibCompilation == true || compilation is KotlinMetadataCompilationData
                }
            ).disallowChanges()
            task.refinesMetadataPaths.from(getRefinesMetadataPaths(project)).disallowChanges()
        }
    }

    private fun getRefinesMetadataPaths(project: Project): Provider<Iterable<File>> {
        return project.provider {
            when (compilation) {
                is KotlinCompilation<*> -> {
                    val defaultKotlinSourceSet: KotlinSourceSet = compilation.defaultSourceSet
                    val metadataTarget = compilation.owner as KotlinTarget
                    defaultKotlinSourceSet.dependsOnClosure
                        .mapNotNull { sourceSet -> metadataTarget.compilations.findByName(sourceSet.name)?.output?.classesDirs }
                        .flatten()
                }
                is AbstractKotlinFragmentMetadataCompilationData -> {
                    val fragment = compilation.fragment
                    project.files(
                        fragment.refinesClosure.minus(fragment).map {
                            val compilation = compilation.metadataCompilationRegistry.getForFragmentOrNull(it)
                                ?: return@map project.files()
                            compilation.output.classesDirs
                        }
                    )
                }
                else -> error("unexpected compilation type")
            }
        }
    }
}