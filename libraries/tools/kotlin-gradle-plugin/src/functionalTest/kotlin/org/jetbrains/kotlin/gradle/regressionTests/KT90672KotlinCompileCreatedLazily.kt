/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.internal.TaskInternal
import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProject
import kotlin.test.Test
import kotlin.test.assertTrue

class KT90672KotlinCompileCreatedLazily {

    @Test
    fun `test - KotlinCompile task can be realized from lazy task configuration - KT-90672`() {
        val project = buildProject {}
        val kotlinJvmFactory = project.plugins.apply(KotlinApiPlugin::class.java)

        val compilerOptions = kotlinJvmFactory.createCompilerJvmOptions()
        val kotlinCompileTask = project.tasks.register("compileKotlin", KotlinCompile::class.java, compilerOptions)
        project.tasks.register("realizeCompileKotlin") {
            project.tasks.named("compileKotlin").get()
        }

        val compileTask = project.tasks.getByName("compileKotlin") as TaskInternal
        assertTrue(
            compileTask.requiredServices.isServiceRequired(VariantImplementationFactories.getProvider(project)),
            "Task '${compileTask.path}' should declare usage of the variant implementation factories service"
        )
    }
}
