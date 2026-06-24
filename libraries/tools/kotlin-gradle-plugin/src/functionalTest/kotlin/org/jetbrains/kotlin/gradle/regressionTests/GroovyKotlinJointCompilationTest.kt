/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.tasks.compile.GroovyCompile
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroovyKotlinJointCompilationTest {

    @Test
    fun `KT-17153 - groovy compile classpath contains kotlin compile output when flag is enabled`() {
        val project = buildProjectWithJvm(
            preApplyCode = { enableDefaultStdlibDependency(false) }
        ) {
            plugins.apply("groovy")
        }

        // opt-in to joint compilation — KGP will add compileKotlin output to compileGroovy classpath
        project.kotlinExtension.groovyKotlinJointCompilation = true

        project.evaluate()

        val kotlinTask = project.tasks.withType(KotlinCompile::class.java).named("compileKotlin").get()
        val groovyTask = project.tasks.withType(GroovyCompile::class.java).named("compileGroovy").get()

        // the directory where compileKotlin puts its .class files
        val kotlinOutput = kotlinTask.destinationDirectory.asFile.get()

        // verify that Kotlin output directory is present in compileGroovy classpath
        // so Groovy classes can reference Kotlin classes
        assertTrue(
            groovyTask.classpath.files.contains(kotlinOutput),
            "compileGroovy classpath should contain compileKotlin output directory.\n" +
                    "Expected: $kotlinOutput\nActual classpath: ${groovyTask.classpath.files}"
        )
    }

    @Test
    fun `KT-17153 - groovy compile depends on kotlin compile when flag is enabled`() {
        val project = buildProjectWithJvm(
            preApplyCode = { enableDefaultStdlibDependency(false) }
        ) {
            plugins.apply("groovy")
        }

        // opt-in to joint compilation
        project.kotlinExtension.groovyKotlinJointCompilation = true

        project.evaluate()

        val kotlinTask = project.tasks.named("compileKotlin").get()
        val groovyTask = project.tasks.named("compileGroovy").get()

        // verify that compileGroovy has a task dependency on compileKotlin
        // so Gradle executes compileKotlin before compileGroovy
        assertTrue(
            groovyTask.taskDependencies.getDependencies(groovyTask).contains(kotlinTask),
            "compileGroovy should depend on compileKotlin"
        )
    }

    @Test
    fun `KT-87200 - groovy compile classpath does not contain kotlin compile output by default`() {
        val project = buildProjectWithJvm(
            preApplyCode = { enableDefaultStdlibDependency(false) }
        ) {
            plugins.apply("groovy")
        }

        // groovyKotlinJointCompilation is false by default — KGP must not touch compileGroovy classpath
        project.evaluate()

        val kotlinTask = project.tasks.withType(KotlinCompile::class.java).named("compileKotlin").get()
        val groovyTask = project.tasks.withType(GroovyCompile::class.java).named("compileGroovy").get()

        val kotlinOutput = kotlinTask.destinationDirectory.asFile.get()

        // verify that Kotlin output is NOT in compileGroovy classpath when flag is off
        // this protects projects that configure joint compilation manually (e.g. JetBrains Space buildSrc)
        // from getting a circular task dependency: compileGroovy -> compileKotlin -> compileGroovy
        assertFalse(
            groovyTask.classpath.files.contains(kotlinOutput),
            "compileGroovy classpath should NOT contain compileKotlin output by default " +
                    "(groovyKotlinJointCompilation is false). Use groovyKotlinJointCompilation = true to enable."
        )
    }
}
