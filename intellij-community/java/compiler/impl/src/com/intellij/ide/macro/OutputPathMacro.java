// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.macro;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class OutputPathMacro extends Macro {
  @NotNull
  @Override
  public String getName() {
    return "OutputPath";
  }

  @NotNull
  @Override
  public String getDescription() {
    return IdeBundle.message("macro.output.path");
  }

  @Override
  public String expand(@NotNull DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return null;
    }

    VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
    if (file != null) {
      ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      Module module = projectFileIndex.getModuleForFile(file);
      if (module != null){
        boolean isTest = projectFileIndex.isInTestSourceContent(file);
        String outputPathUrl = isTest ? CompilerModuleExtension.getInstance(module).getCompilerOutputUrlForTests()
                               : CompilerModuleExtension.getInstance(module).getCompilerOutputUrl();
        if (outputPathUrl == null) return null;
        return VirtualFileManager.extractPath(outputPathUrl).replace('/', File.separatorChar);
      }
    }

    Module[] allModules = ModuleManager.getInstance(project).getSortedModules();
    if (allModules.length == 0) {
      return null;
    }
    String[] paths = CompilerPaths.getOutputPaths(allModules);
    final StringBuilder outputPath = new StringBuilder();
    for (int idx = 0; idx < paths.length; idx++) {
      String path = paths[idx];
      if (idx > 0) {
        outputPath.append(File.pathSeparator);
      }
      outputPath.append(path);
    }
    return outputPath.toString();
  }
}
