/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.tooling.builder

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.internal.ManifestInternal
import org.gradle.plugins.ear.Ear
import org.gradle.plugins.ear.EarPlugin
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.gradle.model.ear.EarConfiguration
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext
import org.jetbrains.plugins.gradle.tooling.internal.ear.EarConfigurationImpl
import org.jetbrains.plugins.gradle.tooling.internal.ear.EarModelImpl
import org.jetbrains.plugins.gradle.tooling.internal.ear.EarResourceImpl
import org.jetbrains.plugins.gradle.tooling.util.DependencyResolver
import org.jetbrains.plugins.gradle.tooling.util.SourceSetCachedFinder
import org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl

/**
 * @author Vladislav.Soroka
 */
@CompileStatic
class EarModelBuilderImpl extends AbstractModelBuilderService {

  private static final String APP_DIR_PROPERTY = "appDirName"
  private SourceSetCachedFinder mySourceSetFinder = null
  // Manifest.writeTo(Writer) was deprecated since 2.14.1 version
  // https://github.com/gradle/gradle/commit/b435112d1baba787fbe4a9a6833401e837df9246
  private static boolean is2_14_1_OrBetter = GradleVersion.current().baseVersion >= GradleVersion.version("2.14.1")

  @Override
  boolean canBuild(String modelName) {
    return EarConfiguration.name == modelName
  }

  @Nullable
  @Override
  Object buildAll(String modelName, Project project, @NotNull ModelBuilderContext context) {
    final EarPlugin earPlugin = project.plugins.findPlugin(EarPlugin)
    if (earPlugin == null) return null

    if (mySourceSetFinder == null) mySourceSetFinder = new SourceSetCachedFinder(context)

    final String appDirName = !project.hasProperty(APP_DIR_PROPERTY) ?
                              "src/main/application" : String.valueOf(project.property(APP_DIR_PROPERTY))
    def earModels = []

    def deployConfiguration = project.configurations.findByName(EarPlugin.DEPLOY_CONFIGURATION_NAME)
    def earlibConfiguration = project.configurations.findByName(EarPlugin.EARLIB_CONFIGURATION_NAME)

    DependencyResolver dependencyResolver = new DependencyResolverImpl(project, false, false, false, mySourceSetFinder)

    def deployDependencies = dependencyResolver.resolveDependencies(deployConfiguration)
    def earlibDependencies = dependencyResolver.resolveDependencies(earlibConfiguration)
    def buildDirPath = project.getBuildDir().absolutePath

    for (task in project.tasks) {
      if (task instanceof Ear) {
        final EarModelImpl earModel = new EarModelImpl(task.archiveName, appDirName, task.getLibDirName())

        final List<EarConfiguration.EarResource> earResources = []
        final Ear earTask = task as Ear

        try {
          new CopySpecWalker().walk(earTask.rootSpec, new CopySpecWalker.Visitor() {
            @Override
            void visitSourcePath(String relativePath, String path) {
              def file = new File(path)
              addPath(buildDirPath, earResources, relativePath, "",
                      file.absolute ? file : new File(earTask.project.projectDir, path),
                      deployConfiguration, earlibConfiguration)
            }

            @Override
            void visitDir(String relativePath, FileVisitDetails dirDetails) {
              addPath(buildDirPath, earResources, relativePath, dirDetails.path, dirDetails.file, deployConfiguration, earlibConfiguration)
            }

            @Override
            void visitFile(String relativePath, FileVisitDetails fileDetails) {
              addPath(buildDirPath, earResources, relativePath, fileDetails.path,
                      fileDetails.file, deployConfiguration, earlibConfiguration)
            }
          })
        }
        catch (Exception e) {
          ErrorMessageBuilder builderError = getErrorMessageBuilder(project, e)
          project.getLogger().error(builderError.build())
        }

        earModel.resources = earResources

        def deploymentDescriptor = earTask.deploymentDescriptor
        if (deploymentDescriptor != null) {
          def writer = new StringWriter()
          deploymentDescriptor.writeTo(writer)
          earModel.deploymentDescriptor = writer.toString()
        }

        earModel.archivePath = earTask.archivePath

        Manifest manifest = earTask.manifest
        if (manifest != null) {
          if (is2_14_1_OrBetter) {
            if (manifest instanceof ManifestInternal) {
              OutputStream outputStream = new ByteArrayOutputStream()
              writeToOutputStream(manifest, outputStream)
              def contentCharset = (manifest as ManifestInternal).contentCharset
              earModel.manifestContent = outputStream.toString(contentCharset)
            }
          }
          else {
            Writer writer = new StringWriter()
            writeToWriter(manifest, writer)
            earModel.manifestContent = writer.toString()
          }
        }

        earModels.add(earModel)
      }
    }

    new EarConfigurationImpl(earModels, deployDependencies, earlibDependencies)
  }

  @NotNull
  @Override
  ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    ErrorMessageBuilder.create(
      project, e, "JEE project import errors"
    ).withDescription("Ear Artifacts may not be configured properly")
  }

  @CompileDynamic
  private static Manifest writeToOutputStream(Manifest manifest, OutputStream outputStream) {
    return manifest.writeTo(outputStream)
  }

  @CompileDynamic
  private static Manifest writeToWriter(Manifest manifest, StringWriter writer) {
    return manifest.writeTo((Writer)writer)
  }

  private static boolean addPath(String buildDirPath,
                                 List<EarConfiguration.EarResource> earResources,
                                 String earRelativePath,
                                 String fileRelativePath,
                                 File file,
                                 Configuration... earConfigurations) {

    if (file.absolutePath.startsWith(buildDirPath)) return

    for (Configuration conf : earConfigurations) {
      if (conf.files.contains(file)) return
    }

    earRelativePath = earRelativePath == null ? "" : earRelativePath

    EarConfiguration.EarResource earResource = new EarResourceImpl(earRelativePath, fileRelativePath, file)
    earResources.add(earResource)
  }
}
