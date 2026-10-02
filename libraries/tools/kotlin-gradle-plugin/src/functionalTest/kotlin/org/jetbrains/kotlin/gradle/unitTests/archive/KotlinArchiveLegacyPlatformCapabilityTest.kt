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
    fun `legacy platform component is replaced by the kotlin archive component when the consumer renames the js target`() {
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
            dependencyCoordinates = listOf("$GROUP:intermediateLibrary:1.0", "$GROUP:library:2.0"),
            targetName = RENAMED_JS_TARGET_NAME,
        )

        assertEquals(
            mapOf(
                "$GROUP:intermediateLibrary:1.0" to "jsApiElements-published",
                "$GROUP:library:2.0" to "jsApiElements-published",
            ).prettyPrinted,
            consumer.jsCompileResolution(RENAMED_JS_TARGET_NAME).prettyPrinted,
        )
        assertEquals(
            mapOf(
                "$GROUP:intermediateLibrary:1.0" to "jsRuntimeElements-published",
                "$GROUP:library:2.0" to "jsRuntimeElements-published",
            ).prettyPrinted,
            consumer.jsRuntimeResolution(RENAMED_JS_TARGET_NAME).prettyPrinted,
        )
    }

    @Test
    fun `legacy platform component is replaced by the kotlin archive component of a producer with a renamed js target`() {
        val repository = generateMockRepository(
            temporaryDirectory,
            listOf(
                legacyPlatformComponent("1.0", LEGACY_RENAMED_JS_MODULE),
                kotlinArchiveComponent("2.0", LEGACY_RENAMED_JS_MODULE),
                intermediateComponent("intermediateLibrary", dependency(LEGACY_RENAMED_JS_MODULE, "1.0")),
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
        targetName: String = DEFAULT_JS_TARGET_NAME,
    ): Project = buildProjectWithMPP(
        preApplyCode = {
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)
        },
    ) {
        repositories.maven { it.setUrl(repository) }
        kotlin {
            js(targetName)
            sourceSets.commonMain.dependencies {
                dependencyCoordinates.forEach { implementation(it) }
            }
        }
    }.evaluate()

    private fun Project.jsCompileResolution(targetName: String = DEFAULT_JS_TARGET_NAME) =
        jsTarget(targetName).compilationConfiguration().resolveSelectedVariantNames()

    private fun Project.jsRuntimeResolution(targetName: String = DEFAULT_JS_TARGET_NAME) =
        jsTarget(targetName).runtimeConfiguration().resolveSelectedVariantNames()

    private fun Project.jsTarget(targetName: String) = multiplatformExtension.targets.getByName(targetName)

    private fun legacyPlatformComponent(version: String, legacyModule: String = LEGACY_JS_MODULE) = jsGradleComponent(
        module = legacyModule,
        version = version,
        apiAttributes = kmpJsApiVariantAttributes,
        runtimeAttributes = kmpJsRuntimeVariantAttributes,
    )

    private fun kotlinArchiveComponent(version: String, legacyModule: String = LEGACY_JS_MODULE) = jsGradleComponent(
        module = "library",
        version = version,
        apiAttributes = karJsApiVariantAttributes,
        runtimeAttributes = karJsRuntimeVariantAttributes,
        capabilities = listOf(
            GradleMetadataComponent.Capability(GROUP, legacyModule, version),
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
        private const val DEFAULT_JS_TARGET_NAME = "js"
        private const val RENAMED_JS_TARGET_NAME = "custom"
        private const val LEGACY_JS_MODULE = "library-js"
        private const val LEGACY_RENAMED_JS_MODULE = "library-custom"

        private val karJsApiVariantAttributes =
            kmpJsApiVariantAttributes +
                    ("org.jetbrains.kotlin.kar.compression.method" to "xz")

        private val karJsRuntimeVariantAttributes =
            kmpJsRuntimeVariantAttributes +
                    ("org.jetbrains.kotlin.kar.compression.method" to "xz")
    }
}
