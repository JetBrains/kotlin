// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.progress

import com.intellij.compiler.BaseCompilerTestCase
import com.intellij.openapi.Disposable
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.fixtures.BuildViewTestFixture
import com.intellij.util.ThrowableRunnable

class CompilerBuildViewTest : BaseCompilerTestCase() {

  private lateinit var buildViewTestFixture: BuildViewTestFixture
  private val testDisposable: Disposable = object : Disposable {
    override fun dispose() {
    }
  }

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
    build(module)
    buildViewTestFixture.assertBuildViewTreeEquals("-\n build finished")

    rebuildProject()
    buildViewTestFixture.assertBuildViewTreeEquals("-\n rebuild finished")

    rebuild(module)
    buildViewTestFixture.assertBuildViewTreeEquals("-\n recompile finished")
  }

  fun `test build with compile error`() {
    val file = createFile("src/A.java", "public class A a{}foo")
    val srcRoot = file.parent
    val module = addModule("a", srcRoot)
    build(module, true)
    buildViewTestFixture.assertBuildViewTreeEquals(
      "-\n" +
      " -build failed\n" +
      "  -src/A.java\n" +
      "   '{' expected\n" +
      "   reached end of file while parsing"
    )

    rebuildProject(true)
    buildViewTestFixture.assertBuildViewTreeEquals(
      "-\n" +
      " -rebuild failed\n" +
      "  -src/A.java\n" +
      "   '{' expected\n" +
      "   reached end of file while parsing"
    )

    rebuild(module, true)
    buildViewTestFixture.assertBuildViewTreeEquals(
      "-\n" +
      " -recompile failed\n" +
      "  -src/A.java\n" +
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
}