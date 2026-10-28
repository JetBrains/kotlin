/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_COMPILE_MAX_WORKERS
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.maxParallelUsagesOf
import org.jetbrains.kotlin.gradle.util.withGradleProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinNativeCompileTest {

    @Test
    fun `KotlinNativeCompile task configures the parallelism limit service using the KOTLIN_NATIVE_COMPILE_MAX_WORKERS property, when set`() {
        withGradleProperty(KOTLIN_NATIVE_COMPILE_MAX_WORKERS, value = "3") {
            val project = buildProjectWithMPP {
                kotlin {
                    linuxX64()
                }
            }
            project.evaluate()

            // Force the KotlinNativeCompile task to be instantiated, so its init block runs
            project.tasks.getByName("compileKotlinLinuxX64") as KotlinNativeCompile

            assertEquals(3, project.maxParallelUsagesOf("KotlinNativeCompile"))
        }
    }

    @Test
    fun `KotlinNativeCompile task sets no parallelism limit when the KOTLIN_NATIVE_COMPILE_MAX_WORKERS property is not set`() {
        withGradleProperty(KOTLIN_NATIVE_COMPILE_MAX_WORKERS, value = null) {
            val project = buildProjectWithMPP {
                kotlin {
                    linuxX64()
                }
            }
            project.evaluate()

            // Force the KotlinNativeCompile task to be instantiated, so its init block runs
            project.tasks.getByName("compileKotlinLinuxX64") as KotlinNativeCompile

            assertNull(project.maxParallelUsagesOf("KotlinNativeCompile"))
        }
    }
}
