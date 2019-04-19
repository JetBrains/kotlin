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
package com.intellij.compiler.impl;

import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.ExportableUserDataHolderBase;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OneProjectItemCompileScope extends ExportableUserDataHolderBase implements CompileScope{
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.impl.OneProjectItemCompileScope");
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
    return VfsUtil.toVirtualFileArray(files);
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
    final Module module = ModuleUtil.findModuleForFile(myFile, myProject);
    if (module == null) {
      LOG.error("Module is null for file " + myFile.getPresentableUrl());
      return Module.EMPTY_ARRAY;
    }
    return new Module[] {module};
  }

}
