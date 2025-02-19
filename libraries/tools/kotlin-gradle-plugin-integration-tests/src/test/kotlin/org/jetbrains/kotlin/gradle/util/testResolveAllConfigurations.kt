/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.testbase.*
import java.io.File
import kotlin.test.assertTrue

private const val RESOLVE_ALL_CONFIGURATIONS_TASK_NAME = "resolveAllConfigurations"
private const val UNRESOLVED_MARKER = "<<!>>UNRESOLVED:"
private val unresolvedConfigurationRegex = "${Regex.escape(UNRESOLVED_MARKER)}(.*)".toRegex()

fun TestProject.testResolveAllConfigurations(
    subproject: String? = null,
    withUnresolvedConfigurationNames: TestProject.(unresolvedConfigurations: List<String>, buildResult: BuildResult) -> Unit = { conf, buildResult ->
        assertTrue(
            """
            |${buildResult.failedAssertionOutput()}
            |Unresolved configurations: $conf
            """.trimMargin()
        ) { conf.isEmpty() }
    },
) {
    val targetProject = subproject?.let { subProject(it) } ?: this

    targetProject.buildScriptInjection {
        project.registerResolveAllConfigurationsTask()
    }

    build(RESOLVE_ALL_CONFIGURATIONS_TASK_NAME) {
        assertTasksExecuted(":${subproject?.let { "$it:" }.orEmpty()}$RESOLVE_ALL_CONFIGURATIONS_TASK_NAME")
        val unresolvedConfigurations = unresolvedConfigurationRegex.findAll(output).map { it.groupValues[1] }.toList()
        withUnresolvedConfigurationNames(unresolvedConfigurations, this)
    }
}

private fun Project.registerResolveAllConfigurationsTask() {
    tasks.register(RESOLVE_ALL_CONFIGURATIONS_TASK_NAME) {
        it.notCompatibleWithConfigurationCache(
            "The configuration resolution happens during execution, so we have to disable CC for this task"
        )
        if ("commonizeNativeDistribution" in rootProject.tasks.names) {
            it.dependsOn(":commonizeNativeDistribution")
        }

        val excludeConfigs = setOf(
            "default",
            "archives",
        )
        val projectPath = project.path
        val projectRootDir = project.rootDir

        val resolvableConfigurations = project.provider {
            project.configurations
                .filter { configuration -> configuration.isCanBeResolved }
                .filterNot { configuration -> configuration.name in excludeConfigs }
        }

        it.doFirst { task ->
            val knownExtensions = setOf("jar", "klib")
            resolvableConfigurations.get().forEach { configuration ->
                val configurationPath = if (projectPath == ":") ":" + configuration.name else projectPath + ":" + configuration.name
                try {
                    println("Resolving $configurationPath")
                    configuration.files.forEach { file ->
                        val path = if (file.extension in knownExtensions) {
                            file.name
                        } else {
                            file.relativeTo(projectRootDir).invariantSeparatorsPath
                        }
                        println(">> $configurationPath --> $path")
                    }
                    println("OK, resolved $configurationPath\n")
                } catch (e: Throwable) {
                    var ex = e as Throwable?
                    while (ex != null) {
                        println(ex.message)
                        ex = ex.cause
                    }
                    println("$UNRESOLVED_MARKER $configurationPath\n")
                }
            }
        }
    }
}
