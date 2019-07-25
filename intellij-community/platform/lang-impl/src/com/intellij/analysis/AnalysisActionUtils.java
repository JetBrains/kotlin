/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.analysis;

import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

final public class AnalysisActionUtils {
  private static AnalysisScope getFileScopeFromInspectionView(DataContext dataContext) {
    InspectionResultsView inspectionView = dataContext.getData(InspectionResultsView.DATA_KEY);
    if (inspectionView != null) {
      AnalysisScope scope = inspectionView.getScope();
      int type = scope.getScopeType();
      if (type != AnalysisScope.MODULE && type != AnalysisScope.PROJECT && scope.isValid()) {
        return scope;
      }
    }
    return null;
  }

  @Nullable
  public static AnalysisScope getInspectionScope(@NotNull DataContext dataContext, @NotNull Project project, Boolean acceptNonProjectDirectories) {
    AnalysisScope scope = getFileScopeFromInspectionView(dataContext);
    if (scope != null) return scope;
    scope = getInspectionScopeImpl(dataContext, project, acceptNonProjectDirectories);
    return scope.getScopeType() != AnalysisScope.INVALID ? scope : null;
  }

  @NotNull
  private static AnalysisScope getInspectionScopeImpl(@NotNull DataContext dataContext, @NotNull Project project, Boolean acceptNonProjectDirectories) {
    // possible scopes: file, directory, package, project, module.
    Project projectContext = PlatformDataKeys.PROJECT_CONTEXT.getData(dataContext);
    if (projectContext != null) {
      return new AnalysisScope(projectContext);
    }

    AnalysisScope analysisScope = AnalysisScopeUtil.KEY.getData(dataContext);
    if (analysisScope != null) {
      return analysisScope;
    }

    PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
    if (psiFile != null && psiFile.getManager().isInProject(psiFile)) {
      VirtualFile file = psiFile.getVirtualFile();
      if (file != null && file.isValid() && file.getFileType() instanceof ArchiveFileType && acceptNonProjectDirectories) {
        VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(file);
        if (jarRoot != null) {
          PsiDirectory psiDirectory = psiFile.getManager().findDirectory(jarRoot);
          if (psiDirectory != null) {
            return new AnalysisScope(psiDirectory);
          }
        }
      }
      return new AnalysisScope(psiFile);
    }

    VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
    if (virtualFiles != null) {
      // analyze on selection
      ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      if (virtualFiles.length == 1) {
        PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFiles[0]);
        if (psiDirectory != null && (acceptNonProjectDirectories || psiDirectory.getManager().isInProject(psiDirectory))) {
          return new AnalysisScope(psiDirectory);
        }
      }
      Set<VirtualFile> files = new HashSet<>();
      for (VirtualFile vFile : virtualFiles) {
        if (fileIndex.isInContent(vFile)) {
          files.add(vFile);
        }
      }
      return new AnalysisScope(project, files);
    }

    Module moduleContext = LangDataKeys.MODULE_CONTEXT.getData(dataContext);
    if (moduleContext != null) {
      return new AnalysisScope(moduleContext);
    }

    Module[] modulesArray = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
    if (modulesArray != null) {
      return new AnalysisScope(modulesArray);
    }

    return new AnalysisScope(project);
  }
}
