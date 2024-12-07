/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool

internal val LanguageSettingsSetupAction = KotlinProjectSetupAction {
    // common source sets use the compiler options from the metadata compilation:
    val metadataCompilation = multiplatformExtension.metadata().compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    val primaryCompilationsBySourceSet by lazy { // don't evaluate eagerly: Android targets are not created at this point
        val allCompilationsForSourceSets = project.multiplatformExtension.sourceSets.associateWith { sourceSet ->
            sourceSet.internal.compilations.filter { compilation -> compilation.target.platformType != KotlinPlatformType.common }
        }

        allCompilationsForSourceSets.mapValues { (_, compilations) -> // choose one primary compilation
            when (compilations.size) {
                0 -> metadataCompilation
                1 -> compilations.single()
                else -> {
                    val sourceSetTargets = compilations.map { it.target }.distinct()
                    when (sourceSetTargets.size) {
                        1 -> sourceSetTargets.single().compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                            ?: // use any of the compilations for now, looks OK for Android TODO maybe reconsider
                            compilations.first()

                        else -> metadataCompilation
                    }
                }
            }
        }
    }

    project.kotlinExtension.sourceSets.all { sourceSet ->
        (sourceSet.languageSettings as? DefaultLanguageSettingsBuilder)?.run {
            compilerPluginOptionsTask = lazy {
                val associatedCompilation = primaryCompilationsBySourceSet[sourceSet] ?: metadataCompilation
                project.tasks.getByName(associatedCompilation.compileKotlinTaskName) as AbstractKotlinCompileTool<*>
            }
        }
    }
}