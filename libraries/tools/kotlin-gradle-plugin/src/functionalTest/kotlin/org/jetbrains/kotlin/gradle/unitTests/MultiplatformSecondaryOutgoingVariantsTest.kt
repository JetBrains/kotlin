/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.NON_PACKED_KLIB_VARIANT_NAME
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.toAttribute
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlinCompileConfig.Companion.CLASSES_SECONDARY_VARIANT_NAME
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableDefaultJsDomApiDependency
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.enableNonPackedKlibsUsage
import org.jetbrains.kotlin.gradle.util.enableSecondaryJvmClassesVariant
import org.jetbrains.kotlin.gradle.util.osVariantSeparatorsPathString
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import kotlin.test.*

class MultiplatformSecondaryOutgoingVariantsTest {

    private fun buildProjectWithMPPAndJvmClassesVariant(
        code: Project.() -> Unit = {},
    ) = buildProjectWithMPP(
        preApplyCode = {
            project.enableSecondaryJvmClassesVariant()
        },
        code = code
    )

    private fun buildKmpProjectWithKlibTargets(
        projectBuilder: ProjectBuilder.() -> Unit = { },
        evaluate: Boolean = true,
        preApplyCode: Project.() -> Unit = {},
        code: Project.() -> Unit = {},
    ): ProjectInternal {
        val project = buildProjectWithMPP(projectBuilder = projectBuilder, preApplyCode = preApplyCode) {
            @OptIn(ExperimentalWasmDsl::class)
            with(multiplatformExtension) {
                linuxX64 {
                    compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.create("dummy")
                }
                js()
                wasmJs()
                wasmWasi()
                applyDefaultHierarchyTemplate()
            }
            repositories.mavenLocal()
        }
        code(project)
        if (evaluate) {
            project.evaluate()
        }
        return project
    }

