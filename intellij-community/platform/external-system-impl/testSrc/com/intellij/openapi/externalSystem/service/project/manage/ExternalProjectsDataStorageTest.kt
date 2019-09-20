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
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.BDDAssertions.then
import org.junit.After
import org.junit.Before
import org.junit.Test
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

    val testSystemId = ProjectSystemId("Test")
    val externalName = "external_name"
    val externalProjectInfo = createExternalProjectInfo(
      testSystemId, externalName, FileUtil.toSystemIndependentName(createTempDir(suffix = externalName).canonicalPath))

    dataStorage.update(externalProjectInfo)
    dataStorage.save()
    dataStorage.load()

    val list = dataStorage.list(testSystemId)
    then(list).hasSize(1)
    then(list.iterator().next().externalProjectStructure?.data?.externalName).isEqualTo(externalName)
  }

  @Test
  fun `test external project data updated before storage initialization is not lost`() = runBlocking<Unit> {
    val dataStorage = ExternalProjectsDataStorage(myFixture.project)

    val testSystemId = ProjectSystemId("Test")
    val externalName1 = "external_name1"
    dataStorage.update(createExternalProjectInfo(
      testSystemId, externalName1, FileUtil.toSystemIndependentName(createTempDir(suffix = externalName1).canonicalPath)))
    dataStorage.load()

    val externalName2 = "external_name2"
    dataStorage.update(createExternalProjectInfo(
      testSystemId, externalName2, FileUtil.toSystemIndependentName(createTempDir(suffix = externalName2).canonicalPath)))

    val list = dataStorage.list(testSystemId)
    then(list).hasSize(2)
    then(list)
      .anyMatch { it.externalProjectStructure?.data?.externalName == externalName1 }
      .anyMatch { it.externalProjectStructure?.data?.externalName == externalName2 }
  }

  private fun createExternalProjectInfo(testId: ProjectSystemId,
                                        externalName: String,
                                        externalProjectPath: String): InternalExternalProjectInfo {
    val projectData = ProjectData(testId, externalName, externalProjectPath, externalProjectPath)
    val node = DataNode<ProjectData>(Key(ProjectData::class.jvmName, 0), projectData, null)
    return InternalExternalProjectInfo(testId, externalProjectPath, node)
  }
}