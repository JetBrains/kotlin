/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.Project
import kotlin.io.path.appendText
import kotlin.io.path.exists
import kotlin.io.path.readText

interface BuildGradleKtsInjection {
    fun inject(project: Project)
}

private const val marker = "// MARKER: BuildGradleKtsInjections Enabled"

fun TestProject.enableBuildGradleKtsInjectionsIfNecessary() {
    val injectionClasses = System.getProperty("buildGradleKtsInjectionsClasspath")
        ?: error("Missing required system property 'buildGradleKtsInjectionsClasspath'")

    if (buildGradle.exists()) {
        error("build.gradle file is detected in Test Project '$projectName'. buildGradleKtsInjection works only with build.gradle.kts")
    }
    if (!buildGradleKts.exists()) {
        error("build.gradle.kts file not found in Test Project '$projectName'. Please check if it is a valid gradle project")
    }
    if (buildGradleKts.readText().contains(marker)) return

    buildGradleKts.modify {
        it.insertBlockToBuildScriptAfterPluginsAndImports("""
            $marker
            buildscript {
                println("⚠️ BuildGradleKtsInjections Enabled. Classes from kotlin-gradle-plugin-integration-tests injected to buildscript")               
                val files = project.files("$injectionClasses")
                dependencies {
                    classpath(files)
                }
            }            
        """.trimIndent())
    }
}

fun invokeBuildGradleKtsInjection(project: Project, fqn: String) {
    val cl = object {}.javaClass.classLoader
    val injectionClass = cl.loadClass(fqn)
    val injection = injectionClass.getDeclaredConstructor().newInstance() as BuildGradleKtsInjection
    injection.inject(project)
}

@DslMarker
annotation class BuildGradleKtsInjectionScope

@BuildGradleKtsInjectionScope
class BuildGradleKtsInjectionContext(
    val project: Project
) {
    val kotlinMultiplatform = project.extensions.getByName("kotlin") as org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
}

inline fun TestProject.buildGradleKtsInjection(crossinline code: BuildGradleKtsInjectionContext.() -> Unit) {
    enableBuildGradleKtsInjectionsIfNecessary()

    // it is important to create an anonymous object here, so that we can invoke this via reflection in buildscripts
    // because regular lambdas get executed through ivokedynamic logic. i.e. classes created on fly.
    val injection = object : BuildGradleKtsInjection {
        override fun inject(project: Project) {
            val scope = BuildGradleKtsInjectionContext(project)
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
            |Check the way how buildGradleKtsInjection {} is invoked. 
            |It should not capture any variables.
            |Constructors:
            |$allConstructors
            |""".trimMargin()
        )
    }

    if (primaryConstructor.parameterCount != 0) {
        error(
            """$clazz can't have any parameters. 
            |Please check that buildGradleKtsInjection {} doesn't capture any parameters outside lambda.
            |Types of leaked parameters:
            |${primaryConstructor.parameters.joinToString(separator = "\n") { "  * $it" }}
            |
            |If you need to use these parameters make sure you pass them via [BuildGradleKtsInjectionContext] object.
            |""".trimMargin()
        )
    }
    val escapedFqn = injection.javaClass.name.replace("$", "\\\$")
    buildGradleKts.appendText("\norg.jetbrains.kotlin.gradle.testbase.invokeBuildGradleKtsInjection(project, \"$escapedFqn\")\n")
}