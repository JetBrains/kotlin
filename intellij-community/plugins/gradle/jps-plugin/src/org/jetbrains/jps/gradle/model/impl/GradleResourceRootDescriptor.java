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
package org.jetbrains.jps.gradle.model.impl;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.BuildRootDescriptor;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Vladislav.Soroka
 */
public class GradleResourceRootDescriptor extends BuildRootDescriptor {
  private final GradleResourcesTarget myTarget;
  private final ResourceRootConfiguration myConfig;
  private final File myFile;
  private final String myId;
  private final boolean myOverwrite;

  private final int myIndexInPom;

  public GradleResourceRootDescriptor(@NotNull GradleResourcesTarget target,
                                      ResourceRootConfiguration config,
                                      int indexInPom,
                                      boolean overwrite) {
    myTarget = target;
    myConfig = config;
    final String path = FileUtil.toCanonicalPath(config.directory);
    myFile = new File(path);
    myId = path;
    myIndexInPom = indexInPom;
    myOverwrite = overwrite;
  }

  public ResourceRootConfiguration getConfiguration() {
    return myConfig;
  }

  @Override
  public String getRootId() {
    return myId;
  }

  @Override
  public File getRootFile() {
    return myFile;
  }

  @Override
  public GradleResourcesTarget getTarget() {
    return myTarget;
  }

  @NotNull
  @Override
  public FileFilter createFileFilter() {
    return new GradleResourceFileFilter(myFile, myConfig);
  }

  @Override
  public boolean canUseFileCache() {
    return true;
  }

  public int getIndexInPom() {
    return myIndexInPom;
  }

  public boolean isOverwrite() {
    return myOverwrite;
  }
}
