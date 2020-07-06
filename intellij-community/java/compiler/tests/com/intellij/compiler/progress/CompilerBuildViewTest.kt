// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.progress

import com.intellij.compiler.BaseCompilerTestCase
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.PsiTestUtil.addSourceRoot
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.fixtures.BuildViewTestFixture
import com.intellij.util.ThrowableRunnable
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.jps.model.java.JavaResourceRootType

class CompilerBuildViewTest : BaseCompilerTestCase() {

  private lateinit var buildViewTestFixture: BuildViewTestFixture
  private val testDisposable: Disposable = Disposer.newDisposable()

  @Throws(Exception::class)
  public override fun setUp() {
    super.setUp()
    buildViewTestFixture = BuildViewTestFixture(project)
    buildViewTestFixture.setUp()
    val registryValue = Registry.get("ide.jps.use.build.tool.window")
    registryValue.setValue(true, testDisposable)
  }

  public override fun tearDown() {
    RunAll()
      .append(ThrowableRunnable { if (::buildViewTestFixture.isInitialized) buildViewTestFixture.tearDown() })
      .append(ThrowableRunnable { Disposer.dispose(testDisposable) })
      .append(ThrowableRunnable { super.tearDown() })
      .run()
  }

  fun `test empty build`() {
    make(compilerManager.createProjectCompileScope(myProject))
    buildViewTestFixture.assertBuildViewTreeEquals("-\n build finished")
    buildViewTestFixture.assertBuildViewSelectedNode("build finished", "", false)

    rebuildProject(false)
    buildViewTestFixture.assertBuildViewTreeEquals("-\n rebuild finished")
    buildViewTestFixture.assertBuildViewSelectedNode("rebuild finished", "", false)

    compile(compilerManager.createProjectCompileScope(myProject), true)
    buildViewTestFixture.assertBuildViewTreeEquals("-\n recompile finished")
    buildViewTestFixture.assertBuildViewSelectedNode("recompile finished", "", false)
  }

  fun `test successful build`() {
    val file = createFile("src/A.java", "public class A {}")
    val srcRoot = file.parent
    val module = addModule("a", srcRoot)
    val propFile = createFile("resources/foo.properties", "bar=baz")
    runWriteAction { addSourceRoot(module, propFile.parent, JavaResourceRootType.RESOURCE) }

    build(module)
    buildViewTestFixture.assertBuildViewTreeEquals("-\n build finished")

    runWithProgressExIndicatorSupport { rebuildProject() }
    buildViewTestFixture.assertBuildViewTreeEquals("-\n rebuild finished")
    buildViewTestFixture.assertBuildViewSelectedNode("rebuild finished", false) { output: String? ->
      assertThat(output).startsWith("Clearing build system data...\n" +
                                    "Executing pre-compile tasks...\n" +
                                    "Loading Ant Configuration...\n" +
                                    "Running Ant Tasks...\n" +
                                    "Cleaning output directories...\n" +
                                    "Running 'before' tasks\n" +
                                    "Checking sources\n" +
                                    "Copying resources... [a]\n" +
                                    "Parsing java... [a]\n" +
                                    "Writing classes... [a]\n" +
                                    "Updating dependency information... [a]\n" +
                                    "Adding @NotNull assertions... [a]\n" +
                                    "Adding pattern assertions... [a]\n" +
                                    "Running 'after' tasks\n")
      assertThat(output).contains("Finished, saving caches...\n" +
                                  "Executing post-compile tasks...\n" +
                                  "Loading Ant Configuration...\n" +
                                  "Running Ant Tasks...\n" +
                                  "Synchronizing output directories...")
    }

    runWithProgressExIndicatorSupport { rebuild(module) }
    buildViewTestFixture.assertBuildViewTreeEquals("-\n recompile finished")
    buildViewTestFixture.assertBuildViewSelectedNode("recompile finished", false) { output: String? ->
      assertThat(output).startsWith("Executing pre-compile tasks...\n" +
                                    "Loading Ant Configuration...\n" +
                                    "Running Ant Tasks...\n" +
                                    "Cleaning output directories...\n" +
                                    "Running 'before' tasks\n" +
                                    "Checking sources\n" +
                                    "Copying resources... [a]\n" +
                                    "Parsing java... [a]\n" +
                                    "Writing classes... [a]\n" +
                                    "Updating dependency information... [a]\n" +
                                    "Adding @NotNull assertions... [a]\n" +
                                    "Adding pattern assertions... [a]\n" +
                                    "Running 'after' tasks")
      assertThat(output).contains("Finished, saving caches...\n" +
                                  "Executing post-compile tasks...\n" +
                                  "Loading Ant Configuration...\n" +
                                  "Running Ant Tasks...\n" +
                                  "Synchronizing output directories...")
    }
  }

  fun `test build with compile error`() {
    val file = createFile("src/A.java", "public class A a{}foo")
    val srcRoot = file.parent
    val module = addModule("a", srcRoot)
    build(module, true)
    buildViewTestFixture.assertBuildViewTreeEquals(
      "-\n" +
      " -build failed\n" +
      "  -A.java\n" +
      "   '{' expected\n" +
      "   reached end of file while parsing"
    )

    rebuildProject(true)
    buildViewTestFixture.assertBuildViewTreeEquals(
      "-\n" +
      " -rebuild failed\n" +
      "  -A.java\n" +
      "   '{' expected\n" +
      "   reached end of file while parsing"
    )

    rebuild(module, true)
    buildViewTestFixture.assertBuildViewTreeEquals(
      "-\n" +
      " -recompile failed\n" +
      "  -A.java\n" +
      "   '{' expected\n" +
      "   reached end of file while parsing"
    )
  }

  private fun build(module: Module, errorsExpected: Boolean = false): CompilationLog? {
    val compileScope = compilerManager.createModuleCompileScope(module, false)
    return compile(compileScope, false, errorsExpected)
  }

  private fun rebuild(module: Module, errorsExpected: Boolean = false): CompilationLog? {
    val compileScope = compilerManager.createModuleCompileScope(module, false)
    return compile(compileScope, true, errorsExpected)
  }

  private fun rebuildProject(errorsExpected: Boolean = false): CompilationLog? {
    return compile(errorsExpected) { compileStatusNotification: CompileStatusNotification? ->
      compilerManager.rebuild(compileStatusNotification)
    }
  }

  private fun runWithProgressExIndicatorSupport(action: () -> Unit) {
    val key = "intellij.progress.task.ignoreHeadless"
    val prev = System.setProperty(key, "true")
    try {
      return action()
    }
    finally {
      if (prev != null) {
        System.setProperty(key, prev)
      }
      else {
        System.clearProperty(key)
      }
    }
  }
}