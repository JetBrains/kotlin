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
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertContainsNoTaskWithName
import org.jetbrains.kotlin.gradle.util.assertContainsTaskWithName
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import kotlin.test.Test
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Isolated("Modifies system properties")
class PublishingTests {

    @Test
    fun `test publishing BLOCKED on unsupported host with Native targets`() {
        // Case: kmp (jvm, js, wasm, native) - affected: publishing forbidden by default
        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                plugins.apply("maven-publish")
                kotlin {
                    jvm()
                    js { browser() }
                    // Adding a Native target should TRIGGER the block on FreeBSD
                    linuxX64()
                }
                evaluate()

                // 1. Assert Diagnostic reported
                assertContainsDiagnostic(KotlinToolingDiagnostics.PublishingDisabledOnUnsupportedHost)

                val publishing = extensions.getByType(PublishingExtension::class.java)
                val mavenPublications = publishing.publications.filterIsInstance<MavenPublication>()

                // 2. Assert NO publications exist (blocked to prevent incomplete artifacts)
                assertTrue(
                    mavenPublications.isEmpty(),
                    "Expected NO publications on unsupported host with Native targets, but found: ${mavenPublications.map { it.name }}"
                )

                // 3. Verify Tasks are NOT created
                assertContainsNoTaskWithName("publishKotlinMultiplatformPublicationToMavenLocal")
                assertContainsNoTaskWithName("publishJvmPublicationToMavenLocal")
            }
        }
    }

    @Test
    @OptIn(ExperimentalWasmDsl::class)
    fun `test publishing ALLOWED on unsupported host with NO Native targets`() {
        // Case: kmp (jvm, js, wasm) - shouldn't be affected: compile/publishing works
        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
            with(buildProjectWithMPP()) {
                configureRepositoriesForTests()
                plugins.apply("maven-publish")
                kotlin {
                    // Only JVM/JS/Wasm targets - should be safe to publish even on FreeBSD
                    jvm()
                    js { browser() }
                    wasmJs { browser() }
                }
                evaluate()

                // 1. Assert NO Diagnostic
                assertNoDiagnostics(KotlinToolingDiagnostics.PublishingDisabledOnUnsupportedHost)

                val publishing = extensions.getByType(PublishingExtension::class.java)
                val mavenPublications = publishing.publications.filterIsInstance<MavenPublication>()

                // 2. Assert Publications EXIST
                val expectedPublications = setOf("kotlinMultiplatform", "jvm", "js", "wasmJs")
                val actualPublications = mavenPublications.map { it.name }.toSet()

                assertEquals(expectedPublications, actualPublications, "Expected non-native publications to be created")

                // 3. Verify Tasks ARE created
                assertContainsTaskWithName("publishKotlinMultiplatformPublicationToMavenLocal")
                assertContainsTaskWithName("publishJvmPublicationToMavenLocal")
            }
        }
    }

    @Test
    fun `test publishing ALLOWED on unsupported host with Native targets WHEN override property is set`() {
        // Case: kmp (native) - affected: publishing forbidden by default with a key to overwrite.
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
                    linuxX64() // Native target
                }
                evaluate()

                // 1. Assert NO Diagnostic (suppressed by override)
                assertNoDiagnostics(KotlinToolingDiagnostics.PublishingDisabledOnUnsupportedHost)

                val publishing = extensions.getByType(PublishingExtension::class.java)
                val mavenPublications = publishing.publications.filterIsInstance<MavenPublication>()

                // 2. Assert Root Publication EXISTS due to override
                val rootPublication = mavenPublications.firstOrNull { it.name == "kotlinMultiplatform" }
                assertNotNull(rootPublication, "Root publication should exist when override is active")

                // 3. Assert JVM published
                assertNotNull(mavenPublications.find { it.name == "jvm" }, "JVM should be published")

                // 4. Assert Native target NOT published (because HostManager still says it's unsupported)
                // The block was bypassed, but the individual target creation logic checks cross-compilation support.
                assertNull(
                    mavenPublications.find { it.name == "linuxX64" },
                    "Native target cannot be published on FreeBSD even with override (unsupported host)"
                )
            }
        }
    }

    @Test
    fun `test pure kotlin-jvm project on unsupported host is NOT affected`() {
        // Case: pure kotlin-jvm (no MPP plugin) - shouldn't be affected
        withModifiedSystemProperties("os.name" to "FreeBSD", "os.arch" to "amd64") {
            with(buildProjectWithJvm()) {
                configureRepositoriesForTests()
                plugins.apply("maven-publish")

                // In a pure JVM project, users often define publications manually or rely on plugins that do so.
                // We create one here to verify that the publishing machinery itself isn't blocked.
                extensions.configure(PublishingExtension::class.java) { publishing ->
                    publishing.publications.create("myLib", MavenPublication::class.java) {
                        it.from(components.getByName("java"))
                    }
                }

                evaluate()

                // 1. Assert NO Diagnostic (The KMP diagnostic logic shouldn't run, so this must be empty)
                assertNoDiagnostics(KotlinToolingDiagnostics.PublishingDisabledOnUnsupportedHost)

                val publishing = extensions.getByType(PublishingExtension::class.java)

                // 2. Assert user-defined publication exists
                assertNotNull(
                    publishing.publications.findByName("myLib"),
                    "Standard JVM publication should be created"
                )

                // 3. Assert MPP publication does NOT exist (confirming isolation)
                assertNull(
                    publishing.publications.findByName("kotlinMultiplatform"),
                    "MPP publication should not exist in pure JVM project"
                )

                // 4. Verify no tasks related to MPP publishing exist
                assertContainsNoTaskWithName("publishKotlinMultiplatformPublicationToMavenLocal")
            }
        }
    }
}
