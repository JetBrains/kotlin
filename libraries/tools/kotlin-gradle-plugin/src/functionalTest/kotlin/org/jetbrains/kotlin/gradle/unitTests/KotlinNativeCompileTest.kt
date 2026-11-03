/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_COMPILE_MAX_WORKERS
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.isParallelismLimitServiceRegistered
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.maxParallelUsagesOf
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KotlinNativeCompileTest {

    @Test
    fun `KotlinNativeCompile task configures the parallelism limit service using the KOTLIN_NATIVE_COMPILE_MAX_WORKERS property, when set`() {
        val project = buildProjectWithMPP(
            preApplyCode = { propertiesExtension.set(KOTLIN_NATIVE_COMPILE_MAX_WORKERS, "3") }
        ) {
            kotlin {
                linuxX64()
            }
        }
        project.evaluate()

        // Force the KotlinNativeCompile task to be instantiated, so its configureEach actions run
        project.tasks.getByName("compileKotlinLinuxX64") as KotlinNativeCompile

        assertEquals(3, project.maxParallelUsagesOf<KotlinNativeCompile>())
    }

    @Test
    fun `KotlinNativeCompile task does not register the parallelism limit service when the KOTLIN_NATIVE_COMPILE_MAX_WORKERS property is not set`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64()
            }
        }
        project.evaluate()

        // Force the KotlinNativeCompile task to be instantiated, so its configureEach actions run
        project.tasks.getByName("compileKotlinLinuxX64") as KotlinNativeCompile

        assertFalse(project.isParallelismLimitServiceRegistered<KotlinNativeCompile>())
    }
}
