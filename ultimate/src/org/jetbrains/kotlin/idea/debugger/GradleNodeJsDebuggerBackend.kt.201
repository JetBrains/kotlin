/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.externalSystem.debugger.DebuggerBackendExtension
import com.intellij.openapi.project.Project
import com.jetbrains.debugger.wip.JSRemoteDebugConfiguration
import com.jetbrains.debugger.wip.JSRemoteDebugConfigurationType
import java.io.File

class GradleNodeJsDebuggerBackend : DebuggerBackendExtension {
    override fun id(): String = "Gradle NodeJS"

    override fun initializationCode(dispatchPort: String, serializedParams: String): List<String> {
        return javaClass
            .getResourceAsStream("/org/jetbrains/kotlin/idea/debugger/GradleNodeJsDebugConfigurator.groovy")
            .bufferedReader()
            .readLines()
            .map { line ->
                line
                    .replace("%id", id())
                    .replace("%dispatchPort", dispatchPort)
            }
    }

    override fun debugConfigurationSettings(
        project: Project,
        processName: String,
        processParameters: String
    ): RunnerAndConfigurationSettings {
        val runManager = RunManager.getInstance(project)

        return runManager.createConfiguration(
            processName,
            JSRemoteDebugConfigurationType::class.java
        ).apply {
            // WA for debugger to process source maps
            with(configuration as JSRemoteDebugConfiguration) {
                val basePath = project.basePath

                if (basePath != null) {
                    val base = File(basePath)
                    val packages = base
                        .resolve("build")
                        .resolve("js")

                    mappings.add(RemoteUrlMappingBean(packages.canonicalPath, packages.canonicalPath))
                }
            }
        }
    }
}