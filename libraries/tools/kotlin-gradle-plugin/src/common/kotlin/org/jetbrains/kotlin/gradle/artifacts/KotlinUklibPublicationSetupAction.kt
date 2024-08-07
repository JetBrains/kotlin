/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseCompilations
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.setAttribute
import java.io.File

internal val KotlinUklibPublicationSetupAction = KotlinProjectSetupAction {
    project.launch {
        val sourceSets = multiplatformExtension.awaitSourceSets()
        val metadataTarget = multiplatformExtension.awaitMetadataTarget()
        val targets = multiplatformExtension.awaitTargets()
        AfterFinaliseCompilations.await()

        val compilationToArtifact = mutableMapOf<KotlinCompilation<*>, Iterable<File>>()

        targets.forEach { target ->
            when (target) {
                is KotlinJsIrTarget -> {
                    error("...")
//                    val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
//                    compilationToArtifact[mainComp] = mainComp.out
                }
                is KotlinJvmTarget -> {
                    val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
                    compilationToArtifact[mainComp] = mainComp.output.classesDirs
                }
                is KotlinNativeTarget -> {
                    val mainComp = target.compilations.getByName(MAIN_COMPILATION_NAME)
                    compilationToArtifact[mainComp] = listOf(
                        // FIXME: Make this lazy
                        // FIXME: We have to unzip this
                        target.compilations.getByName(MAIN_COMPILATION_NAME).compileTaskProvider.flatMap {
                            it.outputFile
                        }.get()
                    )
                }
                is KotlinMetadataTarget -> {
                    target.compilations
                        // Probably this is not needed
                        .filterNot { it is KotlinCommonCompilation && !it.isKlibCompilation }
                        .forEach { compilation ->
                            // FIXME: Aren't test compilations going to be here?
                            compilationToArtifact[compilation] = compilation.output.classesDirs
                        }
                }
            }
        }

        val apiElements = configurations.getByName(metadataTarget.apiElementsConfigurationName)
        apiElements.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, usageByName(KotlinUsages.KOTLIN_UKLIB))
        apiElements.attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, categoryByName(Category.LIBRARY))
    }
}