// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.HierarchicalElement;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.internal.ImmutableDomainObjectSet;

import java.util.List;
import java.util.Set;

import static org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter.AdapterUtils.wrap;
import static org.jetbrains.plugins.gradle.tooling.util.GradleContainerUtil.emptyDomainObjectSet;

public class InternalIdeaProject implements IdeaProject {
  private String name;
  private String description;
  private Set<InternalIdeaModule> children = emptyDomainObjectSet();
  private InternalIdeaLanguageLevel languageLevel;
  private String jdkName;
  private InternalIdeaJavaLanguageSettings javaLanguageSettings;

  @Override
  public InternalIdeaLanguageLevel getLanguageLevel() {
    return this.languageLevel;
  }

  public void setLanguageLevel(InternalIdeaLanguageLevel languageLevel) {
    this.languageLevel = languageLevel;
  }

  @Override
  public String getJdkName() {
    return this.jdkName;
  }

  public void setJdkName(String jdkName) {
    this.jdkName = jdkName;
  }

  @Override
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public HierarchicalElement getParent() {
    return null;
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaModule> getChildren() {
    return getModules();
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaModule> getModules() {
    return wrap(children);
  }

  public void setModules(List<InternalIdeaModule> modules) {
    children = ImmutableDomainObjectSet.of(modules);
  }

  @Override
  public InternalIdeaJavaLanguageSettings getJavaLanguageSettings() {
    return this.javaLanguageSettings;
  }

  public void setJavaLanguageSettings(InternalIdeaJavaLanguageSettings javaLanguageSettings) {
    this.javaLanguageSettings = javaLanguageSettings;
  }

  public String toString() {
    return String.format("IdeaProject{ name='%s', description='%s', children count=%d, languageLevel='%s', jdkName='%s'}",
                         this.name, this.description, this.children.size(), this.languageLevel, this.jdkName);
  }
}
