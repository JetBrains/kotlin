// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import com.google.gson.Gson;
import com.intellij.compiler.server.BuildProcessParametersProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import groovy.lang.GroovyObject;
import org.apache.tools.ant.taskdefs.Ant;
import org.gradle.internal.impldep.com.google.common.base.Optional;
import org.gradle.tooling.ProjectConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.slf4j.Logger;
import org.slf4j.impl.Log4jLoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds Gradle build dependencies to the project build process' classpath.
 *
 * @author Vladislav.Soroka
 */
public class GradleBuildProcessParametersProvider extends BuildProcessParametersProvider {
  @NotNull private final Project myProject;

  private List<String> myGradleClasspath;

  public GradleBuildProcessParametersProvider(@NotNull Project project) {
    myProject = project;
  }

  @Override
  @NotNull
  public List<String> getClassPath() {
    List<String> result = new ArrayList<>();
    if (!GradleSettings.getInstance(myProject).getLinkedProjectsSettings().isEmpty()) {
      addGradleClassPath(result);
      addOtherClassPath(result);
    }
    return result;
  }

  private void addGradleClassPath(@NotNull final List<String> classpath) {
    if (myGradleClasspath == null) {
      myGradleClasspath = new ArrayList<>();
      String gradleToolingApiJarPath = PathUtil.getJarPathForClass(ProjectConnection.class);
      if (!StringUtil.isEmpty(gradleToolingApiJarPath)) {
        myGradleClasspath.add(gradleToolingApiJarPath);
      }
      String gradleToolingApiImplDepJarPath = PathUtil.getJarPathForClass(Optional.class);
      if (!StringUtil.isEmpty(gradleToolingApiImplDepJarPath)) {
        myGradleClasspath.add(gradleToolingApiImplDepJarPath);
      }
    }
    classpath.addAll(myGradleClasspath);
  }

  private static void addOtherClassPath(@NotNull final List<String> classpath) {
    classpath.add(PathUtil.getJarPathForClass(Ant.class));
    classpath.add(PathUtil.getJarPathForClass(GroovyObject.class));
    classpath.add(PathUtil.getJarPathForClass(Gson.class));
    classpath.add(PathUtil.getJarPathForClass(Logger.class));
    classpath.add(PathUtil.getJarPathForClass(Log4jLoggerFactory.class));
  }
}
