// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.internal.impldep.aQute.lib.collections.SortedList;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.HierarchicalElement;
import org.gradle.tooling.model.idea.IdeaCompilerOutput;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.internal.ImmutableDomainObjectSet;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter.AdapterUtils.wrap;
import static org.jetbrains.plugins.gradle.tooling.util.GradleContainerUtil.emptyDomainObjectSet;

public class InternalIdeaModule implements IdeaModule {
  private String name;
  private String description;
  private InternalIdeaProject parent;
  private Set<InternalIdeaContentRoot> contentRoots = emptyDomainObjectSet();
  private List<InternalIdeaDependency> dependencies = new LinkedList<InternalIdeaDependency>();
  private InternalGradleProject gradleProject;
  private InternalIdeaCompilerOutput compilerOutput;
  private InternalIdeaJavaLanguageSettings javaLanguageSettings;
  private String jdkName;

  @Override
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaContentRoot> getContentRoots() {
    return wrap(contentRoots);
  }

  public void setContentRoots(List<InternalIdeaContentRoot> contentRoots) {
    this.contentRoots = ImmutableDomainObjectSet.of(contentRoots);
  }

  @Override
  public InternalIdeaProject getParent() {
    return this.parent;
  }

  @Override
  public InternalIdeaProject getProject() {
    return this.parent;
  }

  public void setParent(InternalIdeaProject parent) {
    this.parent = parent;
  }

  @Override
  public DomainObjectSet<InternalIdeaDependency> getDependencies() {
    return ImmutableDomainObjectSet.of(this.dependencies);
  }

  public void setDependencies(List<InternalIdeaDependency> dependencies) {
    this.dependencies = dependencies;
  }

  @Override
  public DomainObjectSet<? extends HierarchicalElement> getChildren() {
    return ImmutableDomainObjectSet.of(SortedList.<HierarchicalElement>empty());
  }

  @Override
  public InternalGradleProject getGradleProject() {
    return this.gradleProject;
  }

  public void setGradleProject(InternalGradleProject gradleProject) {
    this.gradleProject = gradleProject;
  }

  @Override
  public IdeaCompilerOutput getCompilerOutput() {
    return this.compilerOutput;
  }

  public void setCompilerOutput(InternalIdeaCompilerOutput compilerOutput) {
    this.compilerOutput = compilerOutput;
  }

  @Override
  public InternalIdeaJavaLanguageSettings getJavaLanguageSettings() {
    return this.javaLanguageSettings;
  }

  public void setJavaLanguageSettings(InternalIdeaJavaLanguageSettings javaLanguageSettings) {
    this.javaLanguageSettings = javaLanguageSettings;
  }

  @Override
  public String getJdkName() {
    return this.jdkName;
  }

  public void setJdkName(String jdkName) {
    this.jdkName = jdkName;
  }

  @Override
  public InternalProjectIdentifier getProjectIdentifier() {
    return this.gradleProject.getProjectIdentifier();
  }

  public String getProjectPath() {
    return this.getProjectIdentifier().getProjectPath();
  }

  public File getRootDir() {
    return this.getProjectIdentifier().getBuildIdentifier().getRootDir();
  }

  public String toString() {
    return "IdeaModule{name='" +
           this.name +
           '\'' +
           ", gradleProject='" +
           this.gradleProject +
           '\'' +
           ", contentRoots=" +
           this.contentRoots +
           ", compilerOutput=" +
           this.compilerOutput +
           ", dependencies count=" +
           this.dependencies.size() +
           '}';
  }
}
