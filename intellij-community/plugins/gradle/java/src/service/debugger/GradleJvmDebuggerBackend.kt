// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.debugger

import com.intellij.execution.RunManager.Companion.getInstance
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.remote.RemoteConfiguration
import com.intellij.execution.remote.RemoteConfigurationType
import com.intellij.openapi.externalSystem.debugger.DebuggerBackendExtension
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper
import com.intellij.openapi.project.Project

class GradleJvmDebuggerBackend : DebuggerBackendExtension {
  override fun id() = "Gradle JVM"

  override fun debugConfigurationSettings(project: Project,
                                          processName: String,
                                          processParameters: String): RunnerAndConfigurationSettings {
    val runSettings = getInstance(project).createConfiguration(processName, RemoteConfigurationType::class.java)
    val description = splitParameters(processParameters)

    val configuration = runSettings.configuration as RemoteConfiguration
    configuration.HOST = "localhost"
    configuration.PORT = description[ForkedDebuggerHelper.DEBUG_SERVER_PORT_KEY]
    configuration.USE_SOCKET_TRANSPORT = true
    configuration.SERVER_MODE = true

    return runSettings
  }

  override fun initializationCode(dispatchPort: String, parameters: String) = listOf(
    "import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper",
    "gradle.taskGraph.beforeTask { Task task ->",
    "  if (task instanceof org.gradle.api.tasks.testing.Test) {",
    "    task.maxParallelForks = 1",
    "    task.forkEvery = 0",
    "  }",
    "  if (task instanceof JavaForkOptions) {",
    "    def debugPort = ForkedDebuggerHelper.setupDebugger('${id()}', task.path, '$parameters', $dispatchPort)",
    "    def jvmArgs = task.jvmArgs.findAll{!it?.startsWith('-agentlib:jdwp') && !it?.startsWith('-Xrunjdwp')}",
    "    jvmArgs << ForkedDebuggerHelper.JVM_DEBUG_SETUP_PREFIX + debugPort",
    "    task.jvmArgs = jvmArgs",
    "  }",
    "}",
    "gradle.taskGraph.afterTask { Task task ->",
    "  if (task instanceof JavaForkOptions) {",
    "    ForkedDebuggerHelper.signalizeFinish('${id()}', task.path, $dispatchPort)",
    "  }",
    "}")
}