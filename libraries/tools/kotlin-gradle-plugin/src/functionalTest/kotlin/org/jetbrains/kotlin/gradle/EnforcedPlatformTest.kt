/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import kotlin.test.Test
import kotlin.test.assertTrue

class EnforcedPlatformTest : MultiplatformExtensionTest() {
    @Test
    fun `using enforcedPlatform adds BOM dependency correctly`() {
        val project = buildProjectWithMPP {
            kotlin {
                js("browser") {
                    browser {
                        binaries.executable()
                    }
                }
                sourceSets.getByName("browserMain").apply {
                    dependencies {
                        implementation(enforcedPlatform("test:enforced-platform-dependency"))
                    }
                }
            }
        }

        with(project.evaluate()) {
            val hasEnforcedPlatformDependency = configurations.getByName("browserMainImplementation").allDependencies.any {
                it.group == "test" && it.name == "enforced-platform-dependency" && it.version == null
            }
            assertTrue(hasEnforcedPlatformDependency, "Could not find enforced platform dependency")
        }
    }
}