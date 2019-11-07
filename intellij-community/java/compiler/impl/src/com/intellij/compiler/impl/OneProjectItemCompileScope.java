// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.ExportableUserDataHolderBase;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OneProjectItemCompileScope extends ExportableUserDataHolderBase implements CompileScope{
  private static final Logger LOG = Logger.getInstance(OneProjectItemCompileScope.class);
  private final Project myProject;
  private final VirtualFile myFile;
  private final String myUrl;

  public OneProjectItemCompileScope(Project project, VirtualFile file) {
    myProject = project;
    myFile = file;
    final String url = file.getUrl();
    myUrl = file.isDirectory()? url + "/" : url;
  }

  @Override
  @NotNull
  public VirtualFile[] getFiles(final FileType fileType, final boolean inSourceOnly) {
    final List<VirtualFile> files = new ArrayList<>(1);
    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    final ContentIterator iterator = new CompilerContentIterator(fileType, projectFileIndex, inSourceOnly, files);
    if (myFile.isDirectory()){
      projectFileIndex.iterateContentUnderDirectory(myFile, iterator);
    }
    else{
      iterator.processFile(myFile);
    }
    return VfsUtilCore.toVirtualFileArray(files);
  }

  @Override
  public boolean belongs(@NotNull String url) {
    if (myFile.isDirectory()){
      return FileUtil.startsWith(url, myUrl);
    }
    return FileUtil.pathsEqual(url, myUrl);
  }

  @Override
  @NotNull
  public Module[] getAffectedModules() {
    final Module module = ModuleUtilCore.findModuleForFile(myFile, myProject);
    if (module == null) {
      LOG.error("Module is null for file " + myFile.getPresentableUrl());
      return Module.EMPTY_ARRAY;
    }
    return new Module[] {module};
  }

}
