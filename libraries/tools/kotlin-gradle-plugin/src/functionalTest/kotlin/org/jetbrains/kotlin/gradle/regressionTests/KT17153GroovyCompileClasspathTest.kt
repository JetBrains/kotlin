/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.tasks.compile.GroovyCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import kotlin.test.Test
import kotlin.test.assertTrue

class KT17153GroovyCompileClasspathTest {

    @Test
    fun `test - groovy compile classpath contains kotlin compile output`() {
        val project = buildProjectWithJvm(
            preApplyCode = { enableDefaultStdlibDependency(false) }
        ) {
            plugins.apply("groovy")
        }

        project.evaluate()

        val kotlinTask = project.tasks.withType(KotlinCompile::class.java).named("compileKotlin").get()
        val groovyTask = project.tasks.withType(GroovyCompile::class.java).named("compileGroovy").get()

        val kotlinOutput = kotlinTask.destinationDirectory.asFile.get()
        assertTrue(
            groovyTask.classpath.files.contains(kotlinOutput),
            "compileGroovy classpath should contain compileKotlin output directory.\n" +
                    "Expected: $kotlinOutput\nActual classpath: ${groovyTask.classpath.files}"
        )
    }

    @Test
    fun `test - groovy compile depends on kotlin compile`() {
        val project = buildProjectWithJvm(
            preApplyCode = { enableDefaultStdlibDependency(false) }
        ) {
            plugins.apply("groovy")
        }

        project.evaluate()

        val kotlinTask = project.tasks.named("compileKotlin").get()
        val groovyTask = project.tasks.named("compileGroovy").get()

        assertTrue(
            groovyTask.taskDependencies.getDependencies(groovyTask).contains(kotlinTask),
            "compileGroovy should depend on compileKotlin"
        )
    }
}
