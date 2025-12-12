/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.internal.extensions.core.extra
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertContainsNoTaskWithName
import org.jetbrains.kotlin.gradle.util.assertContainsTaskWithName
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.Test
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Isolated("Modifies system properties")
class UnsupportedKotlinNativeHostTest {

    @Test
    fun `test jvm project configuration`() {
        with(buildProjectWithMPP()) {
            configureRepositoriesForTests()
            multiplatformExtension.jvm()
            evaluate()
            assertNoDiagnostics(KotlinToolingDiagnostics.NativeHostNotSupportedError)
        }
    }

    @Test
    fun `test project configuration on Linux Arm64 host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "aarch64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Windows Arm64 host`() {
        withModifiedSystemProperties("os.name" to "Windows", "os.arch" to "arm64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux X64 host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Windows X64 host`() {
        withModifiedSystemProperties("os.name" to "Windows", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertNoDiagnostics(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on FreeBSD host`() {
        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux RISC-V host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "riscv64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux MIPS host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "mips64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Solaris host`() {
        withModifiedSystemProperties("os.name" to "SunOS", "os.arch" to "x86_64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test project configuration on Linux PowerPC host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "ppc64le") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                multiplatformExtension.linuxX64()
                evaluate()
                assertContainsDiagnostic(KotlinToolingDiagnostics.NativeHostNotSupportedError)
            }
        }
    }

    @Test
    fun `test publishing is enabled with override property on unsupported (FreeBSD) host`() {
        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
            with(buildProjectWithMPP(preApplyCode = {
                project.extra.set(
                    PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_ALLOW_MULTIPLATFORM_PUBLICATIONS_ON_UNSUPPORTED_HOST,
                    "true"
                )
            })) {
                configureRepositoriesForTests()
                plugins.apply("maven-publish")
                kotlin {
                    jvm()
                    macosArm64() // Unsupported
                    linuxX64()   // Unsupported
                    js { browser() }
                }
                evaluate()

                assertNoDiagnostics(KotlinToolingDiagnostics.PublishingDisabledOnUnsupportedHost)

                val publishing = extensions.getByType(PublishingExtension::class.java)
                val mavenPublications = publishing.publications.filterIsInstance<MavenPublication>()

                // 1. Assert Root Publication EXISTS
                val rootPublication = mavenPublications.firstOrNull { it.name == "kotlinMultiplatform" }
                assertNotNull(rootPublication, "Root publication should exist when override is active")

                // 2. Assert Supported targets (JVM/JS) ARE published
                assertNotNull(mavenPublications.find { it.name == "jvm" }, "JVM should be published")
                assertNotNull(mavenPublications.find { it.name == "js" }, "JS should be published")

                // 3. Assert Native targets are STILL NOT published (physically impossible)
                assertNull(
                    mavenPublications.find { it.name == "macosArm64" },
                    "Native target cannot be published on FreeBSD even with override"
                )

                // 4. Verify Tasks
                assertContainsTaskWithName("publishKotlinMultiplatformPublicationToMavenLocal")
                assertContainsTaskWithName("publishJvmPublicationToMavenLocal")
            }
        }
    }

    @Test
    @OptIn(ExperimentalWasmDsl::class)
    fun `test publishing not possible on unsupported (FreeBSD) host`() {
        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                plugins.apply("maven-publish")
                kotlin {
                    jvm()
                    macosArm64()
                    linuxX64()
                    mingwX64()
                    js { browser() }
                    wasmJs { browser() }
                    wasmWasi { nodejs() }
                }
                evaluate()

                assertContainsDiagnostic(KotlinToolingDiagnostics.PublishingDisabledOnUnsupportedHost)

                val publishing = extensions.getByType(PublishingExtension::class.java)
                val mavenPublications = publishing.publications.filterIsInstance<MavenPublication>()

                // 1. Assert NO publications exist
                // Since the host (FreeBSD) is unsupported, the entire publishing setup should be aborted
                // to prevent incomplete artifacts.
                assertTrue(
                    mavenPublications.isEmpty(),
                    "Expected NO publications on unsupported host, but found: ${mavenPublications.map { it.name }}"
                )

                // 2. Specific assertion for the Root Publication
                // This confirms the 'kotlinMultiplatform' publication was explicitly skipped.
                val rootPublication = mavenPublications.firstOrNull { it.name == "kotlinMultiplatform" }
                assertNull(rootPublication, "Expected no root 'kotlinMultiplatform' publication")

                // 3. Verify Tasks are NOT created
                assertContainsNoTaskWithName("publishKotlinMultiplatformPublicationToMavenLocal")

                // JVM/JS/Wasm tasks should also be missing because publishing was globally disabled
                assertContainsNoTaskWithName("publishJvmPublicationToMavenLocal")
                assertContainsNoTaskWithName("publishJsPublicationToMavenLocal")
                assertContainsNoTaskWithName("publishWasmJsPublicationToMavenLocal")
            }
        }
    }
}