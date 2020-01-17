// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.test.ExternalSystemTestUtil.assertMapsEqual
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.testFramework.registerServiceInstance
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.BuildModel
import org.gradle.tooling.model.Model
import org.gradle.tooling.model.ProjectModel
import org.gradle.tooling.model.gradle.GradleBuild
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.model.ModelsHolder
import org.jetbrains.plugins.gradle.model.Project
import org.jetbrains.plugins.gradle.model.ProjectImportAction
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider
import org.jetbrains.plugins.gradle.service.project.*
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.junit.Test
import java.io.Serializable
import java.util.function.Predicate

class GradleIncrementalImportingTest : BuildViewMessagesImportingTestCase() {
  override fun setUp() {
    super.setUp()
    myProject.registerServiceInstance(ModelConsumer::class.java, ModelConsumer())
    GradleProjectResolverExtension.EP_NAME.getPoint(null).registerExtension(TestProjectIncrementalResolverExtension(), testRootDisposable)
    ProjectModelContributor.EP_NAME.getPoint(null).registerExtension(TestProjectModelContributor(), testRootDisposable)
  }


  @Test
  fun `test incremental re-import`() {
    createAndImportTestProject()
    assertReceivedModels(mapOf("prop1" to "val1"),
                         mapOf("prop2" to "val2"))

    val initialProjectStructure = ProjectDataManager.getInstance()
      .getExternalProjectData(myProject, SYSTEM_ID, projectPath)!!
      .externalProjectStructure!!
      .graphCopy()

    createProjectSubFile("gradle.properties", "prop1=val1_inc\n" +
                                              "prop2=val2_inc\n")
    cleanupBeforeReImport()
    ExternalSystemUtil.refreshProject(projectPath,
                                      ImportSpecBuilder(myProject, SYSTEM_ID)
                                        .use(ProgressExecutionMode.MODAL_SYNC)
                                        .projectResolverPolicy(
                                          GradleIncrementalResolverPolicy(Predicate { it is TestProjectIncrementalResolverExtension }))
    )

    assertSyncViewTreeEquals("-\n" +
                             " finished")

    assertReceivedModels(mapOf("prop1" to "val1_inc"),
                         mapOf("prop2" to "val2_inc"))

    val projectStructureAfterIncrementalImport = ProjectDataManager.getInstance()
      .getExternalProjectData(myProject, SYSTEM_ID, projectPath)!!
      .externalProjectStructure!!
      .graphCopy()

    assertEquals(initialProjectStructure, projectStructureAfterIncrementalImport)
  }

  @Test
  fun `test import cancellation on project loaded phase`() {
    createAndImportTestProject()
    assertReceivedModels(mapOf("prop1" to "val1"),
                         mapOf("prop2" to "val2"))

    createProjectSubFile("gradle.properties", "prop1=error\n" +
                                              "prop2=val22\n")
    cleanupBeforeReImport()
    ExternalSystemUtil.refreshProject(projectPath, ImportSpecBuilder(myProject, SYSTEM_ID).use(ProgressExecutionMode.MODAL_SYNC))

    if (currentGradleBaseVersion >= GradleVersion.version("4.8")) {
      assertSyncViewTreeEquals("-\n" +
                               " -failed\n" +
                               "  Build cancelled")
      assertReceivedModels(mapOf("prop1" to "error"))
    }
    else {
      assertSyncViewTreeEquals("-\n" +
                               " finished")
      assertReceivedModels(mapOf("prop1" to "error"), mapOf("prop2" to "val22"))
    }
  }

  private fun createAndImportTestProject() {
    val injectModelBuilder = """
          import org.gradle.api.Project;
          import javax.inject.Inject;
          import org.gradle.tooling.provider.model.ToolingModelBuilder;
          import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
          class TestPlugin implements Plugin<Project> {
            private ToolingModelBuilderRegistry registry;
  
            @Inject
            TestPlugin(ToolingModelBuilderRegistry registry) {
              this.registry = registry;
            }
  
            void apply(Project project) {
              registry.register(new MyModelBuilder1())
              registry.register(new MyModelBuilder2())
            }
  
            private static class MyModelBuilder1 implements ToolingModelBuilder {
              boolean canBuild(String modelName) {
                return 'java.util.HashMap' == modelName;
              }
  
            @Override
            Object buildAll(String modelName, Project project) {
                def map = new HashMap<>()
                map.put("prop1", project.properties['prop1'])
                return map;
              }
            }
            
            private static class MyModelBuilder2 implements ToolingModelBuilder {
              boolean canBuild(String modelName) {
                return 'java.util.LinkedHashMap' == modelName;
              }
  
            @Override
            Object buildAll(String modelName, Project project) {
                def map = new LinkedHashMap<>()
                map.put("prop2", project.properties['prop2'])
                return map;
              }
            }
          }
        """.trimIndent()
    createProjectSubFile("gradle.properties", "prop1=val1\n" +
                                              "prop2=val2\n")

    importProject(GradleBuildScriptBuilderEx()
                    .withJavaPlugin()
                    .addPostfix(injectModelBuilder)
                    .applyPlugin("TestPlugin")
                    .generate())
  }

