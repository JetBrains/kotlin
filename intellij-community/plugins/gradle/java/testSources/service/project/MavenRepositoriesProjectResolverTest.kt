// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project

import com.intellij.externalSystem.MavenRepositoryData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import org.gradle.internal.impldep.org.apache.commons.lang.NotImplementedException
import org.gradle.tooling.model.idea.IdeaModule
import org.gradle.tooling.model.idea.IdeaProject
import org.jetbrains.plugins.gradle.model.MavenRepositoryModel
import org.jetbrains.plugins.gradle.model.RepositoriesModel
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class MavenRepositoriesProjectResolverTest {

  private val myRepoList: MutableList<MavenRepositoryModel> = arrayListOf()
  private lateinit var myResolver: MavenRepositoriesProjectResolver
  private lateinit var myProjectNode: DataNode<ProjectData>
  private lateinit var myModuleNode: DataNode<ModuleData>
  private lateinit var myProject: IdeaProject
  private lateinit var myModule: IdeaModule

  @Before
  fun setUp() {
    myResolver = MavenRepositoriesProjectResolver()
    myResolver.nextResolver = mock(GradleProjectResolverExtension::class.java)

    myRepoList.clear()
    val fakeModel = MyRepositoriesModel(myRepoList)

    myProject = mock(IdeaProject::class.java)
    myModule = mock(IdeaModule::class.java)
    val projectData = ProjectData(GradleConstants.SYSTEM_ID,
                                  "testName",
                                  "fake/path",
                                  "fake/external/project/path")
    myProjectNode = DataNode(ProjectKeys.PROJECT, projectData, null)
    val moduleData = ModuleData("fakeId", GradleConstants.SYSTEM_ID, "typeId",
                                "moduleName", "fake/Path", "fake/Path")
    myModuleNode = DataNode(ProjectKeys.MODULE, moduleData, myProjectNode)

    val fakeContext = mock(ProjectResolverContext::class.java)
    `when`<RepositoriesModel>(fakeContext.getExtraProject(RepositoriesModel::class.java)).thenReturn(fakeModel)
    `when`<RepositoriesModel>(fakeContext.getExtraProject(myModule, RepositoriesModel::class.java)).thenReturn(fakeModel)
    myResolver.setProjectResolverContext(fakeContext)
  }

  @Test
  fun testProjectRepositoriesImported() {
    val mavenRepo = MyMavenRepoModel("name", "http://some.host")
    myRepoList.add(mavenRepo)

    myResolver.populateProjectExtraModels(myProject, myProjectNode)

    assertProjectContainsExactly(mavenRepo)
  }

  @Test
  fun testModuleRepositoriesImported() {
    val mavenRepo = MyMavenRepoModel("name", "http://some.host")
    myRepoList.add(mavenRepo)

    myResolver.populateModuleExtraModels(myModule, myModuleNode)

    assertProjectContainsExactly(mavenRepo)
  }

  @Test
  fun testRepositoriesDeduplicated() {
    val mavenRepo1 = MyMavenRepoModel("name", "http://some.host")
    myRepoList.add(mavenRepo1)

    myResolver.populateProjectExtraModels(myProject, myProjectNode)

    val mavenRepo2 = MyMavenRepoModel("name1", "http://some.other.host")
    myRepoList.apply {
      add(MyMavenRepoModel("name", "http://some.host"))
      add(mavenRepo2)
    }

    myResolver.populateModuleExtraModels(myModule, myModuleNode)

    assertProjectContainsExactly(mavenRepo1, mavenRepo2)
  }

  private fun assertProjectContainsExactly(vararg mavenRepoModels: MavenRepositoryModel) {
    assertEquals(myProjectNode.mavenRepositories(), mavenRepoModels.toMavenRepoData())
  }


  private fun DataNode<*>.mavenRepositories(): Collection<MavenRepositoryData> =
    ExternalSystemApiUtil.getChildren(this, MavenRepositoryData.KEY).map { it.data }

  private fun Array<out MavenRepositoryModel>.toMavenRepoData(): Collection<MavenRepositoryData> =
    this.map { MavenRepositoryData(GradleConstants.SYSTEM_ID, it.name, it.url) }


  private class MyMavenRepoModel(private val myName: String, private val myUrl: String) : MavenRepositoryModel {
    override fun getName(): String = myName
    override fun getUrl(): String = myUrl
  }

  private class MyRepositoriesModel(private val myRepositories: Collection<MavenRepositoryModel>) : RepositoriesModel {
    override fun add(model: MavenRepositoryModel) {
      throw NotImplementedException("Method not implemented for test stub")
    }

    override fun getAll(): Collection<MavenRepositoryModel> = myRepositories
  }
}