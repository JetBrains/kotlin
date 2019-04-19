/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * @author yole
 */
public class ExcludeCompilerOutputPolicy implements DirectoryIndexExcludePolicy {
  private final Project myProject;

  public ExcludeCompilerOutputPolicy(final Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public String[] getExcludeUrlsForProject() {
    CompilerProjectExtension projectExtension = CompilerProjectExtension.getInstance(myProject);
    String outputPath = projectExtension == null ? null : projectExtension.getCompilerOutputUrl();
    if (outputPath != null) {
      return new String[] { outputPath };
    }
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @NotNull
  @Override
  public VirtualFilePointer[] getExcludeRootsForModule(@NotNull final ModuleRootModel rootModel) {
    ArrayList<VirtualFilePointer> result = new ArrayList<>();
    final CompilerModuleExtension extension = rootModel.getModuleExtension(CompilerModuleExtension.class);
    if (extension == null) {
      return VirtualFilePointer.EMPTY_ARRAY;
    }
    if (extension.isCompilerOutputPathInherited()) {
      CompilerProjectExtension projectExtension = CompilerProjectExtension.getInstance(myProject);
      if (projectExtension != null) {
        ContainerUtil.addIfNotNull(result, projectExtension.getCompilerOutputPointer());
      }
    }
    else {
      if (!extension.isExcludeOutput()) return VirtualFilePointer.EMPTY_ARRAY;
      ContainerUtil.addIfNotNull(result, extension.getCompilerOutputPointer());
      ContainerUtil.addIfNotNull(result, extension.getCompilerOutputForTestsPointer());
    }
    return result.isEmpty() ? VirtualFilePointer.EMPTY_ARRAY : result.toArray(VirtualFilePointer.EMPTY_ARRAY);
  }
}
