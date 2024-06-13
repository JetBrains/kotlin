/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.CFLAGS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.FRAMEWORK_PATHS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.HEADER_PATHS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkCopy
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.CocoapodsPluginDiagnostics
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Test
import kotlin.test.assertEquals

class CocoapodsUnitTests {
    @Test
    fun `warning is reported on non-mac machines`() {
        Assume.assumeTrue(!HostManager.hostIsMac)

        buildProjectWithCocoapods {
            assertContainsDiagnostic(CocoapodsPluginDiagnostics.UnsupportedOs)
        }
    }

    @Test
    fun `warning is reported when using deprecated properties`() {
        buildProjectWithMPP {
            propertiesExtension.set(CFLAGS_PROPERTY, "cflags value")

            applyCocoapodsPlugin()
            assertContainsDiagnostic(CocoapodsPluginDiagnostics.DeprecatedPropertiesUsed)
        }

        buildProjectWithMPP {
            propertiesExtension.set(HEADER_PATHS_PROPERTY, "header paths value")

            applyCocoapodsPlugin()
            assertContainsDiagnostic(CocoapodsPluginDiagnostics.DeprecatedPropertiesUsed)
        }

        buildProjectWithMPP {
            propertiesExtension.set(FRAMEWORK_PATHS_PROPERTY, "framework paths value")

            applyCocoapodsPlugin()
            assertContainsDiagnostic(CocoapodsPluginDiagnostics.DeprecatedPropertiesUsed)
        }

        buildProjectWithMPP {
            propertiesExtension.set(FRAMEWORK_PATHS_PROPERTY, "framework paths value")
            propertiesExtension.set(HEADER_PATHS_PROPERTY, "header paths value")

            applyCocoapodsPlugin()
            assertContainsDiagnostic(CocoapodsPluginDiagnostics.DeprecatedPropertiesUsed(listOf(FRAMEWORK_PATHS_PROPERTY, HEADER_PATHS_PROPERTY)))
        }
    }

    @Test
    fun `sync framework task dependencies - have aligned inputs and outputs`() {
        val project = buildProjectWithMPP {
            propertiesExtension.set(KotlinCocoapodsPlugin.CONFIGURATION_PROPERTY, "Debug")
            propertiesExtension.set(KotlinCocoapodsPlugin.PLATFORM_PROPERTY, "iphonesimulator")
            propertiesExtension.set(KotlinCocoapodsPlugin.ARCHS_PROPERTY, "arm64 x86_64")

            applyCocoapodsPlugin()
            kotlin {
                iosSimulatorArm64()
                iosX64()

                cocoapods {
                    framework {
                        baseName = "foo"
                    }
                }
            }
        }.evaluate()

        val syncFrameworkTask = assertIsInstance<FrameworkCopy>(project.tasks.getByName("syncFramework"))
        val universalFrameworkTask = assertIsInstance<FatFrameworkTask>(project.tasks.getByName("fatFramework"))

        assertEquals(
            project.layout.buildDirectory.file("cocoapods/fat-frameworks/debug/foo.framework").get().asFile,
            syncFrameworkTask.sourceFramework.get().asFile,
        )
        assertEquals(
            project.layout.buildDirectory.file("cocoapods/fat-frameworks/debug/foo.framework").get().asFile,
            universalFrameworkTask.fatFramework,
        )
        assertEquals(
            listOf(
                project.layout.buildDirectory.file("bin/iosSimulatorArm64/podDebugFramework/foo.framework").get().asFile,
                project.layout.buildDirectory.file("bin/iosX64/podDebugFramework/foo.framework").get().asFile,
            ),
            universalFrameworkTask.frameworks.map { it.file },
        )
    }

}