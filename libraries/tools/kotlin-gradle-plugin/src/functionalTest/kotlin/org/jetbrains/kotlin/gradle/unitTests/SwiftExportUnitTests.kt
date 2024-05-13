/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.NamedDomainObjectCollection
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XcodeEnvironment.Companion.XCODE_ENVIRONMENT_OVERRIDE_KEY
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SwiftExportUnitTests {
    @Test
    fun `swift export compilation test`() {
        Assume.assumeTrue("macOS host required for this test", HostManager.hostIsMac)
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.extensions.extraProperties.set(
                    "$XCODE_ENVIRONMENT_OVERRIDE_KEY.CONFIGURATION",
                    "Debug"
                )
                project.extensions.extraProperties.set(
                    "$XCODE_ENVIRONMENT_OVERRIDE_KEY.SDK_NAME",
                    "iphonesimulator"
                )
                project.extensions.extraProperties.set(
                    "$XCODE_ENVIRONMENT_OVERRIDE_KEY.ARCHS",
                    "arm64"
                )
                project.extensions.extraProperties.set(
                    "$XCODE_ENVIRONMENT_OVERRIDE_KEY.BUILT_PRODUCTS_DIR",
                    layout.buildDirectory.dir("products").getFile().canonicalPath
                )
                project.extensions.extraProperties.set(
                    "$XCODE_ENVIRONMENT_OVERRIDE_KEY.TARGET_BUILD_DIR",
                    layout.buildDirectory.dir("buildDir").getFile().canonicalPath
                )
                project.extensions.extraProperties.set(
                    "$XCODE_ENVIRONMENT_OVERRIDE_KEY.FRAMEWORKS_FOLDER_PATH",
                    "Frameworks"
                )
                project.extensions.extraProperties.set(
                    "$XCODE_ENVIRONMENT_OVERRIDE_KEY.EXPANDED_CODE_SIGN_IDENTITY",
                    "-"
                )
                project.extensions.extraProperties.set(
                    "$XCODE_ENVIRONMENT_OVERRIDE_KEY.ENABLE_USER_SCRIPT_SANDBOXING",
                    "NO"
                )

                enableSwiftExport()
            },
            code = {
                repositories.mavenLocal()

                kotlin {
                    iosSimulatorArm64 {
                        binaries.framework()
                    }
                }
            }
        )

        project.evaluate()

        val compilations = project.multiplatformExtension.iosSimulatorArm64().compilations
        val mainCompilation = compilations.main
        val swiftExportCompilation = compilations.swiftExport

        val mainCompileTask = mainCompilation.compileTaskProvider.get() as KotlinNativeCompile
        val swiftExportCompileTask = swiftExportCompilation.compileTaskProvider.get() as KotlinNativeCompile

        // Main compilation exist
        assertNotNull(mainCompilation)

        // swiftExportMain compilation exist
        assertNotNull(swiftExportCompilation)

        val swiftExportMainAssociation = swiftExportCompilation.associatedCompilations.single()

        // swiftExportMain associated with main
        assertEquals(swiftExportMainAssociation, mainCompilation)

        val mainKlib = mainCompileTask.outputFile.get()
        val swiftExportFriendPaths = swiftExportCompileTask.compilation.friendPaths.files

        // Swift Export compilation have dependency on main compilation
        assert(swiftExportFriendPaths.contains(mainKlib)) { "Swift Export compile task doesn't contain dependency on main klib" }

        val swiftExportLibraries = swiftExportCompileTask.libraries.files

        // Swift Export libraries contains main klib
        assert(swiftExportLibraries.contains(mainKlib))
    }
}

private val <T : KotlinCompilation<*>> NamedDomainObjectCollection<out T>.swiftExport: T get() = getByName("swiftExportMain")