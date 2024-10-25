/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.Project
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.readText

interface GradleBuildScriptInjection {
    fun inject(project: Project)
}

private const val marker = "// MARKER: GradleBuildScriptInjections Enabled"

fun GradleProject.enableBuildScriptInjectionsIfNecessary() {
    val injectionClasses = System.getProperty("buildGradleKtsInjectionsClasspath")
        ?: error("Missing required system property 'buildGradleKtsInjectionsClasspath'")
    val escapedInjectionClasses = injectionClasses
        .replace("\\", "\\\\")
        .replace("$", "\\$")

    if (buildGradle.exists()) {
        if (buildGradle.readText().contains(marker)) return
        buildGradle.modify {
            it.insertBlockToBuildScriptAfterImports("""
            $marker
            buildscript {
                println("⚠️ GradleBuildScriptInjections Enabled. Classes from kotlin-gradle-plugin-integration-tests injected to buildscript")               
                dependencies {
                    classpath(project.files('$escapedInjectionClasses'))
                }
            }
            
        """.trimIndent())
        }
        return
    }

    if (buildGradleKts.exists()) {
        if (buildGradleKts.readText().contains(marker)) return

        buildGradleKts.modify {
            it.insertBlockToBuildScriptAfterPluginsAndImports("""
            $marker
            buildscript {
                println("⚠️ GradleBuildScriptInjections Enabled. Classes from kotlin-gradle-plugin-integration-tests injected to buildscript")               
                val files = project.files("$escapedInjectionClasses")
                dependencies {
                    classpath(files)
                }
            }

            """.trimIndent())
        }
        return
    }

    error("build.gradle.kts nor build.gradle files not found in Test Project '$projectName'. Please check if it is a valid gradle project")
}

fun invokeBuildScriptInjection(project: Project, fqn: String) {
    val cl = object {}.javaClass.classLoader
    val injectionClass = cl.loadClass(fqn)
    val injection = injectionClass.getDeclaredConstructor().newInstance() as GradleBuildScriptInjection
    injection.inject(project)
}

@DslMarker
annotation class BuildGradleKtsInjectionScope

@BuildGradleKtsInjectionScope
class GradleBuildScriptInjectionContext(
    val project: Project
) {
    val kotlinMultiplatform get() = project.extensions.getByName("kotlin") as org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
    val dependencies get() = project.dependencies
}

inline fun GradleProject.buildScriptInjection(crossinline code: GradleBuildScriptInjectionContext.() -> Unit) {
    enableBuildScriptInjectionsIfNecessary()
    // it is important to create an anonymous object here, so that we can invoke this via reflection in buildscripts
    // because regular lambdas get executed through ivokedynamic logic. i.e. classes created on fly.
    val injection = object : GradleBuildScriptInjection {
        override fun inject(project: Project) {
            val scope = GradleBuildScriptInjectionContext(project)
            scope.code()
        }
    }
    // check that injection has expected lambda "shape". i.e. no captured variables
    val clazz = injection.javaClass
    val primaryConstructor = clazz.declaredConstructors.singleOrNull()
    if (primaryConstructor == null) {
        val allConstructors = clazz.declaredConstructors.joinToString(separator = "\n") { it.toString() }
        error(
            """$clazz has multiple constructors. 
            |Check the way how buildScriptInjection {} is invoked. 
            |It should not capture any variables.
            |Constructors:
            |$allConstructors
            |""".trimMargin()
        )
    }

    if (primaryConstructor.parameterCount != 0) {
        error(
            """$clazz can't have any parameters. 
            |Please check that buildScriptInjection {} doesn't capture any parameters outside lambda.
            |Types of leaked parameters:
            |${primaryConstructor.parameters.joinToString(separator = "\n") { "  * $it" }}
            |
            |If you need to use these parameters make sure you pass them via [GradleBuildScriptInjectionContext] object.
            |""".trimMargin()
        )
    }

    val fqn = injection.javaClass.name
    if (buildGradleKts.exists()) {
        val escapedFqn = fqn.replace("$", "\\\$")
        buildGradleKts.appendText("\norg.jetbrains.kotlin.gradle.testbase.invokeBuildScriptInjection(project, \"$escapedFqn\")\n")
    }

    if (buildGradle.exists()) {
        buildGradle.appendText("\norg.jetbrains.kotlin.gradle.testbase.TestGradleBuildInjectionKt.invokeBuildScriptInjection(project, '$fqn')\n")
    }
}