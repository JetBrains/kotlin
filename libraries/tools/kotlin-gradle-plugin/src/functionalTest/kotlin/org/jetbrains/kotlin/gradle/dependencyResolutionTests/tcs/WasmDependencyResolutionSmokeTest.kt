/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import kotlin.test.Test

class WasmDependencyResolutionSmokeTest {

    @Test
    fun `test - project to project ide dependency resolution`() {
        val rootProject = buildProject()
        val consumer = buildProject(projectBuilder = { withName("consumer").withParent(rootProject) })
        val producer = buildProject(projectBuilder = { withName("producer").withParent(rootProject) })

        rootProject.subprojects { project ->
            project.enableDependencyVerification(false)
            project.applyMultiplatformPlugin()
            project.repositories.mavenLocal()
            project.repositories.mavenCentralCacheRedirector()
        }

        producer.multiplatformExtension.apply {
            jvm()
            js(IR) { browser() }
            wasmJs()
        }

        consumer.multiplatformExtension.apply {
            jvm()
            js(IR) { browser() }
            wasmJs()

            sourceSets.commonMain.dependencies {
                implementation(producer)
            }
        }

        rootProject.evaluate()
        producer.evaluate()
        consumer.evaluate()

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("commonMain").assertMatches(
            regularSourceDependency(":producer/commonMain"),
            binaryCoordinates(Regex(".*stdlib.*"))
        )

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("wasmJsMain").assertMatches(
            dependsOnDependency(":consumer/commonMain"),
            projectArtifactDependency(type = Regular, ":producer", FilePathRegex(".*/producer-wasm-js.klib")),
            binaryCoordinates(Regex(".*stdlib-wasm-js.*"))
        )

        consumer.kotlinIdeMultiplatformImport.resolveDependencies("wasmJsTest").assertMatches(
            friendSourceDependency(":consumer/commonMain"),
            friendSourceDependency(":consumer/wasmJsMain"),
            dependsOnDependency(":consumer/commonTest"),
            projectArtifactDependency(type = Regular, ":producer", FilePathRegex(".*/producer-wasm-js.klib")),
            binaryCoordinates(Regex(".*stdlib-wasm-js.*"))
        )
    }
}
