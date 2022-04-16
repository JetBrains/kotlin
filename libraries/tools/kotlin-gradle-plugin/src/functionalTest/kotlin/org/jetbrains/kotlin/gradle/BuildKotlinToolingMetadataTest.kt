/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.*
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.tooling.buildKotlinToolingMetadataTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.toJsonString
import org.junit.Assume
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class BuildKotlinToolingMetadataTest {

    @Parameterized.Parameter(0)
    @JvmField
    val kpmModelMappingEnabled: Boolean = false

    private val project = ProjectBuilder.builder().build().also{ addBuildEventsListenerRegistryMock(it) } as ProjectInternal
    private val multiplatformExtension get() = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
    private val jsExtension get() = project.extensions.getByType(KotlinJsProjectExtension::class.java)

    init {
        project.extensions.getByType(ExtraPropertiesExtension::class.java).set("kotlin.mpp.enableKotlinToolingMetadataArtifact", "true")
    }

    @Before
    fun setup() {
        project.enableKpmModelMapping(kpmModelMappingEnabled)
    }

    @Test
    fun `multiplatform empty setup`() {
        project.plugins.apply("kotlin-multiplatform")

        val metadata = getKotlinToolingMetadata()

        assertEquals("Gradle", metadata.buildSystem)
        assertEquals(project.gradle.gradleVersion, metadata.buildSystemVersion)
        assertEquals(KotlinMultiplatformPluginWrapper::class.java.canonicalName, metadata.buildPlugin)
        assertEquals(project.getKotlinPluginVersion(), metadata.buildPluginVersion)
        assertEquals(1, metadata.projectTargets.size, "Expected one target (metadata)")
        val targetMetadata = metadata.projectTargets.single()
        if (kpmModelMappingEnabled) {
            assertTrue(
                KotlinGradleFragment::class.java.isAssignableFrom(Class.forName(targetMetadata.target)),
                "Expect target to be implement ${KotlinGradleFragment::class.simpleName}"
            )
        } else {
            assertTrue(
                KotlinMetadataTarget::class.java.isAssignableFrom(Class.forName(targetMetadata.target)),
                "Expect target to be implement ${KotlinMetadataTarget::class.simpleName}"
            )
        }
        assertEquals(common.name, targetMetadata.platformType)
        assertTrue(metadata.toJsonString().isNotBlank(), "Expected non blank json representation")
    }

    @Test
    fun `multiplatform JS JVM Android linuxX64 setup`() {
        project.plugins.apply("com.android.application")
        project.plugins.apply("kotlin-multiplatform")

        val android = project.extensions.getByType(BaseExtension::class.java)
        val kotlin = multiplatformExtension

        android.compileSdkVersion(28)
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

        val expectedTargets = if (kpmModelMappingEnabled) {
            mapOf(
                common to KotlinGradleFragmentInternal::class,
                androidJvm to LegacyMappedVariantWithRuntime::class,
                jvm to KotlinJvmVariant::class,
                js to LegacyMappedVariantWithRuntime::class,
                native to KotlinLinuxX64Variant::class
            )
        } else {
            mapOf(
                common to KotlinMetadataTarget::class,
                androidJvm to KotlinAndroidTarget::class,
                jvm to KotlinJvmTarget::class,
                js to KotlinJsTarget::class,
                native to KotlinNativeTargetWithHostTests::class
            )
        }

        assertEquals(
            expectedTargets.keys.map { it.name }.sorted(),
            metadata.projectTargets.map { it.platformType }.sorted()
        )

        expectedTargets.forEach { (platformType, targetClass) ->
            assertEquals(
                targetClass.java.canonicalName,
                metadata.projectTargets.single { it.platformType == platformType.name }.target,
                "Platform '$platformType' has different target class"
            )
        }

        assertEquals(kpmModelMappingEnabled, metadata.projectSettings.isKPMEnabled, "projectSettings.isKPMEnabled must be set")
    }

    @Test
    fun `multiplatform Android target with different source and target compatibility`() {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(BaseExtension::class.java)
        val kotlin = multiplatformExtension
        android.compileSdkVersion(28)
        kotlin.android()
        android.compileOptions.setSourceCompatibility(JavaVersion.VERSION_1_6)
        android.compileOptions.setTargetCompatibility(JavaVersion.VERSION_1_8)
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
        jvm.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).kotlinOptions.jvmTarget = "12"
        jvm.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME).kotlinOptions.jvmTarget = "10"

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
    fun js() {
        Assume.assumeFalse("KPM model mapping is not yet supported in single-platform projects", kpmModelMappingEnabled)
        project.plugins.apply("org.jetbrains.kotlin.js")
        val kotlin = jsExtension
        kotlin.js { nodejs() }

        val metadata = getKotlinToolingMetadata()
        assertEquals(org.jetbrains.kotlin.gradle.plugin.KotlinJsPluginWrapper::class.java.canonicalName, metadata.buildPlugin)

        val jsTarget = metadata.projectTargets.single { it.platformType == js.name }
        assertEquals(true, jsTarget.extras.js?.isNodejsConfigured)
        assertEquals(false, jsTarget.extras.js?.isBrowserConfigured)
    }

    @Test
    fun jvm() {
        Assume.assumeFalse("KPM model mapping is not yet supported in single-platform projects", kpmModelMappingEnabled)
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        val metadata = getKotlinToolingMetadata()
        assertEquals(org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper::class.java.canonicalName, metadata.buildPlugin)
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

        if (kpmModelMappingEnabled) {
            assertEquals(
                KotlinLinuxArm64Variant::class.java.canonicalName,
                linuxArm64.target
            )
            assertEquals(
                KotlinLinuxX64Variant::class.java.canonicalName,
                linuxX64.target
            )
        } else {
            assertEquals(
                KotlinNativeTarget::class.java.canonicalName,
                linuxArm64.target
            )
            assertEquals(
                KotlinNativeTargetWithHostTests::class.java.canonicalName,
                linuxX64.target
            )
        }
    }

    private fun getKotlinToolingMetadata(): KotlinToolingMetadata {
        val task = project.buildKotlinToolingMetadataTask?.get() ?: error("No ${BuildKotlinToolingMetadataTask.defaultTaskName} task")
        return task.kotlinToolingMetadata
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KPM-Model-Mapping:{0}")
        fun parameters() = listOf(
            arrayOf(false),
            arrayOf(true),
        )
    }
}
