/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.frameworkSupport;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class BuildScriptDataBuilder {
  @NotNull private final VirtualFile myBuildScriptFile;
  protected final Set<String> imports = ContainerUtil.newTreeSet();
  protected final Set<String> plugins = ContainerUtil.newTreeSet();
  protected final Set<String> pluginsInGroup = ContainerUtil.newTreeSet();
  protected final Set<String> repositories = ContainerUtil.newTreeSet();
  protected final Set<String> dependencies = ContainerUtil.newTreeSet();
  protected final Set<String> properties = ContainerUtil.newTreeSet();
  protected final Set<String> buildScriptProperties = ContainerUtil.newTreeSet();
  protected final Set<String> buildScriptRepositories = ContainerUtil.newTreeSet();
  protected final Set<String> buildScriptDependencies = ContainerUtil.newTreeSet();
  protected final Set<String> other = ContainerUtil.newTreeSet();
  protected final GradleVersion myGradleVersion;

  public BuildScriptDataBuilder(@NotNull VirtualFile buildScriptFile) {
    this(buildScriptFile, GradleVersion.current());
  }

  public BuildScriptDataBuilder(@NotNull VirtualFile buildScriptFile, @NotNull GradleVersion gradleVersion) {
    myBuildScriptFile = buildScriptFile;
    myGradleVersion = gradleVersion;
  }

  @NotNull
  public VirtualFile getBuildScriptFile() {
    return myBuildScriptFile;
  }

  @NotNull
  public GradleVersion getGradleVersion() {
    return myGradleVersion;
  }

  /**
   * @deprecated use {@link #buildMainPart()} and {@link #buildConfigurationPart()} instead
   */
  @Deprecated
  public String build() {
    return buildMainPart();
  }

  public String buildImports() {
    if (!imports.isEmpty()) {
      return StringUtil.join(imports, "\n") + "\n";
    }

    return "";
  }

  public String buildConfigurationPart() {
    List<String> lines = ContainerUtil.newArrayList();
    addBuildscriptLines(lines, BuildScriptDataBuilder::padding);
    if (!pluginsInGroup.isEmpty()) {
      lines.add("plugins {");
      lines.addAll(ContainerUtil.map(pluginsInGroup, BuildScriptDataBuilder::padding));
      lines.add("}");
      lines.add("");
    }
    return StringUtil.join(lines, "\n");
  }

  public String buildMainPart() {
    List<String> lines = ContainerUtil.newArrayList();
    addPluginsLines(lines, BuildScriptDataBuilder::padding);
    if (!properties.isEmpty()) {
      lines.addAll(properties);
      lines.add("");
    }
    if (!repositories.isEmpty()) {
      lines.add("repositories {");
      lines.addAll(ContainerUtil.map(repositories, BuildScriptDataBuilder::padding));
      lines.add("}");
      lines.add("");
    }
    if (!dependencies.isEmpty()) {
      lines.add("dependencies {");
      lines.addAll(ContainerUtil.map(dependencies, BuildScriptDataBuilder::padding));
      lines.add("}");
      lines.add("");
    }
    if (!other.isEmpty()) {
      lines.addAll(other);
    }
    return StringUtil.join(lines, "\n");
  }

  protected void addPluginsLines(@NotNull List<String> lines, @NotNull Function<String, String> padding) {
    if (!plugins.isEmpty()) {
      lines.addAll(plugins);
      lines.add("");
    }
  }

  private void addBuildscriptLines(@NotNull List<String> lines, @NotNull Function<String, String> padding) {
    if (!buildScriptRepositories.isEmpty() || !buildScriptDependencies.isEmpty() || !buildScriptProperties.isEmpty()) {
      lines.add("buildscript {");
      final List<String> buildScriptLines = ContainerUtil.newSmartList();
      if (!buildScriptProperties.isEmpty()) {
        buildScriptLines.addAll(buildScriptProperties);
        buildScriptLines.add("");
      }
      if (!buildScriptRepositories.isEmpty()) {
        buildScriptLines.add("repositories {");
        buildScriptLines.addAll(ContainerUtil.map(buildScriptRepositories, padding));
        buildScriptLines.add("}");
      }
      if (!buildScriptDependencies.isEmpty()) {
        buildScriptLines.add("dependencies {");
        buildScriptLines.addAll(ContainerUtil.map(buildScriptDependencies, padding));
        buildScriptLines.add("}");
      }
      lines.addAll(ContainerUtil.map(buildScriptLines, padding));
      lines.add("}");
      lines.add("");
    }
  }

  public BuildScriptDataBuilder addImport(@NotNull String importString) {
    imports.add(importString);
    return this;
  }

  public BuildScriptDataBuilder addBuildscriptPropertyDefinition(@NotNull String definition) {
    buildScriptProperties.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addBuildscriptRepositoriesDefinition(@NotNull String definition) {
    buildScriptRepositories.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addBuildscriptDependencyNotation(@NotNull String notation) {
    buildScriptDependencies.add(notation.trim());
    return this;
  }

  public BuildScriptDataBuilder addPluginDefinitionInPluginsGroup(@NotNull String definition) {
    pluginsInGroup.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addPluginDefinition(@NotNull String definition) {
    plugins.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addRepositoriesDefinition(@NotNull String definition) {
    repositories.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addDependencyNotation(@NotNull String notation) {
    dependencies.add(notation.trim());
    return this;
  }

  public BuildScriptDataBuilder addPropertyDefinition(@NotNull String definition) {
    properties.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addOther(@NotNull String definition) {
    other.add(definition.trim());
    return this;
  }

  private static String padding(String s) {return StringUtil.isNotEmpty(s) ? "    " + s : "";}
}
