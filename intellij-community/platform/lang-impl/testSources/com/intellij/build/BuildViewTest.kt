// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build

import com.intellij.build.events.MessageEvent.Kind.ERROR
import com.intellij.build.events.MessageEvent.Kind.INFO
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.fixtures.BuildViewTestFixture
import com.intellij.util.ThrowableRunnable
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

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
      .newBuildProgress(project)
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
  fun `test build with errors`() {
    val title = "A build"
    val buildDescriptor = DefaultBuildDescriptor(Object(), title, "", System.currentTimeMillis())
    val progressDescriptor = object : BuildProgressDescriptor {
      override fun getBuildDescriptor(): BuildDescriptor = buildDescriptor
      override fun getTitle(): String = title
    }

    // @formatter:off
    BuildViewManager
      .newBuildProgress(project)
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
      .newBuildProgress(project)
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
}
