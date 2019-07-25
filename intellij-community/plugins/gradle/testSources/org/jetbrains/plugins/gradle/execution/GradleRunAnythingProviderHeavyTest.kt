// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.junit.Test
import org.junit.runners.Parameterized

class GradleRunAnythingProviderHeavyTest : GradleImportingTestCase() {

  @Test
  fun `test class completion`() {
    createTestJavaClass("ClassA")
    createTestJavaClass("ClassB")
    createTestJavaClass("ClassC")
    createTestJavaClass("ClassD")
    createTestJavaClass("ClassE")
    createTestJavaClass("ClassF")
    createTestJavaClass("ClassG")
    val buildScript = GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withJUnit("4.12")
    importProject(buildScript.generate())

    val provider = GradleRunAnythingProvider()
    val dataContext = SimpleDataContext.getProjectContext(myProject)

    val completions = setOf(
      "gradle test --tests *.ClassA",
      "gradle test --tests *.ClassB",
      "gradle test --tests *.ClassC",
      "gradle test --tests *.ClassD",
      "gradle test --tests *.ClassE",
      "gradle test --tests *.ClassF",
      "gradle test --tests *.ClassG"
    )
    val superCompletions = setOf(
      "gradle test --tests org.jetbrains.ClassA",
      "gradle test --tests org.jetbrains.ClassB",
      "gradle test --tests org.jetbrains.ClassC",
      "gradle test --tests org.jetbrains.ClassD",
      "gradle test --tests org.jetbrains.ClassE",
      "gradle test --tests org.jetbrains.ClassF",
      "gradle test --tests org.jetbrains.ClassG"
    )
    assertUnorderedElementsAreEqual(provider.getValues(dataContext, "gradle test "), "gradle test --tests")
    assertUnorderedElementsAreEqual(provider.getValues(dataContext, "gradle test -"), "gradle test --tests")
    assertUnorderedElementsAreEqual(provider.getValues(dataContext, "gradle test --"), "gradle test --tests")
    assertUnorderedElementsAreEqual(provider.getValues(dataContext, "gradle test --t"), "gradle test --tests")
    assertTrue(provider.getValues(dataContext, "gradle test --tests ").containsAll(completions))
    assertTrue(provider.getValues(dataContext, "gradle test --tests *").containsAll(completions))
    assertTrue(provider.getValues(dataContext, "gradle test --tests *.").containsAll(completions))
    assertTrue(provider.getValues(dataContext, "gradle test --tests *.Class").containsAll(completions))
    assertTrue(provider.getValues(dataContext, "gradle test --tests org.jetbrains.").containsAll(superCompletions))
    assertTrue(provider.getValues(dataContext, "gradle test --tests org.jetbrains.Class").containsAll(superCompletions))
  }

  private fun createTestJavaClass(name: String) {
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