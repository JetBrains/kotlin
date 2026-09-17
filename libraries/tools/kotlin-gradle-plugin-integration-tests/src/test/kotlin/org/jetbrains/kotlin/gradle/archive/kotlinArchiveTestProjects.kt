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

internal val kotlinArchiveAppleSourceSets = listOf(
    "appleMain", "iosArm64Main", "macosArm64Main"
)
internal val kotlinArchiveAllSourceSets = listOf(
    "commonMain", "nativeMain", "appleMain", "webMain", "jvmMain",
    "jsMain", "wasmJsMain", "linuxMain", "linuxX64Main", "linuxArm64Main",
    "iosArm64Main", "macosArm64Main",
)


internal fun KGPBaseTest.kotlinArchiveProducer(
    gradleVersion: GradleVersion,
    withAppleTargets: Boolean = true,
): TestProject {

    return project("empty", gradleVersion) {
        addKgpToBuildScriptCompilationClasspath()
        settingsBuildScriptInjection {
            settings.rootProject.name = "producer"
        }
        kotlinArchiveAllSourceSets.filter { withAppleTargets || it !in kotlinArchiveAppleSourceSets }.forEach { sourceSetName ->
            addSourceFile(sourceSetName, "fun $sourceSetName() {}")
        }
        buildScriptInjection {
            project.applyMultiplatform {
                kotlinArchiveTargets(withAppleTargets)

                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        }
    }
}

internal fun KotlinMultiplatformExtension.kotlinArchiveTargets(withAppleTargets: Boolean = true) {
    jvm()
    js()
    wasmJs()
    linuxX64()
    linuxArm64()
    if (withAppleTargets) {
        iosArm64()
        macosArm64()
    }
}

internal fun TestProject.addSourceFile(sourceSetName: String, content: String, fileName: String = "$sourceSetName.kt") {
    val sourceFile = kotlinSourcesDir(sourceSetName).resolve(fileName)
    sourceFile.parent.createDirectories()
    sourceFile.writeText("$content\n")
}
