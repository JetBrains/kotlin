/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.archive

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.testing.compilationConfiguration
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.resolveSelectedVariantNames
import org.jetbrains.kotlin.gradle.testing.runtimeConfiguration
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.GradleMetadataComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.MavenComponent
import org.jetbrains.kotlin.gradle.unitTests.uklibs.generateMockRepository
import org.jetbrains.kotlin.gradle.unitTests.uklibs.kmpIosArm64VariantAttributes
import org.jetbrains.kotlin.gradle.unitTests.uklibs.kmpIosArm64MetadataVariantAttributes
import org.jetbrains.kotlin.gradle.unitTests.uklibs.kmpMetadataVariantAttributes
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
        assertNoUnresolvedDependencies(consumer)
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
        assertNoUnresolvedDependencies(consumer)
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
        assertNoUnresolvedDependencies(consumer)
    }

    @Test
    fun `legacy platform component is replaced by the kotlin archive component of a producer with a renamed js target`() {
        val repository = generateMockRepository(
            temporaryDirectory,
            listOf(
                legacyPlatformComponent("1.0", legacyModule = LEGACY_RENAMED_JS_MODULE),
                kotlinArchiveComponent("2.0", legacyModule = LEGACY_RENAMED_JS_MODULE),
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
        assertNoUnresolvedDependencies(consumer)
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
        assertNoUnresolvedDependencies(consumer)
    }

    @Test
    fun `legacy platform component is replaced by the kotlin archive component in all configurations of a native framework`() {
        val repository = generateMockRepository(
            temporaryDirectory,
            listOf(
                legacyPlatformComponent("1.0", MockPlatform.IOS_ARM64),
                kotlinArchiveComponent("2.0", MockPlatform.IOS_ARM64),
                intermediateComponent(
                    "intermediateLibrary",
                    dependency(LEGACY_IOS_ARM64_MODULE, "1.0"),
                    MockPlatform.IOS_ARM64,
                ),
            )
        )

        val consumer = buildProjectWithMPP(
            preApplyCode = {
                enableDefaultStdlibDependency(false)
            },
        ) {
            repositories.maven { it.setUrl(repository) }
            kotlin {
                iosArm64 {
                    binaries.framework {
                        export("$GROUP:library:2.0")
                    }
                }
                sourceSets.commonMain.dependencies {
                    api("$GROUP:intermediateLibrary:1.0")
                    api("$GROUP:library:2.0")
                }
            }
        }.evaluate()

        assertEquals(
            mapOf(
                "$GROUP:intermediateLibrary:1.0" to IOS_ARM64_API_VARIANT,
                "$GROUP:library:2.0" to IOS_ARM64_API_VARIANT,
            ).prettyPrinted,
            consumer.iosArm64MainCompilation.internal.configurations.compileDependencyConfiguration
                .resolveSelectedVariantNames().prettyPrinted,
        )
        assertNoUnresolvedDependencies(consumer)
    }

    private fun assertNoUnresolvedDependencies(consumer: Project) {
        assertEquals(
            emptyMap<String, List<String>>().prettyPrinted,
            consumer.configurations
                .filter { it.isCanBeResolved }
                .associate { it.name to it.unresolvedDependencies() }
                .filterValues { it.isNotEmpty() }
                .prettyPrinted,
        )
    }

    private fun Configuration.unresolvedDependencies(): List<String> {
        val resolutionResult = incoming.resolutionResult

        return resolutionResult.allDependencies
            .filterIsInstance<UnresolvedDependencyResult>()
            .map { it.requested }
            .filter { it is ModuleComponentSelector && it.group == GROUP }
            .map { "unresolved ${it.displayName}" }
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

    private val Project.iosArm64MainCompilation
        get() = (multiplatformExtension.targets.getByName("iosArm64") as KotlinNativeTarget)
            .compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    private enum class MockPlatform(
        val legacyModule: String,
        val variants: Map<String, Map<String, String>>,
        val hostSpecificMetadataVariants: Map<String, Map<String, String>> = emptyMap(),
    ) {
        JS(
            legacyModule = LEGACY_JS_MODULE,
            variants = mapOf(
                "jsApiElements-published" to kmpJsApiVariantAttributes,
                "jsRuntimeElements-published" to kmpJsRuntimeVariantAttributes,
            ),
        ),
        IOS_ARM64(
            legacyModule = LEGACY_IOS_ARM64_MODULE,
            variants = mapOf(IOS_ARM64_API_VARIANT to kmpIosArm64VariantAttributes),
            hostSpecificMetadataVariants = mapOf(IOS_ARM64_METADATA_VARIANT to kmpIosArm64MetadataVariantAttributes),
        );

        val legacyVariants: Map<String, Map<String, String>>
            get() = variants + hostSpecificMetadataVariants
    }

    private fun legacyPlatformComponent(
        version: String,
        platform: MockPlatform = MockPlatform.JS,
        legacyModule: String = platform.legacyModule,
    ) = gradleComponent(
        module = legacyModule,
        version = version,
        variants = platform.legacyVariants.map { (name, attributes) -> variant(name, attributes) },
    )

    private fun kotlinArchiveComponent(
        version: String,
        platform: MockPlatform = MockPlatform.JS,
        legacyModule: String = platform.legacyModule,
    ): GradleComponent {
        val capabilities = listOf(
            GradleMetadataComponent.Capability(GROUP, legacyModule, version),
            GradleMetadataComponent.Capability(GROUP, "library", version),
        )
        return gradleComponent(
            module = "library",
            version = version,
            variants = buildList {
                platform.variants.mapTo(this) { (name, attributes) ->
                    variant(name, attributes + karCompressionAttribute, capabilities = capabilities)
                }
                add(variant("metadataApiElements-published", kmpMetadataVariantAttributes + karCompressionAttribute))
            },
        )
    }

    private fun intermediateComponent(
        module: String,
        dependency: GradleMetadataComponent.Dependency,
        platform: MockPlatform = MockPlatform.JS,
    ) = gradleComponent(
        module = module,
        version = "1.0",
        variants = platform.legacyVariants.map { (name, attributes) -> variant(name, attributes, listOf(dependency)) } +
                variant("metadataApiElements", kmpMetadataVariantAttributes),
    )

    private fun dependency(module: String, version: String) = GradleMetadataComponent.Dependency(
        group = GROUP,
        module = module,
        version = GradleMetadataComponent.Version(version),
    )

    private fun variant(
        name: String,
        attributes: Map<String, String>,
        dependencies: List<GradleMetadataComponent.Dependency> = emptyList(),
        capabilities: List<GradleMetadataComponent.Capability>? = null,
    ) = GradleMetadataComponent.Variant(
        name = name,
        attributes = attributes,
        dependencies = dependencies,
        capabilities = capabilities,
    )

    private fun gradleComponent(
        module: String,
        version: String,
        variants: List<GradleMetadataComponent.Variant>,
    ) = GradleComponent(
        GradleMetadataComponent(
            component = GradleMetadataComponent.Component(GROUP, module, version),
            variants = variants,
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

    companion object {
        private const val GROUP = "kotlinArchiveTest"
        private const val DEFAULT_JS_TARGET_NAME = "js"
        private const val RENAMED_JS_TARGET_NAME = "custom"
        private const val LEGACY_JS_MODULE = "library-js"
        private const val LEGACY_RENAMED_JS_MODULE = "library-custom"
        private const val LEGACY_IOS_ARM64_MODULE = "library-iosarm64"
        private const val IOS_ARM64_API_VARIANT = "iosArm64ApiElements-published"
        private const val IOS_ARM64_METADATA_VARIANT = "iosArm64MetadataElements-published"

        private val karCompressionAttribute = "org.jetbrains.kotlin.kar.compression.method" to "xz"
    }
}
