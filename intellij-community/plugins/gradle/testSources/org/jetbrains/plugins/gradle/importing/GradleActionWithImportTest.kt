// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.extensions.Extensions
import org.assertj.core.api.Assertions.assertThat
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.idea.IdeaModule
import org.gradle.tooling.model.idea.IdeaProject
import org.jetbrains.plugins.gradle.model.ProjectImportExtraModelProvider
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Test
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class GradleActionWithImportTest: GradleImportingTestCase() {

  @Test
  @TargetVersions("4.8+")
  fun testActionExecutionOnImport() {
    val testFile = File(projectPath, "testFile")
    assertThat(testFile).doesNotExist()

    val point = Extensions.getRootArea().getExtensionPoint(GradleProjectResolverExtension.EP_NAME)

    val extension = TestProjectResolverExtension()
    point.registerExtension(extension, testRootDisposable)

    val randomKey = Random().nextLong().toString()

    importProject(
      """
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
            registry.register(new TestModelBuilder());
          }

          private static class TestModelBuilder implements ToolingModelBuilder {
            boolean canBuild(String modelName) {
              return 'java.lang.Object' == modelName;
            }

          @Override
          Object buildAll(String modelName, Project project) {
              StartParameter startParameter = project.getGradle().getStartParameter();
              Set<String> tasks = new HashSet<>(startParameter.getTaskNames());
              tasks.add("importTestTask");
              startParameter.setTaskNames(tasks);
              return null;
            }
          }
        }
        apply plugin: TestPlugin
        task importTestTask {
          doLast {
            def f = new File('testFile')
            f.write '$randomKey'
          }
        }
      """.trimIndent())

    extension.waitForBuildFinished(10, TimeUnit.SECONDS)
    assertThat(testFile)
      .exists()
      .hasContent(randomKey)
  }
}

class TestExtraModelProvider : ProjectImportExtraModelProvider {

  override fun populateBuildModels(controller: BuildController,
                                   project: IdeaProject,
                                   consumer: ProjectImportExtraModelProvider.BuildModelConsumer) {
    controller.findModel(Object::class.java)
  }

  override fun populateProjectModels(controller: BuildController,
                                     module: IdeaModule?,
                                     modelConsumer: ProjectImportExtraModelProvider.ProjectModelConsumer) {  }
}



class TestProjectResolverExtension : AbstractProjectResolverExtension() {
  init {
    lastBuildFinished.complete(false)
    lastBuildFinished = CompletableFuture()
  }


  override fun buildFinished() {
    lastBuildFinished.complete(true)
  }

  override fun getExtraModelProvider(): ProjectImportExtraModelProvider {
    return TestExtraModelProvider()
  }

  override fun requiresTaskRunning(): Boolean {
    return true
  }

  @Throws(Exception::class)
  fun waitForBuildFinished(timeout: Int, unit: TimeUnit): Boolean {
    return lastBuildFinished.get(timeout.toLong(), unit)
  }

  companion object {
    @Volatile
    private var lastBuildFinished = CompletableFuture<Boolean>()
  }
}
