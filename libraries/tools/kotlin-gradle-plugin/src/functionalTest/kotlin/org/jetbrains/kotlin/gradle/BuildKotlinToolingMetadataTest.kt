/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.toJsonString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildKotlinToolingMetadataTest {

    private val project = ProjectBuilder.builder().build() as ProjectInternal
    private val multiplatform get() = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
    private val js get() = project.extensions.getByType(KotlinJsProjectExtension::class.java)

    @Test
    fun `multiplatform empty setup`() {
        project.plugins.apply("kotlin-multiplatform")

        val metadata = getKotlinToolingMetadata()

        assertEquals("Gradle", metadata.buildSystem)
        assertEquals(project.gradle.gradleVersion, metadata.buildSystemVersion)
        assertEquals(KotlinMultiplatformPluginWrapper::class.java.canonicalName, metadata.buildPlugin)
        assertEquals(project.getKotlinPluginVersion().toString(), metadata.buildPluginVersion)
        assertEquals(1, metadata.projectTargets.size, "Expected one target (metadata)")
        assertTrue(
            KotlinMetadataTarget::class.java.isAssignableFrom(Class.forName(metadata.projectTargets.single().target)),
            "Expect target to be implement ${KotlinMetadataTarget::class.simpleName}"
        )
        assertEquals(
            KotlinPlatformType.common.name, metadata.projectTargets.single().platformType
        )
        assertTrue(metadata.toJsonString().isNotBlank(), "Expected non blank json representation")
    }

    @Test
    fun `multiplatform JS JVM Android setup`() {
        project.plugins.apply("com.android.application")
        project.plugins.apply("kotlin-multiplatform")

        val android = project.extensions.getByType(BaseExtension::class.java)
        val kotlin = multiplatform

        android.compileSdkVersion(28)
        kotlin.android()
        kotlin.jvm()
        kotlin.js {
            nodejs()
            browser()
        }

        project.evaluate()

        val metadata = getKotlinToolingMetadata()
        assertEquals(KotlinMultiplatformPluginWrapper::class.java.canonicalName, metadata.buildPlugin)

        assertEquals(
            listOf(KotlinPlatformType.common, KotlinPlatformType.androidJvm, KotlinPlatformType.jvm, KotlinPlatformType.js)
                .map { it.name }.sorted(),
            metadata.projectTargets.map { it.platformType }.sorted()
        )
    }

    @Test
    fun `multiplatform Android target with different source and target compatibility`() {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("com.android.application")
        val android = project.extensions.getByType(BaseExtension::class.java)
        val kotlin = multiplatform
        android.compileSdkVersion(28)
        kotlin.android()
        android.compileOptions.setSourceCompatibility(JavaVersion.VERSION_1_6)
        android.compileOptions.setTargetCompatibility(JavaVersion.VERSION_1_8)
        project.evaluate()

        val androidTargetMetadata = getKotlinToolingMetadata()
            .projectTargets.single { it.platformType == KotlinPlatformType.androidJvm.name }

        assertEquals("1.6", androidTargetMetadata.extras["sourceCompatibility"])
        assertEquals("1.8", androidTargetMetadata.extras["targetCompatibility"])
    }

    @Test
    fun `multiplatform JVM with different targets`() {
        project.plugins.apply("kotlin-multiplatform")
        val kotlin = multiplatform
        val jvm = kotlin.jvm()
        jvm.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).kotlinOptions.jvmTarget = "12"
        jvm.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME).kotlinOptions.jvmTarget = "10"

        assertEquals(
            "12", getKotlinToolingMetadata().projectTargets
                .single { it.platformType == KotlinPlatformType.jvm.name }.extras["jvmTarget"],
            "Expected jvmTarget of main compilation"
        )
    }

    @Test
    fun `multiplatform with native target`() {
        project.plugins.apply("kotlin-multiplatform")
        val kotlin = multiplatform
        kotlin.linuxX64()

        val metadata = getKotlinToolingMetadata()
        val linuxTarget = metadata.projectTargets.single { it.platformType == KotlinPlatformType.native.name }
        assertEquals(KonanTarget.LINUX_X64.name, linuxTarget.extras["konanTarget"])
    }

    @Test
    fun js() {
        project.plugins.apply("org.jetbrains.kotlin.js")
        val kotlin = js
        kotlin.js { nodejs() }

        val metadata = getKotlinToolingMetadata()
        assertEquals(org.jetbrains.kotlin.gradle.plugin.KotlinJsPluginWrapper::class.java.canonicalName, metadata.buildPlugin)

        val jsTarget = metadata.projectTargets.single { it.platformType == KotlinPlatformType.js.name }
        assertEquals("true", jsTarget.extras["isNodejsConfigured"])
        assertEquals("false", jsTarget.extras["isBrowserConfigured"])
    }

    @Test
    fun jvm() {
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        val metadata = getKotlinToolingMetadata()
        assertEquals(org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper::class.java.canonicalName, metadata.buildPlugin)
    }

    private fun getKotlinToolingMetadata(): KotlinToolingMetadata {
        val task = project.tasks.named(BuildKotlinToolingMetadataTask.defaultTaskName, BuildKotlinToolingMetadataTask::class.java).get()
        return task.getKotlinToolingMetadata()
    }
}
