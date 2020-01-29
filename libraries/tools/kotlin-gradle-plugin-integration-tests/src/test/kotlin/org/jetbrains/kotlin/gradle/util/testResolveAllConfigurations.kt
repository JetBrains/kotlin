/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.BaseGradleIT
import kotlin.test.assertTrue

private const val RESOLVE_ALL_CONFIGURATIONS_TASK_NAME = "resolveAllConfigurations"
private const val UNRESOLVED_MARKER = "<<!>>UNRESOLVED:"
private val unresolvedConfigurationRegex = "${Regex.escape(UNRESOLVED_MARKER)}(.*)".toRegex()

fun BaseGradleIT.Project.testResolveAllConfigurations(
    subproject: String? = null,
    skipSetup: Boolean = false,
    excludePredicate: String = "false",
    withUnresolvedConfigurationNames: BaseGradleIT.CompiledProject.(List<String>) -> Unit = { assertTrue("Unresolved configurations: $it") { it.isEmpty() } }
) = with(testCase) {

    if (!skipSetup) {
        setupWorkingDir()
        gradleBuildScript(subproject).run {
            val taskCode = when (extension) {
                "gradle" -> generateResolveAllConfigurationsTask(excludePredicate)
                "kts" -> generateResolveAllConfigurationsTaskKts(excludePredicate)
                else -> error("Unexpected build script extension $extension")
            }
            appendText("\n" + taskCode)
        }
    }

    build(RESOLVE_ALL_CONFIGURATIONS_TASK_NAME) {
        assertSuccessful()
        assertTasksExecuted(":${subproject?.let { "$it:" }.orEmpty()}$RESOLVE_ALL_CONFIGURATIONS_TASK_NAME")
        val unresolvedConfigurations = unresolvedConfigurationRegex.findAll(output).map { it.groupValues[1] }.toList()
        withUnresolvedConfigurationNames(unresolvedConfigurations)
    }
}

private fun generateResolveAllConfigurationsTask(exclude: String) =
    """
        task $RESOLVE_ALL_CONFIGURATIONS_TASK_NAME {
            doFirst {
                project.configurations
                    .matching { it.canBeResolved }
                    .matching { !{ $exclude }.call(it) }
                    .each { configuration ->
                        try {
                            println "Resolving " + configuration.path
                            configuration.files.each { println '>> ' + configuration.path + ' --> ' + it.name }
                            println "OK, resolved " + configuration.path + "\n"
                        } catch (e) {
                            def ex = e
                            while (ex != null) {
                                println ex.message
                                ex = ex.cause
                            }
                            println '$UNRESOLVED_MARKER' + configuration.name + "\n"
                        }
                    }
            }
        }
    """.trimIndent()

private fun generateResolveAllConfigurationsTaskKts(exclude: String) =
    """
        tasks.create("$RESOLVE_ALL_CONFIGURATIONS_TASK_NAME") {
            doFirst {
                project.configurations
                    .filter { it.isCanBeResolved }
                    .filterNot { $exclude }
                    .forEach { configuration ->
                        val path = (configuration as org.gradle.api.internal.artifacts.configurations.ConfigurationInternal).path
                        try {
                            println("Resolving ${'$'}path")
                            configuration.files.forEach { println(">> ${'$'}path --> ${'$'}{it.name}") }
                            println("OK, resolved ${'$'}path\n")
                        } catch (e: Throwable) {
                            var ex = e as Throwable?
                            while (ex != null) {
                                println(ex.message)
                                ex = ex.cause
                            }
                            println("$UNRESOLVED_MARKER ${'$'}{configuration.name}\n")
                        }
                    }
            }
        }
    """.trimIndent()