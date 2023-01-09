/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import kotlin.test.Test
import kotlin.test.assertSame

class KT54783JvmWithJavaCompilationFactoryTest {
    @Test
    fun `test - create custom jvm compilation`() {
        val project = buildProjectWithJvm()
        val kotlin = project.extensions.getByType<KotlinJvmProjectExtension>()
        var customCompilationInstanceInConfigureBlock: KotlinWithJavaCompilation<*, *>? = null
        kotlin.target.compilations.create("custom") { customCompilationInstance ->
            customCompilationInstanceInConfigureBlock = customCompilationInstance
        }

        assertSame(
            kotlin.target.compilations.getByName("custom"), customCompilationInstanceInConfigureBlock,
            "Expected same compilation instance in configure block as well as present in target.compilations"
        )
    }
}