/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask.Companion.taskNameForKotlinModule
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.parseJsonOrThrow
import org.junit.Assume.assumeFalse
import org.junit.AssumptionViolatedException
import org.junit.Test
import kotlin.test.assertEquals

class KotlinToolingMetadataMppIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    private val defaultKotlinToolingMetadataJsonPath get() =
        if (isKpmModelMappingEnabled) {
            "build/kotlinToolingMetadata/main/kotlin-tooling-metadata.json"
        } else {
            "build/kotlinToolingMetadata/kotlin-tooling-metadata.json"
        }

    private val buildKotlinToolingMetadataTaskName get() =
        if (isKpmModelMappingEnabled) {
            taskNameForKotlinModule(GradleKpmModule.MAIN_MODULE_NAME)
        } else {
            BuildKotlinToolingMetadataTask.defaultTaskName
        }

    @Test
    fun `new-mpp-published`() = with(transformProjectWithPluginsDsl("new-mpp-published")) {

        build("publish") {
            assertSuccessful()
            assertTasksExecuted(":$buildKotlinToolingMetadataTaskName")
            /* Check metadata file in build dir */
            assertFileExists(defaultKotlinToolingMetadataJsonPath)
            val metadataJson = projectDir.resolve(defaultKotlinToolingMetadataJsonPath).readText()
            val metadata = KotlinToolingMetadata.parseJsonOrThrow(metadataJson)
            assertEquals(
                listOfNotNull(
                    KotlinPlatformType.common.name.takeIf { !isKpmModelMappingEnabled },
                    KotlinPlatformType.jvm.name,
                    KotlinPlatformType.js.name,
                    KotlinPlatformType.native.name,
                    KotlinPlatformType.wasm.name,
                ).sorted(),
                metadata.projectTargets.map { it.platformType }.sorted()
            )

            /* Check metadata file in published repository */
            val publishedMetadataJson = projectDir.parentFile.resolve(
                "repo/com/example/bar/my-lib-bar/1.0/my-lib-bar-1.0-kotlin-tooling-metadata.json"
            ).readText()

            assertEquals(
                metadataJson, publishedMetadataJson,
                "Expected published kotlin-tooling-metadata.json to contain same content as in buildDir"
            )
        }

        /* Checking UP-TO-DATE behaviour */
        build("publish") {
            assertSuccessful()
            // Nothing changed. Should be up-to-date
            assertTasksUpToDate(":$buildKotlinToolingMetadataTaskName")
        }

        // Adding macos target
        val buildFile = projectDir.resolve("build.gradle.kts")
        buildFile.writeText(
            buildFile.readText().replace(
                "linuxX64()", "linuxX64()\nmacosX64()"
            )
        )

        build("publish") {
            assertSuccessful()
            assertTasksExecuted(":$buildKotlinToolingMetadataTaskName")
            val metadata = KotlinToolingMetadata.parseJsonOrThrow(projectDir.resolve(defaultKotlinToolingMetadataJsonPath).readText())
            assertEquals(
                listOf(KonanTarget.LINUX_X64.name, KonanTarget.MACOS_X64.name).sorted(),
                metadata.projectTargets.mapNotNull { it.extras.native?.konanTarget }.sorted()
            )
        }

    }

    @Test
    fun `new-mpp-published with KotlinToolingMetadataArtifact disabled`() = with(transformProjectWithPluginsDsl("new-mpp-published")) {
        projectDir.resolve("gradle.properties").appendText("\nkotlin.mpp.enableKotlinToolingMetadataArtifact=false")
        build("publish") {
            assertSuccessful()
            assertNoSuchFile(defaultKotlinToolingMetadataJsonPath)
            assertTasksNotRealized(
                ":$buildKotlinToolingMetadataTaskName"
            )
        }
    }

    @Test
    fun `kotlin-js-browser-project`() = with(transformProjectWithPluginsDsl("kotlin-js-browser-project")) {
        assumeFalse("KPM model mapping is not yet supported in single-platform projects", isKpmModelMappingEnabled)
        build(BuildKotlinToolingMetadataTask.defaultTaskName) {
            assertSuccessful()
            assertTasksExecuted(":app:$buildKotlinToolingMetadataTaskName")
            assertTasksExecuted(":base:$buildKotlinToolingMetadataTaskName")
            assertTasksExecuted(":lib:$buildKotlinToolingMetadataTaskName")
        }
    }

    @Test
    fun `kpm multiple modules`() {
        // TODO: Move it to Integration Tests Container for pure KPM projects
        if (isKpmModelMappingEnabled) throw AssumptionViolatedException("Pure KPM tests don't need KPM model mapping flag")

        with(transformProjectWithPluginsDsl("kpm-multi-module-published")) {
            val expectedMetadataByModule = mapOf<String, KotlinToolingMetadata.() -> Unit>(
                GradleKpmModule.MAIN_MODULE_NAME to {
                    val nativeTarget = projectTargets.single { it.platformType == KotlinPlatformType.native.name }
                    assertEquals(KonanTarget.LINUX_X64.name, nativeTarget.extras.native?.konanTarget)
                },
                "secondaryModule" to {
                    val nativeTarget = projectTargets.single { it.platformType == KotlinPlatformType.native.name }
                    assertEquals(KonanTarget.LINUX_ARM64.name, nativeTarget.extras.native?.konanTarget)
                },
            )

            // FIXME: Use `publish` task for Integration Tests.
            //  However Publishing of multiple modules fails currently and need proper design & implementation KT-49704
            build(BuildKotlinToolingMetadataTask.defaultTaskName) {
                assertSuccessful()
                expectedMetadataByModule.forEach { (moduleName, assertExpected) ->
                    assertTasksExecuted(":${taskNameForKotlinModule(moduleName)}")

                    val pathToMetadata = "build/kotlinToolingMetadata/$moduleName/kotlin-tooling-metadata.json"
                    assertFileExists(pathToMetadata)
                    val metadataJson = projectDir.resolve(pathToMetadata).readText()
                    val metadata = KotlinToolingMetadata.parseJsonOrThrow(metadataJson)

                    metadata.assertExpected()
                }
            }
        }
    }
}
