/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmAbstractFragmentMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmMetadataCompilationData
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import java.io.File

internal class KotlinCompileCommonConfig(
    private val compilationInfo: KotlinCompilationInfo,
) : AbstractKotlinCompileConfig<KotlinCompileCommon>(compilationInfo) {
    init {
        configureTask { task ->
            task.expectActualLinker.value(
                providers.provider {
                    (compilationInfo.origin as? KotlinCommonCompilation)?.isKlibCompilation == true ||
                            compilationInfo.origin is GradleKpmMetadataCompilationData<*>
                }
            ).disallowChanges()
            task.refinesMetadataPaths.from(getRefinesMetadataPaths(project)).disallowChanges()
        }
    }

    private fun getRefinesMetadataPaths(project: Project): Provider<Iterable<File>> {
        return project.provider {
            when (compilationInfo) {
                is KotlinCompilationInfo.TCS -> {
                    val defaultKotlinSourceSet: KotlinSourceSet = compilationInfo.compilation.defaultSourceSet
                    val metadataTarget = compilationInfo.compilation.target
                    defaultKotlinSourceSet.internal.dependsOnClosure
                        .mapNotNull { sourceSet -> metadataTarget.compilations.findByName(sourceSet.name)?.output?.classesDirs }
                        .flatten()
                }

                is KotlinCompilationInfo.KPM -> {
                    val compilationData = compilationInfo.compilationData as GradleKpmAbstractFragmentMetadataCompilationData<*>
                    val fragment = compilationData.fragment
                    project.files(
                        fragment.refinesClosure.minus(fragment).map {
                            val compilation = compilationData.metadataCompilationRegistry.getForFragmentOrNull(it)
                                ?: return@map project.files()
                            compilation.output.classesDirs
                        }
                    )
                }
            }
        }
    }
}
