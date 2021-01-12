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
package com.intellij.task.impl;

import com.intellij.lang.LangBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.task.ModuleFilesBuildTask;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class ModuleFilesBuildTaskImpl extends ModuleBuildTaskImpl implements ModuleFilesBuildTask {
  private final VirtualFile[] myFiles;

  public ModuleFilesBuildTaskImpl(Module module, boolean isIncrementalBuild, VirtualFile... files) {
    super(module, isIncrementalBuild);
    myFiles = files;
  }

  public ModuleFilesBuildTaskImpl(Module module, boolean isIncrementalBuild, Collection<? extends VirtualFile> files) {
    this(module, isIncrementalBuild, files.toArray(VirtualFile.EMPTY_ARRAY));
  }

  @Override
  public VirtualFile[] getFiles() {
    return myFiles;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return LangBundle.message("project.task.name.files.build.task.0", Arrays.toString(myFiles));
  }
}
