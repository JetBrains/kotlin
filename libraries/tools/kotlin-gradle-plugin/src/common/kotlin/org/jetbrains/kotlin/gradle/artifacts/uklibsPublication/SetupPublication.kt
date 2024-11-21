/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts.uklibsPublication

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.tasks.dependsOn

internal suspend fun Project.setupPublication() {
    val sourceSets = multiplatformExtension.awaitSourceSets()
    val metadataTarget = multiplatformExtension.awaitMetadataTarget()
    val targets = multiplatformExtension.awaitTargets()
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    val packUklib = tasks.register("packUklib", UklibArchiveTask::class.java)

    val kgpModel = uklibFromKGPModel(
        targets = targets.toList(),
        onPublishCompilation = {
            packUklib.dependsOn(it.compileTaskProvider)
        }
    )

    packUklib.configure {
        it.model.set(kgpModel)
    }
    artifacts.add(metadataTarget.uklibElementsConfigurationName, packUklib)

    if (kotlinPropertiesProvider.disablePlatformSpecificComponentsReferences) {
        // FIXME: 20.11.2024 - ???
        configurations.configureEach {
            if (!it.isCanBeConsumed) return@configureEach
            if (it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name != KotlinUsages.KOTLIN_UKLIB) {
                it.isCanBeConsumed = false
            }
        }
    }
}