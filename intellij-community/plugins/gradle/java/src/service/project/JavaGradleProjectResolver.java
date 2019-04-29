// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.externalSystem.JavaProjectData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.BuildScriptClasspathModel;
import org.jetbrains.plugins.gradle.model.ClasspathEntryModel;
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionErrorHandler;
import org.jetbrains.plugins.gradle.service.notification.ApplyGradlePluginCallback;
import org.jetbrains.plugins.gradle.service.notification.GotoSourceNotificationCallback;
import org.jetbrains.plugins.gradle.service.notification.OpenGradleSettingsCallback;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public class JavaGradleProjectResolver extends AbstractProjectResolverExtension {
  @Override
  public void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject) {
    // import java project data

    final String projectDirPath = resolverCtx.getProjectPath();
    final IdeaProject ideaProject = resolverCtx.getModels().getIdeaProject();

    // Gradle API doesn't expose gradleProject compile output path yet.
    JavaProjectData javaProjectData = new JavaProjectData(GradleConstants.SYSTEM_ID, projectDirPath + "/build/classes");
    javaProjectData.setJdkVersion(ideaProject.getJdkName());
    LanguageLevel resolvedLanguageLevel = null;
    // org.gradle.tooling.model.idea.IdeaLanguageLevel.getLevel() returns something like JDK_1_6
    final String languageLevel = ideaProject.getLanguageLevel().getLevel();
    for (LanguageLevel level : LanguageLevel.values()) {
      if (level.name().equals(languageLevel)) {
        resolvedLanguageLevel = level;
        break;
      }
    }
    if (resolvedLanguageLevel != null) {
      javaProjectData.setLanguageLevel(resolvedLanguageLevel);
    }
    else {
      javaProjectData.setLanguageLevel(languageLevel);
    }

    ideProject.createChild(JavaProjectData.KEY, javaProjectData);

    nextResolver.populateProjectExtraModels(gradleProject, ideProject);
  }

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    final BuildScriptClasspathModel buildScriptClasspathModel = resolverCtx.getExtraProject(gradleModule, BuildScriptClasspathModel.class);
    final List<BuildScriptClasspathData.ClasspathEntry> classpathEntries;
    if (buildScriptClasspathModel != null) {
      classpathEntries = ContainerUtil.map(
        buildScriptClasspathModel.getClasspath(),
        (Function<ClasspathEntryModel, BuildScriptClasspathData.ClasspathEntry>)model -> new BuildScriptClasspathData.ClasspathEntry(model.getClasses(), model.getSources(), model.getJavadoc()));
    }
    else {
      classpathEntries = ContainerUtil.emptyList();
    }
    BuildScriptClasspathData buildScriptClasspathData = new BuildScriptClasspathData(GradleConstants.SYSTEM_ID, classpathEntries);
    buildScriptClasspathData.setGradleHomeDir(buildScriptClasspathModel != null ? buildScriptClasspathModel.getGradleHomeDir() : null);
    ideModule.createChild(BuildScriptClasspathData.KEY, buildScriptClasspathData);

    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  @NotNull
  @Override
  public ExternalSystemException getUserFriendlyError(@NotNull Throwable error,
                                                      @NotNull String projectPath,
                                                      @Nullable String buildFilePath) {
    ExternalSystemException friendlyError = new JavaProjectImportErrorHandler().getUserFriendlyError(error, projectPath, buildFilePath);
    if (friendlyError != null) {
      return friendlyError;
    }
    return super.getUserFriendlyError(error, projectPath, buildFilePath);
  }

  private static class JavaProjectImportErrorHandler extends AbstractProjectImportErrorHandler {
    @Nullable
    @Override
    public ExternalSystemException getUserFriendlyError(@NotNull Throwable error,
                                                        @NotNull String projectPath,
                                                        @Nullable String buildFilePath) {
      GradleExecutionErrorHandler executionErrorHandler = new GradleExecutionErrorHandler(error, projectPath, buildFilePath);
      ExternalSystemException friendlyError = executionErrorHandler.getUserFriendlyError();
      if (friendlyError != null) {
        return friendlyError;
      }

      Throwable rootCause = executionErrorHandler.getRootCause();
      String location = executionErrorHandler.getLocation();
      if (location == null && !StringUtil.isEmpty(buildFilePath)) {
        location = String.format("Build file: '%1$s'", buildFilePath);
      }

      final String rootCauseText = rootCause.toString();
      if (StringUtil.startsWith(rootCauseText, "org.gradle.api.internal.MissingMethodException")) {
        String method = parseMissingMethod(rootCauseText);
        String msg = "Build script error, unsupported Gradle DSL method found: '" + method + "'!";
        msg += (EMPTY_LINE + "Possible causes could be:  ");
        msg += String.format(
          "%s  - you are using Gradle version where the method is absent (<a href=\"%s\">Fix Gradle settings</a>)",
          '\n', OpenGradleSettingsCallback.ID);
        msg += String.format(
          "%s  - you didn't apply Gradle plugin which provides the method (<a href=\"%s\">Apply Gradle plugin</a>)",
          '\n', ApplyGradlePluginCallback.ID);
        msg += String.format(
          "%s  - or there is a mistake in a build script (<a href=\"%s\">Goto source</a>)",
          '\n', GotoSourceNotificationCallback.ID);
        return createUserFriendlyError(
          msg, location, OpenGradleSettingsCallback.ID, ApplyGradlePluginCallback.ID, GotoSourceNotificationCallback.ID);
      }

      return null;
    }
  }
}
