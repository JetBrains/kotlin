// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model

import org.jetbrains.plugins.gradle.model.DefaultExternalTask
import org.jetbrains.plugins.gradle.model.ExternalTask
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File

abstract class SerializationDataTestCase {
  private val generator = DeterminedEntityGenerator()

  private val boolean get() = generator.nextBoolean()

  private val string get() = generator.nextString(10)

  private val uString get() = generator.nextUString(10)

  private val file get() = File(string)

  private val uFile get() = File(uString)

  private fun <E> generate(times: Int, generate: (Int) -> E) = (0 until times).asSequence().map(generate)

  private fun <E> listOf(generate: (Int) -> E) = generate(generator.nextInt(1, 10), generate).toList()

  private fun <E> setOf(generate: (Int) -> E) = generate(generator.nextInt(1, 10), generate).toSet()

  private val executable get() = CppExecutableImpl(uString, string, string).configure()

  private val sharedLibrary get() = CppSharedLibraryImpl(uString, string, string).configure()

  private val staticLibrary get() = CppStaticLibraryImpl(uString, string, string).configure()

  private val component get() = CppComponentImpl(string, string).configure()

  private val testSuite get() = CppTestSuiteImpl(string, string).configure()

  private val source get() = SourceFileImpl(uFile, file)

  private val macro get() = MacroDirectiveImpl(uString, string)

  private val task get() = DefaultExternalTask().configure()

  private fun DefaultExternalTask.configure() = apply {
    name = string
    qName = string
    description = string
    group = string
    type = string
    isTest = boolean
  }

  private fun <B : CppBinaryImpl> B.configure() = apply {
    compilationDetails = CompilationDetailsImpl().apply {
      frameworkSearchPaths = listOf { file }
      systemHeaderSearchPaths = listOf { file }
      userHeaderSearchPaths = listOf { file }
      sources = setOf { source }
      headerDirs = setOf { file }
      macroDefines = setOf { macro }
      macroUndefines = setOf { string }
      additionalArgs = listOf { string }
      compilerExecutable = file
      compileTask = task
      compileWorkingDir = file
    }
    linkageDetails = LinkageDetailsImpl().apply {
      linkTask = task
      outputLocation = file
      additionalArgs = listOf { string }
    }
  }

  private fun <C : CppComponentImpl> C.configure() = apply {
    addBinary(executable)
    addBinary(sharedLibrary)
    addBinary(staticLibrary)
  }

  private fun CppProjectImpl.configure() = apply {
    mainComponent = component
    testComponent = testSuite
  }

  protected fun generateCppProject() = CppProjectImpl().configure()

  protected fun assertEquals(expected: CppProject?, actual: CppProject?) {
    assertTrue((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assertEquals(expected.mainComponent, actual.mainComponent)
    assertEquals(expected.testComponent, actual.testComponent)
  }

  private fun assertEquals(expected: CppComponent?, actual: CppComponent?) {
    assertTrue((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assertEquals(expected.name, actual.name)
    assertEquals(expected.baseName, actual.baseName)
    assertEquals(expected.binaries, actual.binaries, ::assertEquals) { name }
  }

  private fun <T> assertEquals(expected: List<T>, actual: List<T>, assertEquals: (T, T) -> Unit) {
    assertEquals(expected.size, actual.size)
    expected.zip(actual).forEach { (it1, it2) -> assertEquals(it1, it2) }
  }

  private fun <T> assertEquals(expected: Set<T>, actual: Set<T>, assertEquals: (T, T) -> Unit, key: T.() -> Any) {
    val expectedMap = expected.map { it.key() to it }.toMap()
    val actualMap = actual.map { it.key() to it }.toMap()
    assertEquals(expectedMap.keys, actualMap.keys)
    expectedMap.forEach { (key, value) ->
      assertEquals(value, actualMap.getValue(key))
    }
  }

  private fun assertEquals(expected: CppBinary?, actual: CppBinary?) {
    assertTrue((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assertEquals(expected.name, actual.name)
    assertEquals(expected.baseName, actual.baseName)
    assertEquals(expected.variantName, actual.variantName)
    assertEquals(expected.compilationDetails, actual.compilationDetails)
    assertEquals(expected.linkageDetails, actual.linkageDetails)
  }

  private fun assertEquals(expected: CompilationDetails?, actual: CompilationDetails?) {
    assertTrue((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assertEquals(expected.frameworkSearchPaths, actual.frameworkSearchPaths, ::assertEquals)
    assertEquals(expected.systemHeaderSearchPaths, actual.systemHeaderSearchPaths, ::assertEquals)
    assertEquals(expected.userHeaderSearchPaths, actual.userHeaderSearchPaths, ::assertEquals)
    assertEquals(expected.sources, actual.sources, ::assertEquals) { sourceFile }
    assertEquals(expected.headerDirs, actual.headerDirs)
    assertEquals(expected.macroDefines, actual.macroDefines, ::assertEquals) { name }
    assertEquals(expected.macroUndefines, actual.macroUndefines)
    assertEquals(expected.additionalArgs, actual.additionalArgs, ::assertEquals)
    assertEquals(expected.compilerExecutable, actual.compilerExecutable)
    assertEquals(expected.compileTask, actual.compileTask)
    assertEquals(expected.compileWorkingDir, actual.compileWorkingDir)
  }

  private fun assertEquals(expected: LinkageDetails?, actual: LinkageDetails?) {
    assertTrue((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assertEquals(expected.linkTask, actual.linkTask)
    assertEquals(expected.outputLocation, actual.outputLocation)
    assertEquals(expected.additionalArgs, actual.additionalArgs, ::assertEquals)
  }

  private fun assertEquals(expected: SourceFile?, actual: SourceFile?) {
    assertTrue((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assertEquals(expected.objectFile, actual.objectFile)
    assertEquals(expected.sourceFile, actual.sourceFile)
  }

  private fun assertEquals(expected: MacroDirective?, actual: MacroDirective?) {
    assertTrue((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assertEquals(expected.name, actual.name)
    assertEquals(expected.value, actual.value)
  }

  private fun assertEquals(expected: ExternalTask?, actual: ExternalTask?) {
    assertTrue((expected == null) == (actual == null))
    if (expected == null || actual == null) return
    assertEquals(expected.description, actual.description)
    assertEquals(expected.group, actual.group)
    assertEquals(expected.isTest, actual.isTest)
    assertEquals(expected.name, actual.name)
    assertEquals(expected.qName, actual.qName)
    assertEquals(expected.type, actual.type)
  }
}