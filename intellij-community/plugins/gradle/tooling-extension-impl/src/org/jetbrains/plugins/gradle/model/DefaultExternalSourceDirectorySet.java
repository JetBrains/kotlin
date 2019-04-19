/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import java.io.File;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class DefaultExternalSourceDirectorySet implements ExternalSourceDirectorySet {
  private static final long serialVersionUID = 1L;

  @NotNull
  private String myName;
  @NotNull
  private Set<File> mySrcDirs;
  private File myOutputDir;
  private final List<File> myGradleOutputDirs;
  private final FilePatternSet myPatterns;
  @NotNull
  private List<ExternalFilter> myFilters;

  private boolean myInheritedCompilerOutput;

  public DefaultExternalSourceDirectorySet() {
    mySrcDirs = new HashSet<File>();
    myFilters = new ArrayList<ExternalFilter>();
    myGradleOutputDirs = new ArrayList<File>();
    myPatterns = new FilePatternSetImpl(new LinkedHashSet<String>(), new LinkedHashSet<String>());
  }

  public DefaultExternalSourceDirectorySet(ExternalSourceDirectorySet sourceDirectorySet) {
    this();
    myName = sourceDirectorySet.getName();
    mySrcDirs = new HashSet<File>(sourceDirectorySet.getSrcDirs());
    myOutputDir = sourceDirectorySet.getOutputDir();
    myGradleOutputDirs.addAll(sourceDirectorySet.getGradleOutputDirs());

    myPatterns.getIncludes().addAll(sourceDirectorySet.getPatterns().getIncludes());
    myPatterns.getExcludes().addAll(sourceDirectorySet.getPatterns().getExcludes());
    for (ExternalFilter filter : sourceDirectorySet.getFilters()) {
      myFilters.add(new DefaultExternalFilter(filter));
    }
    myInheritedCompilerOutput = sourceDirectorySet.isCompilerOutputPathInherited();
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  public void setName(@NotNull String name) {
    myName = name;
  }

  @NotNull
  @Override
  public Set<File> getSrcDirs() {
    return mySrcDirs;
  }

  public void setSrcDirs(@NotNull Set<File> srcDirs) {
    mySrcDirs = srcDirs;
  }

  @NotNull
  @Override
  public File getOutputDir() {
    return myOutputDir;
  }

  public void setOutputDir(@NotNull File outputDir) {
    myOutputDir = outputDir;
  }

  @NotNull
  @Override
  public File getGradleOutputDir() {
    assert myGradleOutputDirs.size() > 0;
    return myGradleOutputDirs.get(0);
  }

  @NotNull
  @Override
  public Collection<File> getGradleOutputDirs() {
    return myGradleOutputDirs;
  }

  public void addGradleOutputDir(@NotNull File outputDir) {
    myGradleOutputDirs.add(outputDir);
  }

  @Override
  public boolean isCompilerOutputPathInherited() {
    return myInheritedCompilerOutput;
  }

  @NotNull
  @Override
  public Set<String> getExcludes() {
    return myPatterns.getExcludes();
  }

  public void setExcludes(Set<String> excludes) {
    myPatterns.getExcludes().clear();
    myPatterns.getExcludes().addAll(excludes);
  }

  @NotNull
  @Override
  public Set<String> getIncludes() {
    return myPatterns.getIncludes();
  }

  public void setIncludes(Set<String> includes) {
    myPatterns.getIncludes().clear();
    myPatterns.getIncludes().addAll(includes);
  }

  @NotNull
  @Override
  public FilePatternSet getPatterns() {
    return myPatterns;
  }

  public void setInheritedCompilerOutput(boolean inheritedCompilerOutput) {
    myInheritedCompilerOutput = inheritedCompilerOutput;
  }

  @NotNull
  @Override
  public List<ExternalFilter> getFilters() {
    return myFilters;
  }

  public void setFilters(@NotNull List<ExternalFilter> filters) {
    myFilters = filters;
  }
}
