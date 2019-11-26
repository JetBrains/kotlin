// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.application.ReadAction
import com.intellij.projectView.TestProjectTreeStructure
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.SimpleTextAttributes
import org.junit.Test
import org.junit.runners.Parameterized

@Suppress("GrUnresolvedAccess")
class GradleModulesInProjectViewTest : GradleImportingTestCase() {

  private lateinit var myStructure: TestProjectTreeStructure

  override fun setUp() {
    super.setUp()
    myStructure = TestProjectTreeStructure(myProject, testRootDisposable).apply {
      isShowLibraryContents = false
      hideExcludedFiles()
    }
  }

  @Test
  fun `test gradle source set modules`() {
    createSettingsFile("include 'subProject'")
    createProjectSubDirs("src/main", "src/test", "src/integration-test")
    createProjectSubDirs("subProject")
    importProject("""
      |apply plugin: 'java'
      |
      |sourceSets { 
      | integTest {
      |    java {
      |        srcDir file('src/integration-test/java')
      |    }
      |  }
      |}
      """.trimMargin())

    assertModules("project", "project.main", "project.test", "project.integTest", "project.subProject")

    assertProjectViewPresentation("""
      |project
      | *project*
      |  *subProject*
      |  build.gradle
      |  gradle
      |   wrapper
      |    gradle-wrapper.jar
      |    gradle-wrapper.properties
      |  settings.gradle
      |  src
      |   *main*
      |   *test*
      |   integration-test *[integTest]*
      |
      """.trimMargin(), myStructure)
  }

  private fun assertProjectViewPresentation(expected: String?, structure: AbstractTreeStructure) {
    ReadAction.run<Nothing> {
      val nodePresenter = java.util.function.Function { o: Any ->
        val node = o as AbstractTreeNode<*>
        node.update()
        val presentation = node.presentation
        val fragments = presentation.coloredText
        if (fragments.isEmpty()) presentation.presentableText
        else {
          fragments.joinToString(separator = "") {
            when (it.attributes.style) {
              SimpleTextAttributes.STYLE_BOLD -> "*${it.text}*"
              else -> it.text
            }
          }
        }
      }
      val treeStructure = PlatformTestUtil.print(structure, structure.rootElement, nodePresenter)
      assertEquals(expected, treeStructure)
    }
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