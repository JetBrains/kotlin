/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.util.DependencyDirectories

import kotlin.test.BeforeTest
import kotlin.test.Test

class NativeDownloadIT : BaseGradleIT() {
    override val defaultGradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    private val currentCompilerVersion = NativeCompilerDownloader.DEFAULT_KONAN_VERSION

    companion object {
        private const val KOTLIN_SPACE_DEV = "https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev"
        private const val MAVEN_CENTRAL = "https://cache-redirector.jetbrains.com/maven-central"
    }

    private fun mavenUrl(): String = when (currentCompilerVersion.meta) {
        MetaVersion.DEV -> KOTLIN_SPACE_DEV
        MetaVersion.RELEASE, MetaVersion.RC, MetaVersion("RC2"), MetaVersion.BETA -> MAVEN_CENTRAL
        else -> throw IllegalStateException("Not a published version $currentCompilerVersion")
    }

    @BeforeTest
    fun deleteInstalledDistributions() {
        val currentOsName = HostManager.platformName()
        val lightDistDir = DependencyDirectories.localKonanDir
            .resolve("kotlin-native-$currentOsName-$currentCompilerVersion")
        val prebuiltDistDir = DependencyDirectories.localKonanDir
            .resolve("kotlin-native-prebuilt-$currentOsName-$currentCompilerVersion")

        listOf(lightDistDir, prebuiltDistDir).forEach {
            it.deleteRecursively()
        }
    }

    @Test
    fun `download prebuilt Native bundle with maven`() {
        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                """
                    kotlin.native.distribution.baseDownloadUrl=${mavenUrl()}
                    kotlin.native.distribution.downloadFromMaven=true
                """.trimIndent()
            )
            build("assemble") {
                assertSuccessful()
                assertContains("Unpack Kotlin/Native compiler to ")
                assertNotContains("Generate platform libraries for ")
            }
        }
    }

    @Test
    fun `download light Native bundle with maven`() {
        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                """
                    kotlin.native.distribution.baseDownloadUrl=${mavenUrl()}
                    kotlin.native.distribution.downloadFromMaven=true
                """.trimIndent()
            )
            build("assemble", "-Pkotlin.native.distribution.type=light") {
                assertSuccessful()
                assertContains("Unpack Kotlin/Native compiler to ")
                assertContains("Generate platform libraries for ")
            }
        }
    }

    @Test
    fun `download from maven specified in the build`() {
        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                """
                    kotlin.native.distribution.downloadFromMaven=true
                """.trimIndent()
            )
            gradleBuildScript().let {
                val text = it.readText()
                    .replaceFirst(
                        "mavenLocal()",
                        """
                           mavenLocal()
                           maven("${mavenUrl()}")
                        """.trimIndent()
                    )
                it.writeText(text)
            }

            build("assemble") {
                assertSuccessful()
                assertContains("Unpack Kotlin/Native compiler to ")
            }
        }
    }

    @Test
    fun `download from maven should fall if there is no such build in the default repos`() {
        with(transformNativeTestProjectWithPluginDsl("native-download-maven")) {
            gradleProperties().appendText(
                """
                    kotlin.native.version=1.8.0-dev-1234
                    kotlin.native.distribution.downloadFromMaven=true
                """.trimIndent()
            )
            build("assemble") {
                assertContains("Could not find org.jetbrains.kotlin:kotlin-native")
                assertFailed()
            }
        }
    }
}