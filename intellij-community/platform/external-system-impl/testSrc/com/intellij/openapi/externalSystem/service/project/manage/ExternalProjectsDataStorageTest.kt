// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.Alarm
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.reflect.jvm.jvmName

class ExternalProjectsDataStorageTest: UsefulTestCase() {
  lateinit var myFixture: IdeaProjectTestFixture

  @Before
  override fun setUp() {
    super.setUp()
    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name).fixture
    myFixture.setUp()
  }

  @After
  override fun tearDown() {
    myFixture.tearDown()
    super.tearDown()
  }

  @Test
  fun `test external project data is saved and loaded`() = runBlocking<Unit> {
    val dataStorage = ExternalProjectsDataStorage(myFixture.project)

    val testId = ProjectSystemId("Test")
    val externalName = "external_name"
    val externalProjectPath = FileUtil.toSystemIndependentName(createTempDir(suffix = "externalProject").canonicalPath)

    val projectData = ProjectData(testId, externalName,
                                  "external_project_path",
                                  externalProjectPath)
    val node = DataNode<ProjectData>(Key(ProjectData::class.jvmName, 0), projectData, null)
    val externalProjectInfo = InternalExternalProjectInfo(testId, externalProjectPath, node)

    dataStorage.update(externalProjectInfo)
    dataStorage.save()
    dataStorage.load()

    val list = dataStorage.list(testId)
    then(list).hasSize(1)
    then(list
      .iterator()
      .next()
      .externalProjectStructure?.data?.externalName)
      .isEqualTo(externalName)
  }
}