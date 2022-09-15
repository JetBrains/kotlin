/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.addBuildEventsListenerRegistryMock
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.enableKpmModelMapping
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.tooling.buildKotlinToolingMetadataTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.toJsonString
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.disableLegacyWarning
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.*
import kotlin.test.*

private fun Project.kotlinToolingMetadataOfModule(moduleName: String): KotlinToolingMetadata {
    val module = kpmModules.getByName(moduleName)
    return module.buildKotlinToolingMetadataTask!!.get().kotlinToolingMetadata
}

private val Project.kotlinToolingMetadataOfMainModule get() = kotlinToolingMetadataOfModule(GradleKpmModule.MAIN_MODULE_NAME)

class BuildKotlinToolingMetadataTest : AbstractKpmExtensionTest() {
    @Test
    fun `multiple targets`() {
        // Given
        with(kotlin) {
            mainAndTest {
                jvm
                val linux = fragments.create("linux")
                fragments.create<GradleKpmLinuxX64Variant>("linuxX64").apply { refines(linux) }
                fragments.create<GradleKpmLinuxArm64Variant>("linuxArm64").apply { refines(linux) }
                // No JS & Android variants available at the moment, only through [LegacyMappedVariant] which is tested below
            }
        }

        // When
        val metadata = project.kotlinToolingMetadataOfMainModule

        // Then
        assertEquals("Gradle", metadata.buildSystem)
        assertEquals(project.gradle.gradleVersion, metadata.buildSystemVersion)
        assertEquals(KotlinPm20PluginWrapper::class.java.canonicalName, metadata.buildPlugin)
        assertEquals(project.getKotlinPluginVersion(), metadata.buildPluginVersion)
        assertEquals(3, metadata.projectTargets.size, "Expected 3 targets in KPM")

        val jvmTarget = metadata.projectTargets.single { it.platformType == jvm.name }
        assertEquals(GradleKpmJvmVariant::class.decoratedClassCanonicalName, jvmTarget.target)

        val nativeTargets = metadata.projectTargets.filter { it.platformType == native.name }.map { it.target }.toSet()
        assertEquals(
            setOf(
                GradleKpmLinuxArm64Variant::class.decoratedClassCanonicalName,
                GradleKpmLinuxX64Variant::class.decoratedClassCanonicalName,
            ),
            nativeTargets
        )
    }
}

class KotlinToolingMetadataWithModelMappingTest {
    private val project = ProjectBuilder.builder().build().also { addBuildEventsListenerRegistryMock(it) } as ProjectInternal
    private val multiplatformExtension get() = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
    private fun getKotlinToolingMetadata() = project.kotlinToolingMetadataOfMainModule

    @BeforeTest
    fun setup() {
        project.enableKpmModelMapping()
    }

    @Test
    fun `multiplatform empty setup`() {
        project.plugins.apply("kotlin-multiplatform")

        val metadata = getKotlinToolingMetadata()

        assertEquals("Gradle", metadata.buildSystem)
        assertEquals(project.gradle.gradleVersion, metadata.buildSystemVersion)
        assertEquals(KotlinMultiplatformPluginWrapper::class.java.canonicalName, metadata.buildPlugin)
        assertEquals(project.getKotlinPluginVersion(), metadata.buildPluginVersion)
        assertEquals(0, metadata.projectTargets.size, "Expected no targets in KPM")
        assertTrue(metadata.toJsonString().isNotBlank(), "Expected non blank json representation")
    }

