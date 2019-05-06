// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThrowableRunnable
import org.jetbrains.plugins.gradle.settings.TestRunner.GRADLE
import org.junit.After
import org.junit.Before
import org.junit.Test

class GradleSettingsTest : UsefulTestCase() {

  private lateinit var myTestFixture: IdeaProjectTestFixture
  private lateinit var myProject: Project
  private lateinit var gradleProjectSettings: GradleProjectSettings

  @Before
  override fun setUp() {
    super.setUp()
    myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name).fixture
    myTestFixture.setUp()
    myProject = myTestFixture.project

    gradleProjectSettings = GradleProjectSettings().apply { externalProjectPath = myProject.guessProjectDir()!!.path }
    GradleSettings.getInstance(myProject).linkProject(gradleProjectSettings)
  }

  @After
  override fun tearDown() {
    RunAll()
      .append(ThrowableRunnable { myTestFixture.tearDown() })
      .append(ThrowableRunnable { super.tearDown() })
      .run()
  }

  @Test
  fun `test delegation settings default configuration`() {
    // check test runner defaults
    assertEquals(GRADLE, gradleProjectSettings.testRunner)
    assertEquals(GRADLE, GradleProjectSettings.getTestRunner(myProject,
                                                             gradleProjectSettings.externalProjectPath))

    // check build/run defaults
    assertTrue(gradleProjectSettings.delegatedBuild)
    assertTrue(GradleProjectSettings.isDelegatedBuildEnabled(myProject,
                                                             gradleProjectSettings.externalProjectPath))
  }
}
