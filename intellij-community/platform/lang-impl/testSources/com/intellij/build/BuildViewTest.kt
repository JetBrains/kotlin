// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build

import com.intellij.build.events.MessageEvent.Kind.*
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.fixtures.BuildViewTestFixture
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.util.SystemProperties
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.tree.TreeUtil
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.function.Function

class BuildViewTest : LightPlatformTestCase() {

  private lateinit var buildViewTestFixture: BuildViewTestFixture

  @Before
  override fun setUp() {
    super.setUp()
    buildViewTestFixture = BuildViewTestFixture(project)
    buildViewTestFixture.setUp()
  }
  @After
  override fun tearDown() {
    RunAll()
      .append(ThrowableRunnable { if (::buildViewTestFixture.isInitialized) buildViewTestFixture.tearDown() })
      .append(ThrowableRunnable { super.tearDown() })
      .run()
  }

  @Test
  fun `test successful build`() {
    val title = "A build"
    val buildDescriptor = DefaultBuildDescriptor(Object(), title, "", System.currentTimeMillis())
    val progressDescriptor = object : BuildProgressDescriptor {
      override fun getBuildDescriptor(): BuildDescriptor = buildDescriptor
      override fun getTitle(): String = title
    }

    // @formatter:off
    BuildViewManager
      .createBuildProgress(project)
      .start(progressDescriptor)
        .message("Root message", "Tex of the root message console", INFO, null)
        .progress("Running ...")
        .startChildProgress("Inner progress")
          .fileMessage("File message1", "message1 descriptive text", INFO, FilePosition(File("aFile.java"), 0, 0))
          .fileMessage("File message2", "message2 descriptive text", INFO, FilePosition(File("aFile.java"), 0, 0))
        .finish()
      .finish()
    // @formatter:on

    buildViewTestFixture.assertBuildViewTreeEquals(
      """
      -
       -finished
        Root message
        -Inner progress
         -aFile.java
          File message1
          File message2
      """.trimIndent()
    )

    buildViewTestFixture.assertBuildViewSelectedNode("finished", "", false)
    buildViewTestFixture.assertBuildViewSelectedNode("Root message", "Tex of the root message console\n", false)
    buildViewTestFixture.assertBuildViewSelectedNode(
      "File message1",
      "aFile.java\n" +
      "message1 descriptive text",
      false
    )
  }

  @Test
  fun `test file messages presentation`() {
    val title = "A build"
    val tempDirectory = FileUtil.getTempDirectory() + "/project"
    val buildDescriptor = DefaultBuildDescriptor(Object(), title, tempDirectory, System.currentTimeMillis())
    val progressDescriptor = object : BuildProgressDescriptor {
      override fun getBuildDescriptor(): BuildDescriptor = buildDescriptor
      override fun getTitle(): String = title
    }

    // @formatter:off
    BuildViewManager
      .createBuildProgress(project)
      .start(progressDescriptor)
      .fileMessage("message 1", "message 1 descriptive text", INFO, FilePosition(File("aFile1.java"), 0, 0))
      .fileMessage("message 1.1", "message 1.1 descriptive text", WARNING, FilePosition(File("aFile1.java"), 0, 0))

      .fileMessage("message 2", "message 2 descriptive text", WARNING, FilePosition(File(tempDirectory, "project/aFile2.java"), 0, 0))
      .fileMessage("message 2.1", "message 2.1 descriptive text", WARNING, FilePosition(File(tempDirectory), -1, -1))

      .fileMessage("message 3", "message 3 descriptive text", WARNING, FilePosition(File(tempDirectory, "anotherDir1/aFile3.java"), 0, 0))
      .fileMessage("message 3.1", "message 3.1 descriptive text", ERROR, FilePosition(File(tempDirectory, "anotherDir2/aFile3.java"), 0, 0))

      .fileMessage("message 4", "message 4 descriptive text", INFO, FilePosition(File(SystemProperties.getUserHome(), "foo/aFile4.java"), 0, 0))
      .finish()
    // @formatter:on

    buildViewTestFixture.assertBuildViewTreeEquals(
      """
      -
       -finished
        -aFile1.java
         message 1
         message 1.1
        -aFile2.java
         message 2
        message 2.1
        -aFile3.java
         message 3
        -aFile3.java
         message 3.1
        -aFile4.java
         message 4""".trimIndent()
    )

    val buildView = project.service<BuildViewManager>().getBuildView(buildDescriptor.id)
    val buildTreeConsoleView = buildView!!.getView(BuildTreeConsoleView::class.java.name, BuildTreeConsoleView::class.java)
    val visitor = runInEdtAndGet {
      val tree = buildTreeConsoleView!!.tree
      return@runInEdtAndGet CollectingTreeVisitor().also {
        TreeUtil.visitVisibleRows(tree, it)
      }
    }
    Assertions.assertThat(visitor.userObjects)
      .extracting(Function<Any?, String?> { node ->
        val presentation = (node as ExecutionNode).presentation
        if (presentation.coloredText.isEmpty()) {
          presentation.presentableText
        }
        else {
          presentation.coloredText.joinToString(separator = " =>") { it.text }
        }
      })
      .containsOnlyOnce(
        "aFile1.java =>  1 warning",
        "message 1 => :1",
        "message 1.1 => :1",
        "aFile2.java => project 1 warning",
        "message 2 => :1",
        "message 2.1",
        "aFile3.java => anotherDir1 1 warning",
        "message 3 => :1",
        "aFile3.java => anotherDir2 1 error",
        "message 3.1 => :1",
        "aFile4.java => ~/foo",
        "message 4 => :1"
      )
  }