    @Test
    fun shouldAddSecondaryJvmClassesVariantForJvmApiConfigurations() {
        val project = buildProjectWithMPPAndJvmClassesVariant {
            with(multiplatformExtension) {
                jvm()
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.assertJvmClassesVariants(expectedArtifactsSize = 2)
    }

    @Test
    fun shouldAddSecondaryJvmClassesVariantForJvmApiConfigurationsWithJavaEnabled() {
        val project = buildProjectWithMPPAndJvmClassesVariant {
            with(multiplatformExtension) {
                jvm {
                    @Suppress("DEPRECATION")
                    withJava()
                }
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.assertJvmClassesVariants(expectedArtifactsSize = 3, isLegacyJavaCompilationEnabled = true)
    }

    @Test
    fun shouldAddSecondaryJvmClassesVariantForJvmApiConfigurationsWithJavaEnabledAndJavaLibraryPluginApplied() {
        val project = buildProjectWithMPPAndJvmClassesVariant {
            plugins.apply("java-library")

            with(multiplatformExtension) {
                jvm {
                    @Suppress("DEPRECATION")
                    withJava()
                }
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.assertJvmClassesVariants(expectedArtifactsSize = 3, isLegacyJavaCompilationEnabled = true)
    }

    @Test
    fun shouldAddSecondaryJvmClassesVariantForJvmApiConfigurationsWithJavaTestFixturesApplied() {
        val project = buildProjectWithMPPAndJvmClassesVariant {
            plugins.apply("java-test-fixtures")

            with(multiplatformExtension) {
                jvm {
                    @Suppress("DEPRECATION")
                    withJava()
                }
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.assertJvmClassesVariants(expectedArtifactsSize = 3, isLegacyJavaCompilationEnabled = true)
    }

    @Test
    fun shouldNotAddSecondaryJvmClassesVariantByDefault() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.jvmApiConfigurations.forEach { configuration ->
            assertNull(configuration.outgoing.variants.findByName(CLASSES_SECONDARY_VARIANT_NAME))
        }
    }

    @Test
    fun nonPackedKlibsUsageMayBeDisabled() {
        val project = buildKmpProjectWithKlibTargets(preApplyCode = { project.enableNonPackedKlibsUsage(false) })
        val apiConfigurations = project.getKlibApiConfigurations()
        project.assertKlibWithoutNonPackedVariant(apiConfigurations)
        val runtimeConfigurations = project.getKlibRuntimeConfigurations()
        project.assertKlibWithoutNonPackedVariant(runtimeConfigurations)
    }

    @Test
    fun shouldAddNonPackedKlibVariantByDefaultForKlibTargets() {
        val project = buildKmpProjectWithKlibTargets()
        val apiConfigurations = project.getKlibApiConfigurations()
        project.assertKlibWithNonPackedVariants(apiConfigurations)
        val runtimeConfigurations = project.getKlibRuntimeConfigurations()
        project.assertKlibWithNonPackedVariants(runtimeConfigurations)
    }

    @Test
    fun klibPackagingAttributeIsNotPublished() {
        val project = buildKmpProjectWithKlibTargets {
            plugins.apply("maven-publish")
        }
        project.assertKlibPackagingAttributeNotPublished()
    }

    @Test
    fun klibPackagingAttributeIsNotPublishedWhenNonPackedKlibsAreNotUsed() {
        val project = buildKmpProjectWithKlibTargets(preApplyCode = { project.enableNonPackedKlibsUsage(false) }) {
            plugins.apply("maven-publish")
        }
        project.assertKlibPackagingAttributeNotPublished()
    }

    @Test
    fun checkNonPackedKlibApiVariantResolved() {
        checkNonPackedKlibVariantResolved { it.compileDependencyFiles }
    }

    @Test
    fun checkNonPackedKlibRuntimeVariantResolved() {
        val hasRuntimeClasspath = setOf(KotlinPlatformType.wasm, KotlinPlatformType.js)
        checkNonPackedKlibVariantResolved(hasDependencies = { it.platformType in hasRuntimeClasspath }) { it.runtimeDependencyFiles }
    }

    @Test
    fun checkDefaultResolvedVariant() {
        val project = buildKmpProjectWithKlibTargets(projectBuilder = { withName("app") }, preApplyCode = {
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)
            enableNonPackedKlibsUsage(true)
        })
        val configurations = project.configurations
        for (target in project.multiplatformExtension.targets) {
            if (target.platformType !in PLATFORM_TYPES_SUPPORTING_NON_PACKED_KLIB) continue
            val myDependencyScope = configurations.dependencyScope("${target.name}DependencyScope").get()
            val myResolvable = configurations.resolvable("${target.name}Resolvable") {
                it.extendsFrom(myDependencyScope)
                it.attributes {
                    it.attributes.attribute(KotlinPlatformType.attribute, target.platformType)
                    if (target is KotlinNativeTarget) {
                        it.attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.konanTarget.name)
                    }
                    if (target.platformType == KotlinPlatformType.wasm && target is KotlinJsIrTarget) {
                        val wasmType = target.wasmTargetType?.toAttribute()
                            ?: error("Wasm type attribute expected to be set for $target")
                        it.attributes.attribute(KotlinWasmTargetAttribute.wasmTargetAttribute, wasmType)
                    }
                    it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
                }
            }.get()

            project.dependencies.add(myDependencyScope.name, project.dependencies.project(project.path))

            val resolvedFiles = try {
                myResolvable.resolve()
            } catch (e: Exception) {
                throw IllegalStateException("Failed to resolve dependencies for $target", e)
            }
            for (file in resolvedFiles) {
                assert(file.invariantSeparatorsPath.matches(".*/libs/${project.name}-[a-zA-Z0-9-]+.klib".toRegex())) {
                    "Expected ${file.path} (resolved from $target) to be a klib in the packed form"
                }
            }
        }
    }

    /**
     * Covers the case:
     * * app.main -> lib.main
     * * lib.test -> app.main
     * In this case, the classpath of lib.test should not contain lib in both packed and non-packed form
     */
    private fun checkNonPackedKlibVariantResolved(
        hasDependencies: (KotlinCompilation<*>) -> Boolean = { true },
        resolvedDependencies: (KotlinCompilation<*>) -> FileCollection?,
    ) {
        val disableDefaultDependencies: Project.() -> Unit = {
            // disable external dependencies, leaving only project dependencies
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)
        }
        val app = buildKmpProjectWithKlibTargets(
            projectBuilder = { withName("app") },
            evaluate = false,
            preApplyCode = disableDefaultDependencies,
        )
        val lib = buildKmpProjectWithKlibTargets(
            projectBuilder = { withName("lib").withParent(app) },
            evaluate = false,
            preApplyCode = disableDefaultDependencies,
        ) {
            enableDefaultStdlibDependency(false)
            enableDefaultJsDomApiDependency(false)
            multiplatformExtension.sourceSets.commonTest.get().dependencies {
                implementation(project(":"))
            }
        }
        app.multiplatformExtension.sourceSets.commonMain.get().dependencies {
            implementation(project(":lib"))
        }
        app.evaluate()
        lib.evaluate()

        fun assertNoPackedKlibsInResolvedDependencies(targets: Iterable<KotlinTarget>) {
            for (target in targets) {
                if (target.platformType !in PLATFORM_TYPES_SUPPORTING_NON_PACKED_KLIB) continue
                for (compilation in target.compilations) {
                    if (hasDependencies(compilation)) {
                        val files = (resolvedDependencies(compilation)?.files
                            ?: error("$compilation should have non-null dependency configuration"))
                        for (file in files) {
                            assert(file.extension != "klib") { // checks if it is a directory by extension
                                "Expected $file to be a directory (non-packed klib) of $compilation"
                            }
                        }
                    } else {
                        assertNull(resolvedDependencies(compilation), "$compilation should not have dependency configuration")
                    }
                }
            }
        }

        assertNoPackedKlibsInResolvedDependencies(app.multiplatformExtension.targets)
        assertNoPackedKlibsInResolvedDependencies(lib.multiplatformExtension.targets)
    }

    private fun Project.getKlibApiConfigurations(): List<Configuration> {
        val usages = setOf(KotlinUsages.KOTLIN_API, KotlinUsages.KOTLIN_CINTEROP)
        val apiConfigurations = project.configurations.filter {
            it.isCanBeConsumed &&
                    it.attributes.getAttribute(KotlinPlatformType.attribute) in PLATFORM_TYPES_SUPPORTING_NON_PACKED_KLIB &&
                    it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.toString() in usages
        }
        val numberOfConfigurations =
            multiplatformExtension.targets.filter { it.platformType in PLATFORM_TYPES_SUPPORTING_NON_PACKED_KLIB }.size + multiplatformExtension.targets.filterIsInstance<KotlinNativeTarget>()
                .sumOf { it.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.size }
        assert(numberOfConfigurations == apiConfigurations.size) {
            """
                The number of consumable API configurations is unexpected. Expected to have 1 configuration per target + 1 per declared cinterop ($numberOfConfigurations in total). 
                Found: ${apiConfigurations.map { it.name }}.
                Targets: ${multiplatformExtension.targets.map { it.name }}
            """.trimIndent()
        }
        return apiConfigurations
    }

    /**
     * Returns runtime elements configurations of Klib producing targets that can have secondary outgoing variant
     */
    private fun Project.getKlibRuntimeConfigurations(): List<Configuration> {
        val platformTypes = setOf(KotlinPlatformType.js)
        val runtimeConfigurations = project.configurations.filter {
            it.isCanBeConsumed &&
                    it.attributes.getAttribute(KotlinPlatformType.attribute) in platformTypes &&
                    it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.toString() == KotlinUsages.KOTLIN_RUNTIME &&
                    it.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.toString() == Category.LIBRARY
        }
        val numberOfConfigurations =
            multiplatformExtension.targets.filter { it.platformType in platformTypes }.size
        assert(numberOfConfigurations == runtimeConfigurations.size) {
            """
                The number of consumable runtime configurations is unexpected. Expected to have 1 configuration per target ($numberOfConfigurations in total). 
                Found: ${runtimeConfigurations.map { it.name }}.
                Targets: ${multiplatformExtension.targets.map { it.name }}
            """.trimIndent()
        }
        return runtimeConfigurations
    }

    private val Project.jvmApiConfigurations
        get() = configurations.filter {
            it.isCanBeConsumed &&
                    it.attributes.getAttribute(KotlinPlatformType.attribute) == KotlinPlatformType.jvm &&
                    it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.toString() == Usage.JAVA_API
        }

    private fun Project.assertKlibWithoutNonPackedVariant(
        apiConfigurations: List<Configuration>,
    ) {
        for (configuration in apiConfigurations) {
            assertNull(
                configuration.attributes.getAttribute(KlibPackaging.ATTRIBUTE),
                "The primary variant of ${configuration.name} should not have attribute ${KlibPackaging.ATTRIBUTE.name} set to."
            )
            val nonPackedKlibVariant = configuration.outgoing.variants.findByName(NON_PACKED_KLIB_VARIANT_NAME)
            assertNull(
                nonPackedKlibVariant,
                "Expected non-packed KLIB variant to be absent in configuration ${configuration.name}"
            )
            for (packedArtifact in configuration.artifacts) {
                val packedArtifactPath = packedArtifact.file.invariantSeparatorsPath
                assert(
                    packedArtifactPath.endsWith("/build/classes/kotlin/linuxX64/main/klib/${project.name}.klib") ||
                            packedArtifactPath.matches(".*/build/classes/kotlin/linuxX64/main/cinterop/${project.name}-cinterop-[a-zA-Z0-9]+.klib".toRegex()) ||
                            packedArtifactPath.matches(".*/build/libs/${project.name}-(wasm-wasi|wasm-js|js).klib".toRegex())
                ) {
                    "Artifact $packedArtifactPath from $configuration does not match the expected patterns."
                }
            }
        }
    }

    private fun Project.assertKlibWithNonPackedVariants(
        apiConfigurations: List<Configuration>,
    ) {
        for (configuration in apiConfigurations) {
            assertEquals(
                project.objects.named(KlibPackaging::class.java, KlibPackaging.PACKED),
                configuration.attributes.getAttribute(KlibPackaging.ATTRIBUTE),
                "The primary variant of ${configuration.name} should be packed."
            )
            val nonPackedKlibVariant = configuration.outgoing.variants.findByName(NON_PACKED_KLIB_VARIANT_NAME)
            assertNotNull(nonPackedKlibVariant) {
                "Expected non-packed KLIB variant to be present in configuration ${configuration.name}"
            }
            assertEquals(
                project.objects.named(KlibPackaging::class.java, KlibPackaging.NON_PACKED),
                nonPackedKlibVariant.attributes.getAttribute(KlibPackaging.ATTRIBUTE),
                "The non-packed variant of ${configuration.name} should have proper attribute."
            )
            // cinterop also adds artifacts to main apiElements
            // also we add a fake artifact as a workaround for https://github.com/gradle/gradle/issues/29630
            for (packedArtifact in configuration.artifacts) {
                val packedArtifactPath = packedArtifact.file.invariantSeparatorsPath
                assert(
                    packedArtifactPath.matches(".*/build/libs/.*.klib".toRegex()) ||
                            packedArtifactPath.endsWith("non-existing-file-workaround-for-gradle-29630.txt")
                ) {
                    "Artifact $packedArtifactPath from $configuration does not match the expected patterns."
                }
            }
            for (nonPackedArtifact in nonPackedKlibVariant.artifacts) {
                val nonPackedArtifactPath = nonPackedArtifact.file.invariantSeparatorsPath
                assert(
                    nonPackedArtifactPath.matches(".*/build/classes/kotlin/(wasmWasi|wasmJs|linuxX64|js)/main(/klib/${project.name})?".toRegex()) ||
                            nonPackedArtifactPath.matches(".*/build/classes/kotlin/linuxX64/main/cinterop/${project.name}-cinterop-[a-zA-Z0-9]+".toRegex())
                ) {
                    "Artifact $nonPackedArtifactPath from $nonPackedKlibVariant does not match the expected patterns."
                }
            }
        }
    }

    private fun Project.assertKlibPackagingAttributeNotPublished() {
        val kotlinComponent = project.components.getByName("kotlin") as ComponentWithVariants
        val usagesByVariant = kotlinComponent.variants.associateWith { variant -> (variant as SoftwareComponentInternal).usages }
        for ((variant, usages) in usagesByVariant) {
            for (usage in usages) {
                assertNull(
                    usage.attributes.getAttribute(KlibPackaging.ATTRIBUTE),
                    "Expected no attribute ${KlibPackaging.ATTRIBUTE.name} to be published for usage context ${usage.name} of variant ${variant.name}."
                )
            }
        }
    }

    private fun Project.assertJvmClassesVariants(
        expectedArtifactsSize: Int = 2,
        apiConfigurations: List<Configuration> = jvmApiConfigurations,
        isLegacyJavaCompilationEnabled: Boolean = false,
    ) {
        assertTrue(apiConfigurations.isNotEmpty())

        apiConfigurations.forEach { configuration ->
            val classesVariant = configuration.outgoing.variants.getByName(CLASSES_SECONDARY_VARIANT_NAME)
            assertNotNull(classesVariant)
            assertEquals(expectedArtifactsSize, classesVariant.artifacts.size)

            val kotlinClasses = classesVariant.artifacts.find {
                it.file.path.endsWith("build/classes/kotlin/jvm/main".osVariantSeparatorsPathString)
            }
            assertNotNull(kotlinClasses)
            assertTrue(kotlinClasses.buildDependencies.getDependencies(null).size >= 1)

            val javaClasses = classesVariant.artifacts.find {
                it.file.path.endsWith("build/classes/java/jvmMain".osVariantSeparatorsPathString)
            }
            assertNotNull(javaClasses)
            assertTrue(javaClasses.buildDependencies.getDependencies(null).size >= 1)

            if (isLegacyJavaCompilationEnabled) {
                val javaClassesLegacy = classesVariant.artifacts.find {
                    it.file.path.endsWith("build/classes/java/main".osVariantSeparatorsPathString)
                }
                assertNotNull(javaClassesLegacy)
                assertTrue(javaClassesLegacy.buildDependencies.getDependencies(null).size >= 1)
            }
        }
    }

    companion object {
        val PLATFORM_TYPES_SUPPORTING_NON_PACKED_KLIB = setOf(KotlinPlatformType.wasm, KotlinPlatformType.native, KotlinPlatformType.js)
    }
}