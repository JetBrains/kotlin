// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public final class DefaultExternalSourceDirectorySet implements ExternalSourceDirectorySet {
  private static final long serialVersionUID = 1L;

  @NotNull
  private String name;
  @NotNull
  private Set<File> srcDirs;
  private File outputDir;
  private final List<File> gradleOutputDirs;
  private final FilePatternSetImpl patterns;
  @NotNull
  private List<DefaultExternalFilter> filters;

  private boolean inheritedCompilerOutput;

  public DefaultExternalSourceDirectorySet() {
    srcDirs = new HashSet<File>();
    filters = new ArrayList<DefaultExternalFilter>();
    gradleOutputDirs = new ArrayList<File>();
    patterns = new FilePatternSetImpl(new LinkedHashSet<String>(), new LinkedHashSet<String>());
  }

  public DefaultExternalSourceDirectorySet(ExternalSourceDirectorySet sourceDirectorySet) {
    this();
    name = sourceDirectorySet.getName();
    srcDirs = new HashSet<File>(sourceDirectorySet.getSrcDirs());
    outputDir = sourceDirectorySet.getOutputDir();
    gradleOutputDirs.addAll(sourceDirectorySet.getGradleOutputDirs());

    patterns.getIncludes().addAll(sourceDirectorySet.getPatterns().getIncludes());
    patterns.getExcludes().addAll(sourceDirectorySet.getPatterns().getExcludes());
    for (ExternalFilter filter : sourceDirectorySet.getFilters()) {
      filters.add(new DefaultExternalFilter(filter));
    }
    inheritedCompilerOutput = sourceDirectorySet.isCompilerOutputPathInherited();
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  @NotNull
  @Override
  public Set<File> getSrcDirs() {
    return srcDirs;
  }

  public void setSrcDirs(@NotNull Set<File> srcDirs) {
    this.srcDirs = srcDirs;
  }

  @NotNull
  @Override
  public File getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(@NotNull File outputDir) {
    this.outputDir = outputDir;
  }

  @NotNull
  @Override
  public File getGradleOutputDir() {
    assert gradleOutputDirs.size() > 0;
    return gradleOutputDirs.get(0);
  }

  @NotNull
  @Override
  public Collection<File> getGradleOutputDirs() {
    return gradleOutputDirs;
  }

  public void addGradleOutputDir(@NotNull File outputDir) {
    gradleOutputDirs.add(outputDir);
  }

  @Override
  public boolean isCompilerOutputPathInherited() {
    return inheritedCompilerOutput;
  }

  @NotNull
  @Override
  public Set<String> getExcludes() {
    return patterns.getExcludes();
  }

  public void setExcludes(Set<String> excludes) {
    patterns.getExcludes().clear();
    patterns.getExcludes().addAll(excludes);
  }

  @NotNull
  @Override
  public Set<String> getIncludes() {
    return patterns.getIncludes();
  }

  public void setIncludes(Set<String> includes) {
    patterns.getIncludes().clear();
    patterns.getIncludes().addAll(includes);
  }

  @NotNull
  @Override
  public FilePatternSet getPatterns() {
    return patterns;
  }

  public void setInheritedCompilerOutput(boolean inheritedCompilerOutput) {
    this.inheritedCompilerOutput = inheritedCompilerOutput;
  }

  @NotNull
  @Override
  public List<? extends ExternalFilter> getFilters() {
    return filters;
  }

  public void setFilters(@NotNull List<DefaultExternalFilter> filters) {
    this.filters = filters;
  }
}
