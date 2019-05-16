// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class DefaultGradleExtensions implements GradleExtensions {
  private static final long serialVersionUID = 1L;

  private final List<GradleExtension> extensions;
  private final List<GradleConvention> conventions;
  private final List<GradleProperty> gradleProperties;
  private final List<ExternalTask> tasks;
  private final List<GradleConfiguration> configurations;
  private String parentProjectPath;

  public DefaultGradleExtensions() {
    extensions = new ArrayList<GradleExtension>();
    conventions = new ArrayList<GradleConvention>();
    gradleProperties = new ArrayList<GradleProperty>();
    tasks = new ArrayList<ExternalTask>();
    configurations = new ArrayList<GradleConfiguration>();
  }

  public DefaultGradleExtensions(@NotNull GradleExtensions extensions) {
    parentProjectPath = extensions.getParentProjectPath();

    this.extensions = new ArrayList<GradleExtension>(extensions.getExtensions().size());
    for (GradleExtension extension : extensions.getExtensions()) {
      this.extensions.add(new DefaultGradleExtension(extension));
    }

    conventions = new ArrayList<GradleConvention>(extensions.getConventions().size());
    for (GradleConvention convention : extensions.getConventions()) {
      conventions.add(new DefaultGradleConvention(convention));
    }

    gradleProperties = new ArrayList<GradleProperty>(extensions.getGradleProperties().size());
    for (GradleProperty property : extensions.getGradleProperties()) {
      gradleProperties.add(new DefaultGradleProperty(property));
    }

    tasks = new ArrayList<ExternalTask>(extensions.getTasks().size());
    for (ExternalTask entry : extensions.getTasks()) {
      tasks.add(new DefaultExternalTask(entry));
    }

    configurations = new ArrayList<GradleConfiguration>(extensions.getConfigurations().size());
    for (GradleConfiguration entry : extensions.getConfigurations()) {
      configurations.add(new DefaultGradleConfiguration(entry));
    }
  }

  @Nullable
  @Override
  public String getParentProjectPath() {
    return parentProjectPath;
  }

  public void setParentProjectPath(String parentProjectPath) {
    this.parentProjectPath = parentProjectPath;
  }

  @NotNull
  @Override
  public List<GradleExtension> getExtensions() {
    return extensions == null ? Collections.<GradleExtension>emptyList() : extensions;
  }

  @Override
  @NotNull
  public List<GradleConvention> getConventions() {
    return conventions == null ? Collections.<GradleConvention>emptyList() : conventions;
  }

  @NotNull
  @Override
  public List<GradleProperty> getGradleProperties() {
    return gradleProperties == null ? Collections.<GradleProperty>emptyList() : gradleProperties;
  }

  @NotNull
  @Override
  public List<ExternalTask> getTasks() {
    return tasks == null ? Collections.<ExternalTask>emptyList() : tasks;
  }

  @NotNull
  @Override
  public List<GradleConfiguration> getConfigurations() {
    return configurations == null ? Collections.<GradleConfiguration>emptyList() : configurations;
  }
}
