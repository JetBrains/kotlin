// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.internal.ImmutableDomainObjectSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.util.GradleVersionComparator;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.jetbrains.plugins.gradle.tooling.Exceptions.unsupportedMethod;
import static org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter.AdapterUtils.wrap;
import static org.jetbrains.plugins.gradle.tooling.util.GradleContainerUtil.emptyDomainObjectSet;

public class InternalIdeaContentRoot implements IdeaContentRoot {
  private File rootDirectory;
  private Set<InternalIdeaSourceDirectory> sourceDirectories = emptyDomainObjectSet();
  private Set<InternalIdeaSourceDirectory> testDirectories = emptyDomainObjectSet();
  private Set<InternalIdeaSourceDirectory> resourceDirectories = emptyDomainObjectSet();
  private Set<InternalIdeaSourceDirectory> testResourceDirectories = emptyDomainObjectSet();
  private Set<File> excludeDirectories = new LinkedHashSet<File>();

  private final GradleVersionComparator myGradleVersionComparator;

  public InternalIdeaContentRoot(@NotNull GradleVersionComparator gradleVersionComparator) {
    myGradleVersionComparator = gradleVersionComparator;
  }

  @Override
  public File getRootDirectory() {
    return this.rootDirectory;
  }

  public void setRootDirectory(File rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaSourceDirectory> getSourceDirectories() {
    return wrap(sourceDirectories);
  }

  public void setSourceDirectories(Set<InternalIdeaSourceDirectory> sourceDirectories) {
    this.sourceDirectories = ImmutableDomainObjectSet.of(sourceDirectories);
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaSourceDirectory> getGeneratedSourceDirectories() {
    return ImmutableDomainObjectSet.of(generated(this.sourceDirectories));
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaSourceDirectory> getTestDirectories() {
    return wrap(testDirectories);
  }

  public void setTestDirectories(Set<InternalIdeaSourceDirectory> testDirectories) {
    this.testDirectories = ImmutableDomainObjectSet.of(testDirectories);
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaSourceDirectory> getGeneratedTestDirectories() {
    return ImmutableDomainObjectSet.of(generated(this.testDirectories));
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaSourceDirectory> getResourceDirectories() {
    if (myGradleVersionComparator.lessThan("4.7")) {
      throw unsupportedMethod("IdeaContentRoot.getResourceDirectories()");
    }
    return wrap(resourceDirectories);
  }

  public void setResourceDirectories(Set<InternalIdeaSourceDirectory> resourceDirectories) {
    this.resourceDirectories = ImmutableDomainObjectSet.of(resourceDirectories);
  }

  @Override
  public ImmutableDomainObjectSet<InternalIdeaSourceDirectory> getTestResourceDirectories() {
    if (myGradleVersionComparator.lessThan("4.7")) {
      throw unsupportedMethod("IdeaContentRoot.getTestResourceDirectories()");
    }
    return wrap(testResourceDirectories);
  }

  public void setTestResourceDirectories(Set<InternalIdeaSourceDirectory> testResourceDirectories) {
    this.testResourceDirectories = ImmutableDomainObjectSet.of(testResourceDirectories);
  }

  @Override
  public Set<File> getExcludeDirectories() {
    return this.excludeDirectories;
  }

  public void setExcludeDirectories(Set<File> excludeDirectories) {
    this.excludeDirectories = excludeDirectories;
  }

  private static Set<InternalIdeaSourceDirectory> generated(Set<InternalIdeaSourceDirectory> directories) {
    Set<InternalIdeaSourceDirectory> generated = new LinkedHashSet<InternalIdeaSourceDirectory>();
    for (InternalIdeaSourceDirectory sourceDirectory : directories) {
      if (sourceDirectory.isGenerated()) {
        generated.add(sourceDirectory);
      }
    }

    return generated;
  }

  public String toString() {
    return String.format(
      "IdeaContentRoot{rootDirectory=%s, sourceDirectories count=%d, testDirectories count=%d, resourceDirectories count=%d, testResourceDirectories count=%d, excludeDirectories count=%d}",
      this.rootDirectory, this.sourceDirectories.size(), this.testDirectories.size(),
      this.resourceDirectories.size(), this.testResourceDirectories.size(), this.excludeDirectories.size());
  }
}