    @Test
    fun `multiplatform JS JVM Android linuxX64 setup`() {
        project.plugins.apply("com.android.application")
        project.plugins.apply("kotlin-multiplatform")

        disableLegacyWarning(project)

        val android = project.extensions.getByType(ApplicationExtension::class.java)
        val kotlin = multiplatformExtension

        android.compileSdk = 31
        kotlin.android()
        kotlin.jvm()
        kotlin.js {
            nodejs()
            browser()
        }
        kotlin.linuxX64()

        project.evaluate()

        val metadata = getKotlinToolingMetadata()
        assertEquals(KotlinMultiplatformPluginWrapper::class.java.canonicalName, metadata.buildPlugin)

        assertFalse("Kotlin Tooling Metadata generated from KPM should not contain target of 'common' platform") {
            metadata.projectTargets.any { it.platformType == common.name }
        }

        val expectedTargets = mapOf(
            androidJvm to GradleKpmLegacyMappedVariantWithRuntime::class,
            jvm to GradleKpmJvmVariant::class,
            js to GradleKpmLegacyMappedVariantWithRuntime::class,
            native to GradleKpmLinuxX64Variant::class
        )

        assertEquals(
            expectedTargets.keys.map { it.name }.sorted(),
            metadata.projectTargets.map { it.platformType }.sorted()
        )

        expectedTargets.forEach { (platformType, targetClass) ->
            assertEquals(
                targetClass.decoratedClassCanonicalName,
                metadata.projectTargets.single { it.platformType == platformType.name }.target,
                "Platform '$platformType' has different target class"
            )
        }

        assertTrue(metadata.projectSettings.isKPMEnabled, "projectSettings.isKPMEnabled must be set")
    }

    @Test
    fun `multiplatform Android target with different source and target compatibility`() {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(BaseExtension::class.java)
        val kotlin = multiplatformExtension
        android.compileSdkVersion(28)
        kotlin.android()
        android.compileOptions.sourceCompatibility = JavaVersion.VERSION_1_6
        android.compileOptions.targetCompatibility = JavaVersion.VERSION_1_8
        project.evaluate()

        val androidTargetMetadata = getKotlinToolingMetadata()
            .projectTargets.single { it.platformType == androidJvm.name }

        assertEquals("1.6", androidTargetMetadata.extras.android?.sourceCompatibility)
        assertEquals("1.8", androidTargetMetadata.extras.android?.targetCompatibility)
    }

    @Test
    fun `multiplatform JVM with different targets`() {
        project.plugins.apply("kotlin-multiplatform")
        val kotlin = multiplatformExtension
        val jvm = kotlin.jvm()
        jvm.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).compilerOptions.options.jvmTarget.set(JvmTarget.JVM_12)
        jvm.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME).compilerOptions.options.jvmTarget.set(JvmTarget.JVM_10)

        assertEquals(
            "12", getKotlinToolingMetadata().projectTargets
                .single { it.platformType == KotlinPlatformType.jvm.name }.extras.jvm?.jvmTarget,
            "Expected jvmTarget of main compilation"
        )
    }

    @Test
    fun `multiplatform with native target`() {
        project.plugins.apply("kotlin-multiplatform")
        val kotlin = multiplatformExtension
        kotlin.linuxX64()

        val metadata = getKotlinToolingMetadata()
        val linuxTarget = metadata.projectTargets.single { it.platformType == native.name }
        assertEquals(KonanTarget.LINUX_X64.name, linuxTarget.extras.native?.konanTarget)
        assertEquals(project.konanVersion.toString(), linuxTarget.extras.native?.konanVersion)
        assertEquals(KotlinAbiVersion.CURRENT.toString(), linuxTarget.extras.native?.konanAbiVersion)
    }

    @Test
    fun `multiple native targets`() {
        project.plugins.apply("kotlin-multiplatform")
        val kotlin = multiplatformExtension
        kotlin.linuxX64()
        kotlin.linuxArm64()

        val metadata = getKotlinToolingMetadata()
        val nativeTargets = metadata.projectTargets.filter { it.platformType == native.name }.sortedBy { it.extras.native?.konanTarget }
        assertEquals(2, nativeTargets.size, "Expected only two native targets")
        val (linuxArm64, linuxX64) = nativeTargets

        assertEquals(
            "linux_arm64",
            linuxArm64.extras.native?.konanTarget
        )
        assertEquals(
            "linux_x64",
            linuxX64.extras.native?.konanTarget
        )

        assertEquals(
            GradleKpmLinuxArm64Variant::class.decoratedClassCanonicalName,
            linuxArm64.target
        )
        assertEquals(
            GradleKpmLinuxX64Variant::class.decoratedClassCanonicalName,
            linuxX64.target
        )
    }

}
