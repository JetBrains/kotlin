/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.CFLAGS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.FRAMEWORK_PATHS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.HEADER_PATHS_PROPERTY
import org.jetbrains.kotlin.gradle.targets.native.cocoapods.CocoapodsPluginDiagnostics
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.Test

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
}