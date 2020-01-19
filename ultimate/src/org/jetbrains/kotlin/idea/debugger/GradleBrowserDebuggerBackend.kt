/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.externalSystem.debugger.DebuggerBackendExtension
import com.intellij.openapi.project.Project
import com.jetbrains.debugger.wip.JSRemoteDebugConfiguration
import com.jetbrains.debugger.wip.JSRemoteDebugConfigurationType

private const val DEFAULT_DEBUGGING_PORT = 9222

class GradleBrowserDebuggerBackend : DebuggerBackendExtension {
    override fun id(): String = "Gradle Browser"

    override fun initializationCode(dispatchPort: String, serializedParams: String): List<String> {
        return javaClass
            .getResourceAsStream("/org/jetbrains/kotlin/idea/debugger/GradleBrowserDebugConfigurator.groovy")
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

        val settings = runManager.createConfiguration(
            processName,
            JSRemoteDebugConfigurationType::class.java
        )

        with(settings.configuration as JSRemoteDebugConfiguration) {
            port = DEFAULT_DEBUGGING_PORT
        }

        return settings
    }
}