// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.task

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.tooling.builder.AbstractModelBuilderTest
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.After
import org.junit.Before
import org.junit.Test

class GradleTaskManagerTest: UsefulTestCase() {
  private lateinit var myTestFixture: IdeaProjectTestFixture
  private lateinit var myProject: Project
  private lateinit var tm: GradleTaskManager
  private lateinit var taskId: ExternalSystemTaskId
  private lateinit var gradleExecSettings: GradleExecutionSettings


  @Before
  override fun setUp() {
    super.setUp()
    myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name).fixture
    myTestFixture.setUp()
    myProject = myTestFixture.project

    tm = GradleTaskManager()
    taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID,
                                             ExternalSystemTaskType.EXECUTE_TASK,
                                             myProject)
    gradleExecSettings = GradleExecutionSettings(null, null,
                                                     DistributionType.WRAPPED, false)
  }

  @After
  override fun tearDown() {
    try {
      myTestFixture.tearDown()
    } finally {
      super.tearDown()
    }
  }

  @Test
  fun `test task manager uses wrapper task when configured`() {

    writeBuildScript(
      """
       | wrapper { gradleVersion = "4.8.1" }
      """.trimMargin())

    val listener = MyListener()
    runHelpTask(listener)
    assertTrue("Gradle 4.8.1 should be started", listener.anyLineContains("Welcome to Gradle 4.8.1"))
  }

  @Test
  fun `test task manager uses wrapper task when wrapper already exists`() {
    runWriteAction {
      val baseDir = PlatformTestUtil.getOrCreateProjectTestBaseDir(myProject)
      val wrapperProps = baseDir
        .createChildDirectory(this, "gradle")
        .createChildDirectory(this, "wrapper")
        .createChildData(this, "gradle-wrapper.properties")

      VfsUtil.saveText(wrapperProps, """
      distributionBase=GRADLE_USER_HOME
      distributionPath=wrapper/dists
      distributionUrl=${AbstractModelBuilderTest.DistributionLocator().getDistributionFor(GradleVersion.version("4.8"))}
      zipStoreBase=GRADLE_USER_HOME
      zipStorePath=wrapper/dists
    """.trimIndent())
    }

    writeBuildScript(
      """
       | wrapper { gradleVersion = "4.9" }
      """.trimMargin())

    val listener = MyListener()

    runHelpTask(listener)

    assertTrue("Gradle 4.9 should execute 'help' task", listener.anyLineContains("Welcome to Gradle 4.9"))
    assertFalse("Gradle 4.8 should not execute 'help' task", listener.anyLineContains("Welcome to Gradle 4.8"))
  }


  private fun runHelpTask(listener: MyListener) {
    tm.executeTasks(taskId,
                    listOf("help"),
                    myProject.basePath!!,
                    gradleExecSettings,
                    null, listener)
  }

  private fun writeBuildScript(scriptText: String) {
    runWriteAction {
      VfsUtil.saveText(PlatformTestUtil.getOrCreateProjectTestBaseDir(myProject).createChildData(this, "build.gradle"), scriptText)
    }
  }
}

class MyListener: ExternalSystemTaskNotificationListenerAdapter() {
  private val storage = mutableListOf<String>()
  override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
    storage.add(text)
  }

  fun anyLineContains(something: String): Boolean = storage.any { it.contains(something) }
}