  @Test
  fun `test build with errors`() {
    val title = "A build"
    val buildDescriptor = DefaultBuildDescriptor(Object(), title, "", System.currentTimeMillis())
    val progressDescriptor = object : BuildProgressDescriptor {
      override fun getBuildDescriptor(): BuildDescriptor = buildDescriptor
      override fun getTitle(): String = title
    }

    // @formatter:off
    BuildViewManager
      .createBuildProgress(project)
      .start(progressDescriptor)
        .message("Root message", "Tex of the root message console", INFO, null)
        .progress("Running ...")
        .startChildProgress("Inner progress")
          .fileMessage("File message1", "message1 descriptive text", ERROR, FilePosition(File("aFile.java"), 0, 0))
          .fileMessage("File message2", "message2 descriptive text", ERROR, FilePosition(File("aFile.java"), 0, 0))
        .fail()
      .fail()
    // @formatter:on

    buildViewTestFixture.assertBuildViewTreeEquals(
      """
      -
       -failed
        Root message
        -Inner progress
         -aFile.java
          File message1
          File message2
      """.trimIndent()
    )

    buildViewTestFixture.assertBuildViewSelectedNode(
      "File message1",
      "aFile.java\n" +
      "message1 descriptive text"
    )
    buildViewTestFixture.assertBuildViewSelectedNode("failed", "", false)
    buildViewTestFixture.assertBuildViewSelectedNode("Root message", "Tex of the root message console\n", false)
  }

  @Test
  fun `test cancelled build`() {
    val title = "A build"
    val buildDescriptor = DefaultBuildDescriptor(Object(), title, "", System.currentTimeMillis())
    val progressDescriptor = object : BuildProgressDescriptor {
      override fun getBuildDescriptor(): BuildDescriptor = buildDescriptor
      override fun getTitle(): String = title
    }

    // @formatter:off
    BuildViewManager
      .createBuildProgress(project)
      .start(progressDescriptor)
        .message("Root message", "Tex of the root message console", INFO, null)
        .progress("Running ...")
        .startChildProgress("Inner progress")
        .cancel()
      .cancel()
    // @formatter:on

    buildViewTestFixture.assertBuildViewTreeEquals(
      """
      -
       -cancelled
        Root message
        Inner progress
      """.trimIndent()
    )

    buildViewTestFixture.assertBuildViewSelectedNode("cancelled", "", false)
    buildViewTestFixture.assertBuildViewSelectedNode("Root message", "Tex of the root message console\n", false)
    buildViewTestFixture.assertBuildViewSelectedNode("Inner progress", "", false)
  }

  @Test
  fun `test build view listeners`() {
    val title = "A build"
    val buildDescriptor = DefaultBuildDescriptor(Object(), title, "", System.currentTimeMillis())
    val progressDescriptor = object : BuildProgressDescriptor {
      override fun getBuildDescriptor(): BuildDescriptor = buildDescriptor
      override fun getTitle(): String = title
    }

    val buildMessages = mutableListOf<String>()
    //BuildViewManager
    project.service<BuildViewManager>().addListener(
      BuildProgressListener { _, event -> buildMessages.add(event.message) },
      testRootDisposable
    )

    // @formatter:off
    BuildViewManager
      .createBuildProgress(project)
      .start(progressDescriptor)
        .output("Build greeting\n", true)
        .message("Root message", "Text of the root message console", INFO, null)
        .progress("Running ...")
        .startChildProgress("Inner progress")
          .output("inner progress output", true)
          .fileMessage("File message1", "message1 descriptive text", INFO, FilePosition(File("aFile.java"), 0, 0))
          .fileMessage("File message2", "message2 descriptive text", INFO, FilePosition(File("aFile.java"), 0, 0))
        .finish()
      .output("Build farewell", true)
      .finish()
    // @formatter:on

    buildViewTestFixture.assertBuildViewTreeEquals(
      """
      -
       -finished
        Root message
        -Inner progress
         -aFile.java
          File message1
          File message2
      """.trimIndent()
    )

    buildViewTestFixture.assertBuildViewSelectedNode("finished", "Build greeting\n" +
                                                                 "Build farewell", false)
    buildViewTestFixture.assertBuildViewSelectedNode("Inner progress", "inner progress output", false)
    buildViewTestFixture.assertBuildViewSelectedNode("Root message", "Text of the root message console\n", false)
    buildViewTestFixture.assertBuildViewSelectedNode("File message1",
                                                     "aFile.java\n" +
                                                     "message1 descriptive text",
                                                     false
    )

    assertEquals("running..." +
                 "Build greeting\n" +
                 "Root message" +
                 "Running ..." +
                 "Inner progress" +
                 "inner progress output" +
                 "File message1" +
                 "File message2" +
                 "Inner progress" +
                 "Build farewell" +
                 "finished", buildMessages.joinToString(""))
  }
}
