// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext
import org.jetbrains.plugins.gradle.tooling.internal.web.WarModelImpl
import org.jetbrains.plugins.gradle.tooling.internal.web.WebConfigurationImpl
import org.jetbrains.plugins.gradle.tooling.internal.web.WebResourceImpl

import static org.jetbrains.plugins.gradle.tooling.internal.ExtraModelBuilder.reportModelBuilderFailure

/**
 * @author Vladislav.Soroka
 */
class WarModelBuilderImpl extends AbstractModelBuilderService {

  private static final String WEB_APP_DIR_PROPERTY = "webAppDir"
  private static final String WEB_APP_DIR_NAME_PROPERTY = "webAppDirName"
  private static is4OrBetter = GradleVersion.current().baseVersion >= GradleVersion.version("4.0")


  @Override
  boolean canBuild(String modelName) {
    return WebConfiguration.name == modelName
  }

  @Nullable
  @Override
  Object buildAll(String modelName, Project project, @NotNull ModelBuilderContext context) {
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
        catch (Exception e) {
          reportModelBuilderFailure(project, this, context, e)
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
