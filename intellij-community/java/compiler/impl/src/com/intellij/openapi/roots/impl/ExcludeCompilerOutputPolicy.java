// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.util.ArrayUtilRt;
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
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
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
