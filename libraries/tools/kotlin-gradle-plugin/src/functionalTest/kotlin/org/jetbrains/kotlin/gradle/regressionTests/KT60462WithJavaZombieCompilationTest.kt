/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class KT60462WithJavaZombieCompilationTest {

    /**
     * Regression was introduced by:
     *
     * ```
     * [Gradle] Ensure java source sets being created eagerly for 'withJava' Sebastian Sellmair* 07.07.23, 20:49
     * 817e3de8f546e34974b89fef0f4f93b425e7e607
     * ```
     *
     * The commit was ensuring that jvm compilations will create their associated java source sets
     * as eager as possible. The solution chosen in the commit was that already the construction of the compilation
     * will spawn a coroutine that waits for the `withJavaEnabled` callback to create the java source set.
     *
     * However, a buildscript like
     *
     * ```kotlin
     * kotlin {
     *     jvm().withJava()
     *     val customCompilation = jvm().compilations.create("custom")
     *          //    ^
     *          //    Zombie
     * }
     * ```
     *
     * would therefore try to create the java source set right in the constructor call of the 'custom' compilation.
     * This would have triggered a listener on `javaSourceSets.all {}` which would ensure that all
     * java source sets have a corresponding kotlin compilation created.
     *
     * Since the current stack is currently inside the constructor of the first compilation, the
     * used `compilations.maybeCreate` would trigger the creation of another custom compilation.
     *
     * The initial buildscript call creating the initial custom compilation will therefore return a Zombie instance
     * ```kotlin
     * kotlin {
     *     val customCompilation = jvm().compilations.create("custom")
     *     customCompilation != jvm().compilations.getByName("custom")
     *     //    ^                                   ^
     *     //    Zombie               Real instance created by the javaSourceSets.all listener
     * }
     * ```
     */
    @Test
    fun `test - custom compilation`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.jvm().apply {
            withJava()

            var instanceUsedForConfigureBlock: KotlinJvmCompilation? = null

            val instanceReturnedFromCreate = compilations.create("custom") { instance ->
                assertNull(instanceUsedForConfigureBlock)
                instanceUsedForConfigureBlock = instance
            }

            val instanceReturnedFromGet = compilations.getByName("custom")

            assertSame(instanceReturnedFromCreate, instanceUsedForConfigureBlock)
            assertSame(instanceReturnedFromGet, instanceUsedForConfigureBlock)
        }
    }
}
