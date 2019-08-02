// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.ThrowableRunnable
import org.jetbrains.plugins.gradle.service.settings.IdeaGradleSystemSettingsControlBuilder
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

    gradleProjectSettings = GradleProjectSettings().apply { externalProjectPath = myProject.basePath }
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

  @Test
  fun `test VMOptions writing to gradle properties`() {
    // non existing org.gradle.jvmargs
    assertMigratedVMOption("", "", "org.gradle.jvmargs=\n")
    assertMigratedVMOption("", "-x", "org.gradle.jvmargs=-x\n")
    assertMigratedVMOption("foo=1", "-x", "foo=1\norg.gradle.jvmargs=-x\n")
    assertMigratedVMOption("foo=1\n", "-x", "foo=1\norg.gradle.jvmargs=-x\n")

    // matched existing org.gradle.jvmargs
    assertMigratedVMOption("org.gradle.jvmargs=\n", "-x", "org.gradle.jvmargs=-x\n")
    assertMigratedVMOption("org.gradle.jvmargs=original\n", "-x", "org.gradle.jvmargs=-x\n")
    assertMigratedVMOption("org.gradle.jvmargs:original\n", "-x", "org.gradle.jvmargs:-x\n")
    assertMigratedVMOption(" \"org.gradle.jvmargs\" : original\n", "-x", " \"org.gradle.jvmargs\" :-x\n")
    assertMigratedVMOption("#comment\norg.gradle.jvmargs:original\n", "-x", "#comment\norg.gradle.jvmargs:-x\n")
    assertMigratedVMOption("foo=1\norg.gradle.jvmargs:original\nbar=1", "-x", "foo=1\norg.gradle.jvmargs:-x\nbar=1")

    // escaping
    assertMigratedVMOption("org.gradle.jvmargs=\n", "foo\\bar#baz", "org.gradle.jvmargs=foo\\\\bar\\#baz\n")
    // multiline
    assertMigratedVMOption("foo=1\norg.gradle.jvmargs=original\nbar=2",
                           "-x\n -y", "foo=1\norg.gradle.jvmargs=-x\\n -y\nbar=2")
    assertMigratedVMOption("foo=1\norg.gradle.jvmargs=original\\\nsecond line\\\nthird line\nbar=2",
                           "-x", "foo=1\norg.gradle.jvmargs=-x\nbar=2")

    // unmatched existing org.gradle.jvmargs
    assertMigratedVMOption(" \" org.gradle.jvmargs \"=original\n", "-x",
                           " \" org.gradle.jvmargs \"=original\norg.gradle.jvmargs=-x\n")
    assertMigratedVMOption("#org.gradle.jvmargs=original\n", "-x",
                           "#org.gradle.jvmargs=original\norg.gradle.jvmargs=-x\n")
  }

  private fun assertMigratedVMOption(original: String, vmOptions: String, expected: String) {
    assertEquals(expected, IdeaGradleSystemSettingsControlBuilder.updateVMOptions(original, vmOptions))
  }
}
