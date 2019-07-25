// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestRunConfigurationProducer.findAllTestsTaskToRun
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Assert
import org.junit.Test

class ExternalTestsModelCompatibilityTestCase : GradleImportingTestCase() {
  @Test
  fun `test simple tests finding`() {
    val buildScript = GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withJUnit("4.12")
    importProject(buildScript.generate())
    assertTestTasks(createProjectSubFile("src/test/java/package/TestCase.java", "class TestCase"),
                    listOf(":cleanTest", ":test"))
  }

  @Test
  @TargetVersions("2.4 <=> 4.10.3")
  fun `test intellij tests finding`() {
    val buildScript = GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withJUnit("4.12")
      .addPrefix("""
        sourceSets {
          foo.java.srcDirs = ["foo-src", "foo-other-src"]
          foo.compileClasspath += sourceSets.test.runtimeClasspath
        }
      """.trimIndent())
      .addPrefix("""
        task 'foo test task'(type: Test) {
          testClassesDir = sourceSets.foo.output.classesDir
          classpath += sourceSets.foo.runtimeClasspath
        }
        task 'super foo test task'(type: Test) {
          testClassesDir = sourceSets.foo.output.classesDir
          classpath += sourceSets.foo.runtimeClasspath
        }
      """.trimIndent())
    importProject(buildScript.generate())
    assertTestTasks(createProjectSubFile("foo-src/package/TestCase.java", "class TestCase"),
                    listOf(":cleanFoo test task", ":foo test task"),
                    listOf(":cleanSuper foo test task", ":super foo test task"))
    assertTestTasks(createProjectSubFile("foo-other-src/package/TestCase.java", "class TestCase"),
                    listOf(":cleanFoo test task", ":foo test task"),
                    listOf(":cleanSuper foo test task", ":super foo test task"))
  }

  @Test
  @TargetVersions("4.0+")
  fun `test intellij tests finding new interface`() {
    val buildScript = GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withJUnit("4.12")
      .addPrefix("""
        sourceSets {
          foo.java.srcDirs = ["foo-src", "foo-other-src"]
          foo.compileClasspath += sourceSets.test.runtimeClasspath
        }
      """.trimIndent())
      .addPrefix("""
        task 'foo test task'(type: Test) {
          testClassesDirs = sourceSets.foo.output.classesDirs
          classpath += sourceSets.foo.runtimeClasspath
        }
        task 'super foo test task'(type: Test) {
          testClassesDirs = sourceSets.foo.output.classesDirs
          classpath += sourceSets.foo.runtimeClasspath
        }
      """.trimIndent())
    importProject(buildScript.generate())
    assertTestTasks(createProjectSubFile("foo-src/package/TestCase.java", "class TestCase"),
                    listOf(":cleanFoo test task", ":foo test task"),
                    listOf(":cleanSuper foo test task", ":super foo test task"))
    assertTestTasks(createProjectSubFile("foo-other-src/package/TestCase.java", "class TestCase"),
                    listOf(":cleanFoo test task", ":foo test task"),
                    listOf(":cleanSuper foo test task", ":super foo test task"))
  }

  private fun assertTestTasks(source: VirtualFile, vararg expected: List<String>) {
    val tasks = findAllTestsTaskToRun(source, myProject)
    Assert.assertEquals(expected.toList(), tasks)
  }
}