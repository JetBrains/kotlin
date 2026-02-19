/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test

@OptIn(ExperimentalWasmDsl::class)
class ConfigurationAvoidanceTest {

    @Test
    fun testConfigurationsEagerConfigurationInJvmPlugin() {
        val project = buildProjectWithJvm {
            configurations.consumable("lazyConfiguration") {
                throw RuntimeException("Configuration realized")
            }
        }

        project.evaluate()
    }

    @Test
    fun testConfigurationsEagerConfigurationInKmpPlugin() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                linuxX64()
                wasmWasi()
                js()
            }
            configurations.consumable("lazyConfiguration") {
                throw RuntimeException("Configuration realized")
            }
        }

        project.evaluate()
    }

    @Test
    fun testConfigurationsEagerConfigurationInAndroidPlugin() {
        val project = buildProject {
            applyKotlinAndroidPlugin()
            project.plugins.apply("android-library")

            configurations.consumable("lazyConfiguration") {
                throw RuntimeException("Configuration realized")
            }

            androidLibrary {
                compileSdk = 33
            }
        }

        project.evaluate()
    }
}
