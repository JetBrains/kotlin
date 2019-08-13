// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.gradle.settings.TestRunner
import org.jetbrains.plugins.gradle.util.runReadActionAndWait
import org.junit.Test

class GradleTestRunConfigurationProducerTest : GradleTestRunConfigurationProducerTestCase() {

  @Test
  fun `test simple configuration`() {
    val projectData = generateAndImportTemplateProject()
    assertConfigurationFromContext<TestMethodGradleConfigurationProducer>(
      """:cleanTest :test --tests "TestCase.test1"""",
      projectData["project"]["TestCase"]["test1"].element
    )
    assertConfigurationFromContext<TestClassGradleConfigurationProducer>(
      """:cleanTest :test --tests "TestCase"""",
      projectData["project"]["TestCase"].element
    )
    assertConfigurationFromContext<AllInPackageGradleConfigurationProducer>(
      """:cleanTest :test --tests "pkg.*"""",
      runReadActionAndWait { projectData["project"]["pkg.TestCase"].element.containingFile.containingDirectory }
    )
  }

  @Test
  fun `test pattern configuration`() {
    val projectData = generateAndImportTemplateProject()
    assertConfigurationFromContext<PatternGradleConfigurationProducer>(
      """:cleanTest :test --tests "TestCase.test1" --tests "pkg.TestCase.test1" """ +
      """:module:cleanTest :module:test --tests "ModuleTestCase.test1" --continue""",
      projectData["project"]["TestCase"]["test1"].element,
      projectData["project"]["pkg.TestCase"]["test1"].element,
      projectData["module"]["ModuleTestCase"]["test1"].element
    )
    assertConfigurationFromContext<PatternGradleConfigurationProducer>(
      """:cleanTest :test --tests "TestCase" --tests "pkg.TestCase" """ +
      """:module:cleanTest :module:test --tests "ModuleTestCase" --continue""",
      projectData["project"]["TestCase"].element,
      projectData["project"]["pkg.TestCase"].element,
      projectData["module"]["ModuleTestCase"].element
    )
    assertConfigurationFromContext<PatternGradleConfigurationProducer>(
      """:cleanTest :test --tests "TestCase.test1" --tests "pkg.TestCase.test1" """ +
      """:module:cleanTest :module:test --tests "ModuleTestCase" --continue""",
      projectData["project"]["TestCase"]["test1"].element,
      projectData["project"]["pkg.TestCase"]["test1"].element,
      projectData["module"]["ModuleTestCase"].element
    )
  }

  @Test
  fun `test configuration producer properly matches configuration with context`() {
    val projectData = generateAndImportTemplateProject()
    runReadActionAndWait {
      val locations = listOf(projectData["project"]["TestCase"]["test1"].element,
                             projectData["project"]["TestCase"].element,
                             projectData["project"]["pkg.TestCase"].element.containingFile.containingDirectory,
                             projectData["project"]["TestCase"]["test2"].element)

      val contexts = (locations + locations).map { getContextByLocation(it) } // with duplicate locations
      val cfgFromCtx = contexts.map { getConfigurationFromContext(it) }
      val configurations = cfgFromCtx.map { it.configuration as ExternalSystemRunConfiguration }
      val methodProd = getConfigurationProducer<TestMethodGradleConfigurationProducer>()
      val classProd = getConfigurationProducer<TestClassGradleConfigurationProducer>()
      val packageProd = getConfigurationProducer<AllInPackageGradleConfigurationProducer>()

      for ((j, configuration) in configurations.withIndex()) {
        for ((k, context) in contexts.withIndex()) {
          val cfgMatchesContext = j % locations.size == k % locations.size

          val isPsiMethod = context.psiLocation is PsiMethod
          assertEquals(isPsiMethod && cfgMatchesContext, methodProd.isConfigurationFromContext(configuration, context))

          val isPsiClass = context.psiLocation is PsiClass
          assertEquals(isPsiClass && cfgMatchesContext, classProd.isConfigurationFromContext(configuration, context))

          val isPsiDir = context.psiLocation is PsiDirectory
          assertEquals(isPsiDir && cfgMatchesContext, packageProd.isConfigurationFromContext(configuration, context))
        }
      }

      val context = getContextByLocation(*locations.toTypedArray())

      assertFalse(methodProd.isConfigurationFromContext(configurations[0], context))
      assertFalse(classProd.isConfigurationFromContext(configurations[1], context))
      assertFalse(packageProd.isConfigurationFromContext(configurations[2], context))
    }
  }

  @Test
  fun `test configuration escaping`() {
    val projectData = generateAndImportTemplateProject()
    assertConfigurationFromContext<TestMethodGradleConfigurationProducer>(
      """':my module:cleanTest' ':my module:test' --tests "MyModuleTestCase.test1"""",
      projectData["my module"]["MyModuleTestCase"]["test1"].element
    )
    assertConfigurationFromContext<TestClassGradleConfigurationProducer>(
      """':my module:cleanTest' ':my module:test' --tests "MyModuleTestCase"""",
      projectData["my module"]["MyModuleTestCase"].element
    )
    assertConfigurationFromContext<PatternGradleConfigurationProducer>(
      """':my module:cleanTest' ':my module:test' --tests "MyModuleTestCase.test1" --tests "MyModuleTestCase.test2"""",
      projectData["my module"]["MyModuleTestCase"]["test1"].element,
      projectData["my module"]["MyModuleTestCase"]["test2"].element
    )
    assertConfigurationFromContext<TestMethodGradleConfigurationProducer>(
      """:cleanTest :test --tests "GroovyTestCase.Don\'t use single * quo\*tes"""",
      projectData["project"]["GroovyTestCase"]["""Don\'t use single . quo\"tes"""].element
    )
    assertConfigurationFromContext<PatternGradleConfigurationProducer>(
      """:cleanTest :test --tests "GroovyTestCase.Don\'t use single * quo\*tes" --tests "GroovyTestCase.test2"""",
      projectData["project"]["GroovyTestCase"]["""Don\'t use single . quo\"tes"""].element,
      projectData["project"]["GroovyTestCase"]["test2"].element
    )
  }