  private fun cleanupBeforeReImport() {
    myProject.getService(ModelConsumer::class.java).projectLoadedModels.clear()
    myProject.getService(ModelConsumer::class.java).buildFinishedModels.clear()
  }

  private fun assertReceivedModels(expectedProjectLoadedModelsMap: Map<String, String>,
                                   expectedBuildFinishedModelsMap: Map<String, String>? = null) {
    val modelConsumer = myProject.getService(ModelConsumer::class.java)
    assertSize(1, modelConsumer.projectLoadedModels)
    assertMapsEqual(expectedProjectLoadedModelsMap, modelConsumer.projectLoadedModels.first().second.map)
    if (expectedBuildFinishedModelsMap != null) {
      assertSize(1, modelConsumer.buildFinishedModels)
      assertMapsEqual(expectedBuildFinishedModelsMap, modelConsumer.buildFinishedModels.first().second.map)
    }
    else {
      assertEmpty(modelConsumer.buildFinishedModels)
    }
  }
}

class TestProjectIncrementalResolverExtension : AbstractProjectResolverExtension() {

  override fun projectsLoaded(models: ModelsHolder<BuildModel, ProjectModel>?) {
    val buildFinishedModel = models?.getModel(BuildFinishedModel::class.java)
    if (buildFinishedModel != null) {
      throw ProcessCanceledException(RuntimeException("buildFinishedModel should not be available for projectsLoaded callback"))
    }

    val projectLoadedModel = models?.getModel(ProjectLoadedModel::class.java)
    if (projectLoadedModel == null) {
      throw ProcessCanceledException(RuntimeException("projectLoadedModel should be available for projectsLoaded callback"))
    }

    if (projectLoadedModel.map.containsValue("error")) {
      val modelConsumer = resolverCtx.externalSystemTaskId.findProject()!!.getService(ModelConsumer::class.java)
      val build = (models as ProjectImportAction.AllModels).mainBuild
      for (project in build.projects) {
        modelConsumer.projectLoadedModels.add(project to models.getModel(project, ProjectLoadedModel::class.java)!!)
      }
      throw ProcessCanceledException(RuntimeException(projectLoadedModel.map.toString()))
    }
  }

  override fun getProjectsLoadedModelProvider(): ProjectImportModelProvider? {
    return object : ProjectImportModelProvider {
      override fun populateProjectModels(controller: BuildController,
                                         projectModel: Model,
                                         modelConsumer: ProjectImportModelProvider.ProjectModelConsumer) {
        val model = controller.getModel(projectModel, HashMap::class.java)
        modelConsumer.consume(ProjectLoadedModel(model), ProjectLoadedModel::class.java)
      }

      override fun populateBuildModels(controller: BuildController,
                                       buildModel: GradleBuild,
                                       consumer: ProjectImportModelProvider.BuildModelConsumer) {
      }
    }
  }

  override fun getModelProvider(): ProjectImportModelProvider? {
    return object : ProjectImportModelProvider {
      override fun populateProjectModels(controller: BuildController,
                                         projectModel: Model,
                                         modelConsumer: ProjectImportModelProvider.ProjectModelConsumer) {
        val model = controller.getModel(projectModel, LinkedHashMap::class.java)
        modelConsumer.consume(BuildFinishedModel(model), BuildFinishedModel::class.java)
      }

      override fun populateBuildModels(controller: BuildController,
                                       buildModel: GradleBuild,
                                       consumer: ProjectImportModelProvider.BuildModelConsumer) {
      }
    }
  }
}

internal class TestProjectModelContributor() : ProjectModelContributor {
  override fun accept(projectModelBuilder: ProjectModelBuilder,
                      toolingModelsProvider: ToolingModelsProvider,
                      resolverContext: ProjectResolverContext) {
    val modelConsumer = resolverContext.externalSystemTaskId.findProject()!!.getService(ModelConsumer::class.java)
    toolingModelsProvider.projects().forEach {
      modelConsumer.projectLoadedModels.add(it to toolingModelsProvider.getProjectModel(it, ProjectLoadedModel::class.java)!!)
      modelConsumer.buildFinishedModels.add(it to toolingModelsProvider.getProjectModel(it, BuildFinishedModel::class.java)!!)
    }
  }
}

internal data class ModelConsumer(val projectLoadedModels: MutableList<Pair<Project, ProjectLoadedModel>> = mutableListOf(),
                                  val buildFinishedModels: MutableList<Pair<Project, BuildFinishedModel>> = mutableListOf())

data class ProjectLoadedModel(val map: Map<*, *>) : Serializable
data class BuildFinishedModel(val map: Map<*, *>) : Serializable