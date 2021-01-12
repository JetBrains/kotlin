// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class DefaultGradleExtensions implements GradleExtensions {
  private static final long serialVersionUID = 1L;

  private final List<DefaultGradleExtension> extensions;
  private final List<DefaultGradleConvention> conventions;
  private final List<DefaultGradleProperty> gradleProperties;
  private final ArrayList<DefaultExternalTask> tasks;
  private final List<DefaultGradleConfiguration> configurations;
  private String parentProjectPath;

  public DefaultGradleExtensions() {
    extensions = new ArrayList<DefaultGradleExtension>(0);
    conventions = new ArrayList<DefaultGradleConvention>(0);
    gradleProperties = new ArrayList<DefaultGradleProperty>(0);
    tasks = new ArrayList<DefaultExternalTask>(0);
    configurations = new ArrayList<DefaultGradleConfiguration>(0);
  }

  public DefaultGradleExtensions(@NotNull GradleExtensions extensions) {
    parentProjectPath = extensions.getParentProjectPath();

    this.extensions = new ArrayList<DefaultGradleExtension>(extensions.getExtensions().size());
    for (GradleExtension extension : extensions.getExtensions()) {
      this.extensions.add(new DefaultGradleExtension(extension));
    }

    conventions = new ArrayList<DefaultGradleConvention>(extensions.getConventions().size());
    for (GradleConvention convention : extensions.getConventions()) {
      conventions.add(new DefaultGradleConvention(convention));
    }

    gradleProperties = new ArrayList<DefaultGradleProperty>(extensions.getGradleProperties().size());
    for (GradleProperty property : extensions.getGradleProperties()) {
      gradleProperties.add(new DefaultGradleProperty(property));
    }

    tasks = new ArrayList<DefaultExternalTask>(extensions.getTasks().size());
    for (ExternalTask entry : extensions.getTasks()) {
      tasks.add(new DefaultExternalTask(entry));
    }

    configurations = new ArrayList<DefaultGradleConfiguration>(extensions.getConfigurations().size());
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
  public List<DefaultGradleExtension> getExtensions() {
    return extensions == null ? Collections.<DefaultGradleExtension>emptyList() : extensions;
  }

  @Override
  @NotNull
  public List<DefaultGradleConvention> getConventions() {
    return conventions == null ? Collections.<DefaultGradleConvention>emptyList() : conventions;
  }

  @NotNull
  @Override
  public List<DefaultGradleProperty> getGradleProperties() {
    return gradleProperties == null ? Collections.<DefaultGradleProperty>emptyList() : gradleProperties;
  }

  @NotNull
  @Override
  public List<DefaultExternalTask> getTasks() {
    return tasks;
  }

  public void addTasks(@NotNull Collection<? extends ExternalTask> values) {
    tasks.ensureCapacity(tasks.size() + values.size());
    for (ExternalTask value : values) {
      if (value instanceof DefaultExternalTask) {
        tasks.add((DefaultExternalTask)value);
      }
      else {
        tasks.add(new DefaultExternalTask(value));
      }
    }
  }

  @NotNull
  @Override
  public List<DefaultGradleConfiguration> getConfigurations() {
    return configurations == null ? Collections.<DefaultGradleConfiguration>emptyList() : configurations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultGradleExtensions that = (DefaultGradleExtensions)o;

    if (extensions != null ? !extensions.equals(that.extensions) : that.extensions != null) return false;
    if (conventions != null ? !conventions.equals(that.conventions) : that.conventions != null) return false;
    if (gradleProperties != null ? !gradleProperties.equals(that.gradleProperties) : that.gradleProperties != null) return false;
    if (tasks != null ? !tasks.equals(that.tasks) : that.tasks != null) return false;
    if (configurations != null ? !configurations.equals(that.configurations) : that.configurations != null) return false;
    if (parentProjectPath != null ? !parentProjectPath.equals(that.parentProjectPath) : that.parentProjectPath != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = extensions != null ? extensions.hashCode() : 0;
    result = 31 * result + (conventions != null ? conventions.hashCode() : 0);
    result = 31 * result + (gradleProperties != null ? gradleProperties.hashCode() : 0);
    result = 31 * result + (tasks != null ? tasks.hashCode() : 0);
    result = 31 * result + (configurations != null ? configurations.hashCode() : 0);
    result = 31 * result + (parentProjectPath != null ? parentProjectPath.hashCode() : 0);
    return result;
  }
}
