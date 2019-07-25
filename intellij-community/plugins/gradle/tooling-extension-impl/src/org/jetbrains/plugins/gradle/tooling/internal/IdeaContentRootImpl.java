/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.tooling.internal;

import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.gradle.tooling.model.internal.ImmutableDomainObjectSet;
import org.jetbrains.plugins.gradle.model.ExtIdeaContentRoot;

import java.io.File;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class IdeaContentRootImpl implements ExtIdeaContentRoot {

  private final File myRootDirectory;
  private final List<IdeaSourceDirectory> mySourceDirectories;
  private final List<IdeaSourceDirectory> myTestDirectories;
  private final List<IdeaSourceDirectory> myResourceDirectories;
  private final List<IdeaSourceDirectory> myTestResourceDirectories;
  private final Set<File> myExcludeDirectories;

  public IdeaContentRootImpl(File rootDirectory) {
    myRootDirectory = rootDirectory;
    mySourceDirectories = new ArrayList<IdeaSourceDirectory>();
    myTestDirectories = new ArrayList<IdeaSourceDirectory>();
    myResourceDirectories = new ArrayList<IdeaSourceDirectory>();
    myTestResourceDirectories = new ArrayList<IdeaSourceDirectory>();
    myExcludeDirectories = new HashSet<File>();
  }

  @Override
  public File getRootDirectory() {
    return myRootDirectory;
  }

  @Override
  public DomainObjectSet<? extends IdeaSourceDirectory> getSourceDirectories() {
    return ImmutableDomainObjectSet.of(mySourceDirectories);
  }

  public void addSourceDirectory(IdeaSourceDirectory sourceDirectory) {
    mySourceDirectories.add(sourceDirectory);
  }

  @Override
  public DomainObjectSet<? extends IdeaSourceDirectory> getGeneratedSourceDirectories() {
    List<IdeaSourceDirectory> generatedSourceDirectories = new ArrayList<IdeaSourceDirectory>();
    for (IdeaSourceDirectory sourceDirectory : mySourceDirectories) {
      if(sourceDirectory.isGenerated()) {
        generatedSourceDirectories.add(sourceDirectory);
      }
    }
    return ImmutableDomainObjectSet.of(generatedSourceDirectories);
  }

  public void addTestDirectory(IdeaSourceDirectory testDirectory) {
    myTestDirectories.add(testDirectory);
  }

  public void addResourceDirectory(IdeaSourceDirectory resourceDirectory) {
    myResourceDirectories.add(resourceDirectory);
  }

  public void addTestResourceDirectory(IdeaSourceDirectory resourceDirectory) {
    myTestResourceDirectories.add(resourceDirectory);
  }

  public void addExcludeDirectory(File excludeDirectory) {
    myExcludeDirectories.add(excludeDirectory);
  }

  @Override
  public DomainObjectSet<? extends IdeaSourceDirectory> getTestDirectories() {
    return ImmutableDomainObjectSet.of(myTestDirectories);
  }

  @Override
  public DomainObjectSet<? extends IdeaSourceDirectory> getGeneratedTestDirectories() {
    List<IdeaSourceDirectory> generatedTestDirectories = new ArrayList<IdeaSourceDirectory>();
    for (IdeaSourceDirectory sourceDirectory : myTestDirectories) {
      if(sourceDirectory.isGenerated()) {
        generatedTestDirectories.add(sourceDirectory);
      }
    }
    return ImmutableDomainObjectSet.of(generatedTestDirectories);
  }

  @Override
  public DomainObjectSet<? extends IdeaSourceDirectory> getResourceDirectories() {
    return ImmutableDomainObjectSet.of(myResourceDirectories);
  }

  @Override
  public DomainObjectSet<? extends IdeaSourceDirectory> getTestResourceDirectories() {
    return ImmutableDomainObjectSet.of(myTestResourceDirectories);
  }

  @Override
  public Set<File> getExcludeDirectories() {
    return myExcludeDirectories;
  }
}
