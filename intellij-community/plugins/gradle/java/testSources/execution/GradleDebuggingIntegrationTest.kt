// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemProcessHandler
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemTaskDebugRunner
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.gradle.importing.withMavenCentral
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class GradleDebuggingIntegrationTest : GradleImportingTestCase() {

  @Test
  fun `daemon is started with debug flags only if script debugging is enabled`() {
    importProject(
      GradleBuildScriptBuilderEx()
        .withMavenCentral()
        .applyPlugin("'java'")
        .addPostfix("""
          import java.lang.management.ManagementFactory;
          import java.lang.management.RuntimeMXBean;
          
          task myTask {
            doFirst {
              RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
              List<String> arguments = runtimeMxBean.getInputArguments();
              File file = new File("args.txt")
              file.write(arguments.toString())
            }
          }
        """.trimIndent())
        .generate()
    )

    val gradleRC = createEmptyGradleRunConfiguration("myRC")
    gradleRC.settings.apply {
      externalProjectPath = projectPath
      taskNames = listOf("myTask")
    }
    gradleRC.isScriptDebugEnabled = true

    executeRunConfiguration(gradleRC)

    val reportFile = File(projectPath, "args.txt")
    assertTrue(reportFile.exists())
    assertTrue(reportFile.readText().contains("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address="))

    reportFile.delete()

    gradleRC.isScriptDebugEnabled = false
    executeRunConfiguration(gradleRC)

    assertTrue(reportFile.exists())
    assertFalse(reportFile.readText().contains("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address="))

  }

  private fun executeRunConfiguration(gradleRC: GradleRunConfiguration) {
    val executor = DefaultDebugExecutor.getDebugExecutorInstance()
    val runner = ExternalSystemTaskDebugRunner()
    val latch = CountDownLatch(1)
    val esHandler: AtomicReference<ExternalSystemProcessHandler> = AtomicReference()
    val env = ExecutionEnvironmentBuilder.create(executor, gradleRC)
      .build(ProgramRunner.Callback {
        esHandler.set(it.processHandler as ExternalSystemProcessHandler)
        latch.countDown()
      })

    runInEdt {
      runner.execute(env)
    }

    latch.await()
    esHandler.get().waitFor()
  }
}