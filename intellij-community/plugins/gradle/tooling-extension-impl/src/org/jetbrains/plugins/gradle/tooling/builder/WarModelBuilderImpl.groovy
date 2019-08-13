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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.java.archives.Manifest
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.gradle.model.web.WebConfiguration
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.tooling.internal.web.WarModelImpl
import org.jetbrains.plugins.gradle.tooling.internal.web.WebConfigurationImpl
import org.jetbrains.plugins.gradle.tooling.internal.web.WebResourceImpl

/**
 * @author Vladislav.Soroka
 */
class WarModelBuilderImpl implements ModelBuilderService {

  private static final String WEB_APP_DIR_PROPERTY = "webAppDir"
  private static final String WEB_APP_DIR_NAME_PROPERTY = "webAppDirName"
  private static is4OrBetter = GradleVersion.current().baseVersion >= GradleVersion.version("4.0")


  @Override
  boolean canBuild(String modelName) {
    return WebConfiguration.name.equals(modelName)
  }

  @Nullable
  @Override
  Object buildAll(String modelName, Project project) {
    final WarPlugin warPlugin = project.plugins.findPlugin(WarPlugin)
    if (warPlugin == null) return null

    final String webAppDirName = !project.hasProperty(WEB_APP_DIR_NAME_PROPERTY) ?
                                 "src/main/webapp" : String.valueOf(project.property(WEB_APP_DIR_NAME_PROPERTY))

    final File webAppDir = !project.hasProperty(WEB_APP_DIR_PROPERTY) ? new File(project.projectDir, webAppDirName) :
                           (File)project.property(WEB_APP_DIR_PROPERTY)

    def warModels = []

    project.tasks.each { Task task ->
      if (task instanceof War) {
        final WarModelImpl warModel =
          new WarModelImpl((task as War).archiveName, webAppDirName, webAppDir)

        final List<WebConfiguration.WebResource> webResources = []
        final War warTask = task as War
        warModel.webXml = warTask.webXml
        try {
          CopySpecWalker.walk(warTask.rootSpec, new CopySpecWalker.Visitor() {
            @Override
            void visitSourcePath(String relativePath, String path) {
              def file = new File(path)
              addPath(webResources, relativePath, "", file.absolute ? file : new File(warTask.project.projectDir, path))
            }

            @Override
            void visitDir(String relativePath, FileVisitDetails dirDetails) {
              addPath(webResources, relativePath, dirDetails.path, dirDetails.file)
            }

            @Override
            void visitFile(String relativePath, FileVisitDetails fileDetails) {
              if (warTask.webXml == null ||
                  !fileDetails.file.canonicalPath.equals(warTask.webXml.canonicalPath)) {
                addPath(webResources, relativePath, fileDetails.path, fileDetails.file)
              }
            }
          })
          warModel.classpath = new LinkedHashSet<>(warTask.classpath.files)
        }
        catch (Exception ignore) {
          ErrorMessageBuilder builderError = getErrorMessageBuilder(project, ignore)
          project.getLogger().error(builderError.build())
        }

        warModel.webResources = webResources
        warModel.archivePath = warTask.archivePath

        Manifest manifest = warTask.manifest
        if (manifest != null) {
          if(is4OrBetter) {
            if(manifest instanceof org.gradle.api.java.archives.internal.ManifestInternal) {
              ByteArrayOutputStream baos = new ByteArrayOutputStream()
              manifest.writeTo(baos)
              warModel.manifestContent = baos.toString(manifest.contentCharset)
            }
          } else {
            def writer = new StringWriter()
            manifest.writeTo(writer)
            warModel.manifestContent = writer.toString()
          }
        }
        warModels.add(warModel)
      }
    }

    new WebConfigurationImpl(warModels)
  }

  @NotNull
  @Override
  ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    ErrorMessageBuilder.create(
      project, e, "JEE project import errors"
    ).withDescription("Web Facets/Artifacts will not be configured properly")
  }

  private static addPath(List<WebConfiguration.WebResource> webResources, String warRelativePath, String fileRelativePath, File file) {
    warRelativePath = warRelativePath == null ? "" : warRelativePath

    WebConfiguration.WebResource webResource = new WebResourceImpl(warRelativePath, fileRelativePath, file)
    webResources.add(webResource)
  }
}
