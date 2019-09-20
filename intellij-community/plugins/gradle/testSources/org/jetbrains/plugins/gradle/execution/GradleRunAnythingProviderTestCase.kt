// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import com.intellij.ide.actions.runAnything.RunAnythingContext
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.text.StringUtil
import groovyjarjarcommonscli.Option
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.gradle.service.execution.cmd.GradleCommandLineOptionsProvider
import org.junit.runners.Parameterized

abstract class GradleRunAnythingProviderTestCase : GradleImportingTestCase() {

  private lateinit var myDataContext: DataContext
  private lateinit var provider: GradleRunAnythingProvider

  override fun setUp() {
    super.setUp()
    provider = GradleRunAnythingProvider()
    myDataContext = SimpleDataContext.getProjectContext(myProject)
  }

  fun getRootProjectTasks(prefix: String = "") =
    listOf("init", "wrapper").map { prefix + it }

  fun getCommonTasks(prefix: String = "") =
    listOf("components", "projects", "dependentComponents", "buildEnvironment", "dependencyInsight", "dependencies",
           "prepareKotlinBuildScriptModel", "help", "model", "properties", "tasks").map { prefix + it }

  fun getGradleOptions(prefix: String = ""): List<String> {
    val options = GradleCommandLineOptionsProvider.getSupportedOptions().options.filterIsInstance<Option>()
    val longOptions = options.mapNotNull { it.longOpt }.map { "--$it" }
    val shortOptions = options.mapNotNull { it.opt }.map { "-$it" }
    return (longOptions + shortOptions).map { prefix + it }
  }

  fun withVariantsFor(command: String, moduleName: String, action: (List<String>) -> Unit) {
    val moduleManager = ModuleManager.getInstance(myProject)
    val module = moduleManager.findModuleByName(moduleName)
    requireNotNull(module) { "Module '$moduleName' not found at ${moduleManager.modules.map { it.name }}" }
    withVariantsFor(RunAnythingContext.ModuleContext(module), command, action)
  }

  fun withVariantsFor(command: String, action: (List<String>) -> Unit) {
    withVariantsFor(RunAnythingContext.ProjectContext(myProject), command, action)
  }

  private fun withVariantsFor(context: RunAnythingContext, command: String, action: (List<String>) -> Unit) {
    val contextKey = RunAnythingProvider.EXECUTING_CONTEXT.name
    val dataContext = SimpleDataContext.getSimpleContext(contextKey, context, myDataContext)
    val variants = provider.getValues(dataContext, "gradle $command")
    action(variants.map { StringUtil.trimStart(it, "gradle ") })
  }

  fun createTestJavaClass(name: String) {
    createProjectSubFile("src/test/java/org/jetbrains/$name.java", """
      package org.jetbrains;
      import org.junit.Test;
      public class $name {
        @Test public void test() {}
      }
    """.trimIndent())
  }

  companion object {
    /**
     * It's sufficient to run the test against one gradle version
     */
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests(): Collection<Array<out String>> = arrayListOf(arrayOf(BASE_GRADLE_VERSION))
  }
}