/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.archive

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.testing.compilationConfiguration
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.resolveSelectedVariantNames
import org.jetbrains.kotlin.gradle.testing.runtimeConfiguration
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.MavenComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.generateMockRepository
import org.jetbrains.kotlin.gradle.unitTests.uklibs.kmpJsApiVariantAttributes
import org.jetbrains.kotlin.gradle.unitTests.uklibs.kmpJsRuntimeVariantAttributes
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableDefaultJsDomApiDependency
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinArchiveLegacyPlatformCapabilityTest {

    @field:TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun `legacy platform component is replaced by the kotlin archive component`() {
        val repository = generateMockRepository(
            temporaryDirectory,
            listOf(
                legacyPlatformComponent("1.0"),
                kotlinArchiveComponent("2.0"),
                intermediateComponent("intermediateLibrary", dependency("library-js", "1.0")),
            )
        )

        val consumer = jsConsumer(
            repository,
            dependencyCoordinates = listOf("$GROUP:intermediateLibrary:1.0", "$GROUP:library:2.0")
        )

        assertEquals(
            mapOf(
                "$GROUP:intermediateLibrary:1.0" to "jsApiElements-published",
                "$GROUP:library:2.0" to "jsApiElements-published",
            ).prettyPrinted,
            consumer.jsCompileResolution().prettyPrinted,
        )
        assertEquals(
            mapOf(
                "$GROUP:intermediateLibrary:1.0" to "jsRuntimeElements-published",
                "$GROUP:library:2.0" to "jsRuntimeElements-published",
            ).prettyPrinted,
            consumer.jsRuntimeResolution().prettyPrinted,
        )
    }

    @Test
    fun `newer legacy platform component wins over the kotlin archive component`() {
        val repository = generateMockRepository(
            temporaryDirectory,
            listOf(
                legacyPlatformComponent("3.0"),
                kotlinArchiveComponent("2.0"),
                intermediateComponent("intermediateLibrary", dependency("library-js", "3.0")),
            )
        )

        val consumer = jsConsumer(
            repository,
            dependencyCoordinates = listOf("$GROUP:intermediateLibrary:1.0", "$GROUP:library:2.0")
        )

        assertEquals(
            mapOf(
                "$GROUP:intermediateLibrary:1.0" to "jsApiElements-published",
                "$GROUP:library-js:3.0" to "jsApiElements-published",
            ).prettyPrinted,
            consumer.jsCompileResolution().prettyPrinted,
        )
        assertEquals(
            mapOf(
                "$GROUP:intermediateLibrary:1.0" to "jsRuntimeElements-published",
                "$GROUP:library-js:3.0" to "jsRuntimeElements-published",
            ).prettyPrinted,
            consumer.jsRuntimeResolution().prettyPrinted,
        )
    }

    @Test
    fun `newest kotlin archive component wins over several legacy and several kotlin archive versions`() {
        val repository = generateMockRepository(
            temporaryDirectory,
            listOf(
                legacyPlatformComponent("1.0"),
                legacyPlatformComponent("1.1"),
                kotlinArchiveComponent("2.0"),
                kotlinArchiveComponent("2.1"),
                intermediateComponent("intermediateLegacyOld", dependency("library-js", "1.0")),
                intermediateComponent("intermediateLegacyNew", dependency("library-js", "1.1")),
                intermediateComponent("intermediateArchiveOld", dependency("library", "2.0")),
                intermediateComponent("intermediateArchiveNew", dependency("library", "2.1")),
            )
        )

        val consumer = jsConsumer(
            repository,
            dependencyCoordinates = listOf(
                "$GROUP:intermediateLegacyOld:1.0",
                "$GROUP:intermediateLegacyNew:1.0",
                "$GROUP:intermediateArchiveOld:1.0",
                "$GROUP:intermediateArchiveNew:1.0",
            )
        )

        assertEquals(
            mapOf(
                "$GROUP:intermediateLegacyOld:1.0" to "jsApiElements-published",
                "$GROUP:intermediateLegacyNew:1.0" to "jsApiElements-published",
                "$GROUP:intermediateArchiveOld:1.0" to "jsApiElements-published",
                "$GROUP:intermediateArchiveNew:1.0" to "jsApiElements-published",
                "$GROUP:library:2.1" to "jsApiElements-published",
            ).prettyPrinted,
            consumer.jsCompileResolution().prettyPrinted,
        )
        assertEquals(
            mapOf(
                "$GROUP:intermediateLegacyOld:1.0" to "jsRuntimeElements-published",
                "$GROUP:intermediateLegacyNew:1.0" to "jsRuntimeElements-published",
                "$GROUP:intermediateArchiveOld:1.0" to "jsRuntimeElements-published",
                "$GROUP:intermediateArchiveNew:1.0" to "jsRuntimeElements-published",
                "$GROUP:library:2.1" to "jsRuntimeElements-published",
            ).prettyPrinted,
            consumer.jsRuntimeResolution().prettyPrinted,
        )
    }

    private fun jsConsumer(
        repository: File,
        dependencyCoordinates: List<String>,
    ): Project = buildProjectWithMPP(
        preApplyCode = {
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)
        },
    ) {
        repositories.maven { it.setUrl(repository) }
        kotlin {
            js()
            sourceSets.commonMain.dependencies {
                dependencyCoordinates.forEach { implementation(it) }
            }
        }
    }.evaluate()

    private fun Project.jsCompileResolution() =
        multiplatformExtension.js().compilationConfiguration().resolveSelectedVariantNames()

    private fun Project.jsRuntimeResolution() =
        multiplatformExtension.js().runtimeConfiguration().resolveSelectedVariantNames()

    private fun legacyPlatformComponent(version: String) = jsGradleComponent(
        module = "library-js",
        version = version,
        apiAttributes = kmpJsApiVariantAttributes,
        runtimeAttributes = kmpJsRuntimeVariantAttributes,
    )

    private fun kotlinArchiveComponent(version: String) = jsGradleComponent(
        module = "library",
        version = version,
        apiAttributes = karJsApiVariantAttributes,
        runtimeAttributes = karJsRuntimeVariantAttributes,
        capabilities = listOf(
            GradleMetadataComponent.Capability(GROUP, "library-js", version),
            GradleMetadataComponent.Capability(GROUP, "library", version),
        ),
    )

    private fun intermediateComponent(
        module: String,
        dependency: GradleMetadataComponent.Dependency,
    ) = jsGradleComponent(
        module = module,
        version = "1.0",
        apiAttributes = kmpJsApiVariantAttributes,
        runtimeAttributes = kmpJsRuntimeVariantAttributes,
        dependencies = listOf(dependency),
    )

    private fun jsGradleComponent(
        module: String,
        version: String,
        apiAttributes: Map<String, String>,
        runtimeAttributes: Map<String, String>,
        dependencies: List<GradleMetadataComponent.Dependency> = emptyList(),
        capabilities: List<GradleMetadataComponent.Capability>? = null,
    ) = GradleComponent(
        GradleMetadataComponent(
            component = GradleMetadataComponent.Component(GROUP, module, version),
            variants = listOf(
                GradleMetadataComponent.Variant(
                    name = "jsApiElements-published",
                    attributes = apiAttributes,
                    dependencies = dependencies,
                    capabilities = capabilities,
                ),
                GradleMetadataComponent.Variant(
                    name = "jsRuntimeElements-published",
                    attributes = runtimeAttributes,
                    dependencies = dependencies,
                    capabilities = capabilities,
                ),
            ),
        ),
        MavenComponent(
            groupId = GROUP,
            artifactId = module,
            version = version,
            packaging = null,
            dependencies = emptyList(),
            gradleMetadataMarker = true,
        ),
    )

    private fun dependency(module: String, version: String) = GradleMetadataComponent.Dependency(
        group = GROUP,
        module = module,
        version = GradleMetadataComponent.Version(version),
    )

    companion object {
        private const val GROUP = "kotlinArchiveTest"

        private val karJsApiVariantAttributes =
            kmpJsApiVariantAttributes +
                    ("org.jetbrains.kotlin.kar.compression.method" to "xz")

        private val karJsRuntimeVariantAttributes =
            kmpJsRuntimeVariantAttributes +
                    ("org.jetbrains.kotlin.kar.compression.method" to "xz")
    }
}
