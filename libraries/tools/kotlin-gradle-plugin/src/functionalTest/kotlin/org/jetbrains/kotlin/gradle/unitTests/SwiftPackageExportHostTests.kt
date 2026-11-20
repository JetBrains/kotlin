/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.exportDslProject
import org.jetbrains.kotlin.gradle.util.exportExtension
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test

// KT-91057
@Isolated("Modifies system properties")
class SwiftPackageExportHostTests {

    @Test
    fun `test the swift package integration on a linux host is reported`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            swiftPackageProject().assertContainsDiagnostic(
                KotlinToolingDiagnostics.SwiftPackageExportUnsupportedHost("linux-x86_64")
            )
        }
    }

    @Test
    fun `test the swift package integration on a windows host is reported`() {
        withModifiedSystemProperties("os.name" to "Windows 11", "os.arch" to "amd64") {
            swiftPackageProject().assertContainsDiagnostic(
                KotlinToolingDiagnostics.SwiftPackageExportUnsupportedHost("windows-x86_64")
            )
        }
    }

    @Test
    fun `test the swift package integration on a macos host is not reported`() {
        withModifiedSystemProperties("os.name" to "Mac OS X", "os.arch" to "aarch64") {
            swiftPackageProject().assertNoDiagnostics(KotlinToolingDiagnostics.SwiftPackageExportUnsupportedHost)
        }
    }

    @Test
    fun `test the unsupported host warning is suppressed by the disabled targets property`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            swiftPackageProject(ignoreDisabledTargets = true)
                .assertNoDiagnostics(KotlinToolingDiagnostics.SwiftPackageExportUnsupportedHost)
        }
    }

    @Test
    fun `test the xcode integration alone on a linux host is not reported`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "amd64") {
            exportDslProject {
                exportExtension.swift {
                    moduleName.set("Shared")
                    xcodeIntegration()
                }
            }.assertNoDiagnostics(KotlinToolingDiagnostics.SwiftPackageExportUnsupportedHost)
        }
    }

    // Not exportDslProject: the property has to be set before the plugin reads it.
    private fun swiftPackageProject(ignoreDisabledTargets: Boolean = false) = buildProjectWithMPP(
        preApplyCode = {
            if (ignoreDisabledTargets) extensions.extraProperties.set(KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS, "true")
            configureRepositoriesForTests()
        },
        code = {
            kotlin { iosSimulatorArm64() }
            exportExtension.swift {
                moduleName.set("Shared")
                swiftPackageIntegration { }
            }
        },
    ).also { it.evaluate() }
}