  @Test
  fun `test configuration different tasks in single module`() {
    currentExternalProjectSettings.isResolveModulePerSourceSet = false
    val projectData = generateAndImportTemplateProject()
    assertConfigurationFromContext<PatternGradleConfigurationProducer>(
      """:cleanTest :test --tests "TestCase" :cleanAutoTest :autoTest --tests "AutomationTestCase" --continue""",
      projectData["project"]["TestCase"].element,
      projectData["project"]["AutomationTestCase"].element,
      testTasksFilter = { it in setOf("test", "autoTest") }
    )
  }

  @Test
  fun `test configuration tests for directory`() {
    val projectData = generateAndImportTemplateProject()
    assertConfigurationFromContext<AllInDirectoryGradleConfigurationProducer>(
      """:cleanAutoTest :autoTest --tests * :cleanAutomationTest :automationTest --tests * :cleanTest :test --tests * --continue""",
      projectData["project"].root
    )
    assertConfigurationFromContext<AllInDirectoryGradleConfigurationProducer>(
      """:cleanTest :test --tests *""",
      projectData["project"].root.subDirectory("src")
    )
    assertConfigurationFromContext<AllInDirectoryGradleConfigurationProducer>(
      """:cleanTest :test --tests *""",
      projectData["project"].root.subDirectory("src", "test")
    )
    assertConfigurationFromContext<AllInDirectoryGradleConfigurationProducer>(
      """:cleanTest :test --tests *""",
      projectData["project"].root.subDirectory("src", "test", "java")
    )
    assertConfigurationFromContext<AllInPackageGradleConfigurationProducer>(
      """:cleanTest :test --tests "pkg.*"""",
      projectData["project"].root.subDirectory("src", "test", "java", "pkg")
    )
    assertConfigurationFromContext<AllInDirectoryGradleConfigurationProducer>(
      """:cleanAutoTest :autoTest --tests * :cleanAutomationTest :automationTest --tests * --continue""",
      projectData["project"].root.subDirectory("automation")
    )
    assertConfigurationFromContext<AllInDirectoryGradleConfigurationProducer>(
      """:module:cleanTest :module:test --tests *""",
      projectData["module"].root
    )
  }

  @Test
  fun `test producer choosing per run`() {
    currentExternalProjectSettings.isResolveModulePerSourceSet = false
    currentExternalProjectSettings.testRunner = TestRunner.CHOOSE_PER_TEST
    val projectData = generateAndImportTemplateProject()
    assertProducersFromContext(
      projectData["project"].root,
      "AllInPackageConfigurationProducer",
      "AllInDirectoryGradleConfigurationProducer"
    )
    assertProducersFromContext(
      projectData["project"].root.subDirectory("src"),
      "AllInDirectoryGradleConfigurationProducer"
    )
    assertProducersFromContext(
      projectData["project"].root.subDirectory("src", "test"),
      "AllInDirectoryGradleConfigurationProducer"
    )
    assertProducersFromContext(
      projectData["project"].root.subDirectory("src", "test", "java"),
      "AbstractAllInDirectoryConfigurationProducer",
      "AllInDirectoryGradleConfigurationProducer"
    )
    assertProducersFromContext(
      projectData["project"].root.subDirectory("src", "test", "java", "pkg"),
      "AbstractAllInDirectoryConfigurationProducer",
      "AllInPackageGradleConfigurationProducer"
    )
    assertProducersFromContext(
      projectData["project"].root.subDirectory("automation"),
      "AbstractAllInDirectoryConfigurationProducer",
      "AllInDirectoryGradleConfigurationProducer"
    )
    assertProducersFromContext(
      projectData["module"].root,
      "AllInPackageConfigurationProducer",
      "AllInDirectoryGradleConfigurationProducer"
    )
  }

  @Test
  fun `test multiple selected abstract tests`() {
    val projectData = generateAndImportTemplateProject()
    runReadActionAndWait {
      val producer = getConfigurationProducer<PatternGradleConfigurationProducer>()
      val testClass = projectData["project"]["TestCase"].element
      val abstractTestClass = projectData["project"]["AbstractTestCase"].element
      val abstractTestMethod = projectData["project"]["AbstractTestCase"]["test"].element
      val templateConfiguration = producer.createTemplateConfiguration()
      getContextByLocation(testClass, abstractTestClass).let {
        assertTrue(producer.setupConfigurationFromContext(templateConfiguration, it, Ref(it.psiLocation)))
      }
      getContextByLocation(abstractTestClass, abstractTestMethod).let {
        assertFalse(producer.setupConfigurationFromContext(templateConfiguration, it, Ref(it.psiLocation)))
      }
      getContextByLocation(abstractTestClass, abstractTestClass).let {
        assertFalse(producer.setupConfigurationFromContext(templateConfiguration, it, Ref(it.psiLocation)))
      }
      getContextByLocation(abstractTestMethod, abstractTestMethod).let {
        assertFalse(producer.setupConfigurationFromContext(templateConfiguration, it, Ref(it.psiLocation)))
      }
    }
  }
}
