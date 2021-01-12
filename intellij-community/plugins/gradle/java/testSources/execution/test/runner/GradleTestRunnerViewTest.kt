// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTRunnerNodeDescriptor
import com.intellij.openapi.externalSystem.execution.ExternalSystemExecutionConsoleManager
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.ExtensionTestUtil.maskExtensions
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.util.ui.tree.TreeUtil
import groovy.json.StringEscapeUtils.escapeJava
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Test
import javax.swing.tree.DefaultMutableTreeNode

class GradleTestRunnerViewTest : GradleImportingTestCase() {

  @TargetVersions("5.0+")
  @Test
  fun `test grouping events of the same suite comes from different tasks`() {
    createProjectSubFile("src/test/java/my/pack/AppTest.java",
                         "package my.pack;\n" +
                         "import org.junit.Test;\n" +
                         "import static org.junit.Assert.fail;\n" +
                         "public class AppTest {\n" +
                         "    @Test\n" +
                         "    public void test() {\n" +
                         "        String prop = System.getProperty(\"prop\");\n" +
                         "        if (prop != null) {\n" +
                         "            fail(prop);\n" +
                         "        }\n" +
                         "    }\n" +
                         "}\n")

    val buildScript = GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withJUnit("4.12")
      .withTask(
        "additionalTest",
        types = *arrayOf("Test"),
        content = "testClassesDirs = sourceSets.test.output.classesDirs\n" +
                  "classpath = sourceSets.test.runtimeClasspath\n" +
                  "jvmArgs += \"-Dprop='integ test error'\""
      )

    importProject(buildScript.generate())

    var testsExecutionConsole: GradleTestsExecutionConsole? = null
    maskExtensions(ExternalSystemExecutionConsoleManager.EP_NAME,
                   listOf(object : GradleTestsExecutionConsoleManager() {
                     override fun attachExecutionConsole(project: Project,
                                                         task: ExternalSystemTask,
                                                         env: ExecutionEnvironment?,
                                                         processHandler: ProcessHandler?): GradleTestsExecutionConsole? {
                       testsExecutionConsole = super.attachExecutionConsole(project, task, env, processHandler)
                       return testsExecutionConsole
                     }
                   }),
                   testRootDisposable)

    val settings = ExternalSystemTaskExecutionSettings().apply {
      externalProjectPath = projectPath
      taskNames = listOf("clean", "test", "additionalTest")
      externalSystemIdString = GradleConstants.SYSTEM_ID.id
    }

    ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID,
                               myProject, GradleConstants.SYSTEM_ID, null,
                               ProgressExecutionMode.NO_PROGRESS_SYNC)

    val treeStringPresentation = runInEdtAndGet {
      val tree = testsExecutionConsole!!.resultsViewer.treeView!!
      TestConsoleProperties.HIDE_PASSED_TESTS.set(testsExecutionConsole!!.properties, false)
      TreeUtil.expandAll(tree)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      PlatformTestUtil.waitWhileBusy(tree)
      return@runInEdtAndGet PlatformTestUtil.print(tree, false)
    }

    assertEquals("-[root]\n" +
                 " -my.pack.AppTest\n" +
                 "  test\n" +
                 "  test",
                 treeStringPresentation.trim())
  }

  @TargetVersions("2.14+")
  @Test
  fun `test console empty lines and output without eol at the end`() {
    val testOutputText = "test \noutput\n\ntext"
    createProjectSubFile("src/test/java/my/pack/AppTest.java",
                         """package my.pack;
                            import org.junit.Test;
                            import static org.junit.Assert.fail;
                            public class AppTest {
                                @Test
                                public void test() {
                                    System.out.println("${escapeJava(testOutputText)}");
                                }
                            }""".trimIndent())

    val scriptOutputText = "script \noutput\n\ntext\n"
    val scriptOutputTextWOEol = "text w/o eol"
    importProject(GradleBuildScriptBuilderEx()
                    .withJavaPlugin()
                    .withJUnit("4.12")
                    .addBuildScriptPrefix("print(\"${escapeJava(scriptOutputText)}\")")
                    .addPostfix("print(\"${escapeJava(scriptOutputTextWOEol)}\")")
                    .generate())

    var testsExecutionConsole: GradleTestsExecutionConsole? = null
    maskExtensions(ExternalSystemExecutionConsoleManager.EP_NAME,
                   listOf(object : GradleTestsExecutionConsoleManager() {
                     override fun attachExecutionConsole(project: Project,
                                                         task: ExternalSystemTask,
                                                         env: ExecutionEnvironment?,
                                                         processHandler: ProcessHandler?): GradleTestsExecutionConsole? {
                       testsExecutionConsole = super.attachExecutionConsole(project, task, env, processHandler)
                       return testsExecutionConsole
                     }
                   }),
                   testRootDisposable)

    val settings = ExternalSystemTaskExecutionSettings().apply {
      externalProjectPath = projectPath
      taskNames = listOf("clean", "test")
      scriptParameters = "--quiet"
      externalSystemIdString = GradleConstants.SYSTEM_ID.id
    }

    ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID,
                               myProject, GradleConstants.SYSTEM_ID, null,
                               ProgressExecutionMode.NO_PROGRESS_SYNC)

    val treeStringPresentation = runInEdtAndGet {
      val tree = testsExecutionConsole!!.resultsViewer.treeView!!
      TestConsoleProperties.HIDE_PASSED_TESTS.set(testsExecutionConsole!!.properties, false)
      TreeUtil.expandAll(tree)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      PlatformTestUtil.waitWhileBusy(tree)

      val testsRootNode = TreeUtil.findNode(tree.model.root as DefaultMutableTreeNode) {
        val userObject = it.userObject
        userObject is SMTRunnerNodeDescriptor && userObject.element == testsExecutionConsole!!.resultsViewer.testsRootNode
      }
      TreeUtil.selectNode(tree, testsRootNode)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      PlatformTestUtil.waitWhileBusy(tree)
      return@runInEdtAndGet PlatformTestUtil.print(tree, true)
    }

    assertEquals("-[[root]]\n" +
                 " -my.pack.AppTest\n" +
                 "  test",
                 treeStringPresentation.trim())

    val console = testsExecutionConsole!!.console as ConsoleViewImpl
    val consoleText = runInEdtAndGet {
      console.flushDeferredText()
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      console.text
    }

    val consoleTextWithoutFirstTestingGreetingsLine: String
    if (consoleText.startsWith("Testing started at ")) {
      consoleTextWithoutFirstTestingGreetingsLine = consoleText.substringAfter("\n")
    } else {
      consoleTextWithoutFirstTestingGreetingsLine = consoleText
    }
    assertThat(consoleTextWithoutFirstTestingGreetingsLine).contains(testOutputText)
    val expectedText = if (SystemInfo.isWindows) {
      scriptOutputText + scriptOutputTextWOEol + "\n"
    } else {
      "script \n" +
      "output\n" +
      "text\n" +
      "text w/o eol\n"
    }
    assertEquals(expectedText, consoleTextWithoutFirstTestingGreetingsLine.substringBefore(testOutputText))
  }
}
