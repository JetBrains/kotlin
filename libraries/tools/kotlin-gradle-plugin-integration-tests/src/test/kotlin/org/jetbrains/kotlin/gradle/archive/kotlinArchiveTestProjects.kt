/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.archive

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.addKgpToBuildScriptCompilationClasspath
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * All the source sets of the [kotlinArchiveProducer], both platform and shared ones.
 */
internal val kotlinArchiveSourceSets = listOf(
    "commonMain",
    "nativeMain",
    "appleMain",
    "webMain",
    "jvmMain",
    "jsMain",
    "wasmJsMain",
    "linuxX64Main",
    "iosArm64Main",
    "macosArm64Main",
)

/**
 * A producer with a target of every kind supported by the Kotlin Archive and with
 * a single declaration in every source set, so that the published sources can be checked.
 */
internal fun KGPBaseTest.kotlinArchiveProducer(gradleVersion: GradleVersion): TestProject = project("empty", gradleVersion) {
    addKgpToBuildScriptCompilationClasspath()
    settingsBuildScriptInjection {
        settings.rootProject.name = "producer"
    }
    kotlinArchiveSourceSets.forEach { sourceSetName ->
        addSourceFile(sourceSetName, "fun $sourceSetName() {}")
    }
    buildScriptInjection {
        project.applyMultiplatform {
            kotlinArchiveTargets()

            publishing {
                publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
            }
        }
    }
}

/**
 * The targets of the [kotlinArchiveProducer], which have to be repeated by its consumers.
 */
internal fun KotlinMultiplatformExtension.kotlinArchiveTargets() {
    jvm()
    js()
    wasmJs()
    linuxX64()
    iosArm64()
    macosArm64()
}

internal fun TestProject.addSourceFile(sourceSetName: String, content: String) {
    val sourceFile = kotlinSourcesDir(sourceSetName).resolve("$sourceSetName.kt")
    sourceFile.parent.createDirectories()
    sourceFile.writeText("$content\n")
}
