// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.execution.ExecutionBundle
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.project.Project
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

class AbstractExternalSystemTaskConfigurationTypeTest : UsefulTestCase() {

  private lateinit var myTestFixture: IdeaProjectTestFixture
  private lateinit var myProject: Project

  @Before
  override fun setUp() {
    super.setUp()
    myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name).fixture
    myTestFixture.setUp()
    myProject = myTestFixture.project
  }

  @After
  override fun tearDown() {
    try {
      myTestFixture.tearDown()
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test correct name generated for empty task exec settings`() {
    val testSettings = ExternalSystemTaskExecutionSettings().apply {
      externalSystemIdString = "Test"
    }
    TestCase.assertEquals(ExecutionBundle.message("run.configuration.unnamed.name.prefix"),
                          AbstractExternalSystemTaskConfigurationType.generateName(myProject,
                                                                                   testSettings)
    )
  }
}