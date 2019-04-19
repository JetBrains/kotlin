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
  private final List<GradleExtension> myExtensions;
  private final List<GradleConvention> myConventions;
  private final List<GradleProperty> myGradleProperties;
  private final List<ExternalTask> myTasks;
  private final List<GradleConfiguration> myConfigurations;
  private String myParentProjectPath;

  public DefaultGradleExtensions() {
    myExtensions = new ArrayList<GradleExtension>();
    myConventions = new ArrayList<GradleConvention>();
    myGradleProperties = new ArrayList<GradleProperty>();
    myTasks = new ArrayList<ExternalTask>();
    myConfigurations = new ArrayList<GradleConfiguration>();
  }

  public DefaultGradleExtensions(GradleExtensions extensions) {
    this();
    myParentProjectPath = extensions.getParentProjectPath();
    for (GradleExtension extension : extensions.getExtensions()) {
      myExtensions.add(new DefaultGradleExtension(extension));
    }
    for (GradleConvention convention : extensions.getConventions()) {
      myConventions.add(new DefaultGradleConvention(convention));
    }
    for (GradleProperty property : extensions.getGradleProperties()) {
      myGradleProperties.add(new DefaultGradleProperty(property));
    }
    for (ExternalTask entry : extensions.getTasks()) {
      myTasks.add(new DefaultExternalTask(entry));
    }
    for (GradleConfiguration entry : extensions.getConfigurations()) {
      myConfigurations.add(new DefaultGradleConfiguration(entry));
    }
  }

  @Nullable
  @Override
  public String getParentProjectPath() {
    return myParentProjectPath;
  }

  public void setParentProjectPath(String parentProjectPath) {
    myParentProjectPath = parentProjectPath;
  }

  @NotNull
  @Override
  public List<GradleExtension> getExtensions() {
    return myExtensions == null ? Collections.<GradleExtension>emptyList() : myExtensions;
  }

  @Override
  @NotNull
  public List<GradleConvention> getConventions() {
    return myConventions == null ? Collections.<GradleConvention>emptyList() : myConventions;
  }

  @NotNull
  @Override
  public List<GradleProperty> getGradleProperties() {
    return myGradleProperties == null ? Collections.<GradleProperty>emptyList() : myGradleProperties;
  }

  @NotNull
  @Override
  public List<ExternalTask> getTasks() {
    return myTasks == null ? Collections.<ExternalTask>emptyList() : myTasks;
  }

  @NotNull
  @Override
  public List<GradleConfiguration> getConfigurations() {
    return myConfigurations == null ? Collections.<GradleConfiguration>emptyList() : myConfigurations;
  }
}
