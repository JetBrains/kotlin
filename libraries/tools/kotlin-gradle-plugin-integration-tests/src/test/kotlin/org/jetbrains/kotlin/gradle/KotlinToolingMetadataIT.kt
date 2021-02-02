/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.parseJsonOrThrow
import org.junit.Test
import kotlin.test.assertEquals

class KotlinToolingMetadataIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired = GradleVersionRequired.FOR_MPP_SUPPORT

    private val defaultKotlinToolingMetadataJsonPath = "build/kotlinToolingMetadata/kotlin-tooling-metadata.json"

    @Test
    fun `new-mpp-published`() = with(transformProjectWithPluginsDsl("new-mpp-published")) {
        build("publish") {
            assertSuccessful()
            assertTasksExecuted(
                ":${BuildKotlinToolingMetadataTask.defaultTaskName}"
            )
            /* Check metadata file in build dir */
            assertFileExists(defaultKotlinToolingMetadataJsonPath)
            val metadataJson = projectDir.resolve(defaultKotlinToolingMetadataJsonPath).readText()
            val metadata = KotlinToolingMetadata.parseJsonOrThrow(metadataJson)
            assertEquals(
                listOf(
                    KotlinPlatformType.common.name, KotlinPlatformType.jvm.name,
                    KotlinPlatformType.js.name, KotlinPlatformType.native.name
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
            assertTasksUpToDate(":${BuildKotlinToolingMetadataTask.defaultTaskName}")
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
            assertTasksExecuted(":${BuildKotlinToolingMetadataTask.defaultTaskName}")
            val metadata = KotlinToolingMetadata.parseJsonOrThrow(projectDir.resolve(defaultKotlinToolingMetadataJsonPath).readText())
            assertEquals(
                listOf(KonanTarget.LINUX_X64.name, KonanTarget.MACOS_X64.name).sorted(),
                metadata.projectTargets.mapNotNull { it.extras["konanTarget"] }.sorted()
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
                ":${BuildKotlinToolingMetadataTask.defaultTaskName}"
            )
        }
    }

    @Test
    fun `kotlin-js-browser-project`() = with(transformProjectWithPluginsDsl("kotlin-js-browser-project")) {
        build(BuildKotlinToolingMetadataTask.defaultTaskName) {
            assertSuccessful()
            assertTasksExecuted(":app:${BuildKotlinToolingMetadataTask.defaultTaskName}")
            assertTasksExecuted(":base:${BuildKotlinToolingMetadataTask.defaultTaskName}")
            assertTasksExecuted(":lib:${BuildKotlinToolingMetadataTask.defaultTaskName}")
        }
    }
}
