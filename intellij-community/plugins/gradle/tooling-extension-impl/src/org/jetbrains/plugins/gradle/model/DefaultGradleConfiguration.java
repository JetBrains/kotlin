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

/**
 * @author Vladislav.Soroka
 */
public class DefaultGradleConfiguration implements GradleConfiguration {
  private static final long serialVersionUID = 1L;
  private final String myName;
  private final String myDescription;
  private final boolean myVisible;
  private final boolean myScriptClasspathConfiguration;

  public DefaultGradleConfiguration(String name, String description, boolean visible) {
    this(name, description, visible, false);
  }

  public DefaultGradleConfiguration(@NotNull String name, @Nullable String description, boolean visible, boolean scriptClasspathConfiguration) {
    myName = name;
    myDescription = description;
    myVisible = visible;
    myScriptClasspathConfiguration = scriptClasspathConfiguration;
  }

  public DefaultGradleConfiguration(GradleConfiguration configuration) {
    this(configuration.getName(), configuration.getDescription(), configuration.isVisible(),
         configuration.isScriptClasspathConfiguration());
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }

  @Override
  public boolean isScriptClasspathConfiguration() {
    return myScriptClasspathConfiguration;
  }
}
