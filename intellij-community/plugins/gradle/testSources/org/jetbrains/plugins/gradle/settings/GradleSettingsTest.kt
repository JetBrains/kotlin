// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThreeState.*
import com.intellij.util.ThrowableRunnable
import org.jetbrains.plugins.gradle.service.settings.GradleSettingsService
import org.jetbrains.plugins.gradle.settings.TestRunner.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class GradleSettingsTest : UsefulTestCase() {

  private lateinit var myTestFixture: IdeaProjectTestFixture
  private lateinit var myProject: Project
  private lateinit var settingsService: GradleSettingsService
  private lateinit var defaultSettings: DefaultGradleProjectSettings
  private lateinit var gradleProjectSettings: GradleProjectSettings

  @Before
  override fun setUp() {
    super.setUp()
    myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name).fixture
    myTestFixture.setUp()
    myProject = myTestFixture.project

    defaultSettings = DefaultGradleProjectSettings.getInstance(myProject)
    defaultSettings.loadState(DefaultGradleProjectSettings.MyState())
    settingsService = GradleSettingsService.getInstance(myProject)
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
    assertNull(gradleProjectSettings.testRunner)
    assertEquals(PLATFORM, defaultSettings.testRunner)
    assertEquals(PLATFORM, settingsService.getTestRunner(gradleProjectSettings.externalProjectPath))

    // check build/run defaults
    assertEquals(UNSURE, gradleProjectSettings.delegatedBuild)
    assertFalse(defaultSettings.isDelegatedBuild)
    assertFalse(settingsService.isDelegatedBuildEnabled(gradleProjectSettings.externalProjectPath))
  }

  @Test
  fun `test delegation settings per linked project`() {
    // check test runner configuration change
    gradleProjectSettings.testRunner = CHOOSE_PER_TEST
    assertEquals(PLATFORM, defaultSettings.testRunner)
    assertEquals(CHOOSE_PER_TEST, settingsService.getTestRunner(gradleProjectSettings.externalProjectPath))

    //// check project default change
    defaultSettings.testRunner = GRADLE
    assertEquals(CHOOSE_PER_TEST, gradleProjectSettings.testRunner)
    assertEquals(CHOOSE_PER_TEST, settingsService.getTestRunner(gradleProjectSettings.externalProjectPath))

    // check build/run configuration change
    gradleProjectSettings.delegatedBuild = YES
    assertFalse(defaultSettings.isDelegatedBuild)
    assertTrue(settingsService.isDelegatedBuildEnabled(gradleProjectSettings.externalProjectPath))

    //// check project default change
    defaultSettings.isDelegatedBuild = true
    gradleProjectSettings.delegatedBuild = NO
    assertFalse(settingsService.isDelegatedBuildEnabled(gradleProjectSettings.externalProjectPath))
  }
}
