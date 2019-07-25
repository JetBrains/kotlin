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
package com.intellij.compiler;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.IconLayerProvider;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public class CompilerIconLayerProvider implements IconLayerProvider {
  @Override
  public Icon getLayerIcon(@NotNull Iconable element, boolean isLocked) {
    VirtualFile vFile = null;
    Project project = null;
    if (element instanceof PsiModifierListOwner) {
      project = ((PsiModifierListOwner) element).getProject();
      final PsiFile containingFile = ((PsiModifierListOwner) element).getContainingFile();
      vFile = containingFile == null ? null : containingFile.getVirtualFile();
    }
    else if (element instanceof PsiDirectory) {
      project = ((PsiDirectory) element).getProject();
      vFile = ((PsiDirectory) element).getVirtualFile();
    }
    if (vFile != null && isExcluded(vFile, project)) {
      return PlatformIcons.EXCLUDED_FROM_COMPILE_ICON;
    }
    return null;
  }

  @NotNull
  @Override
  public String getLayerDescription() {
    return CodeInsightBundle.message("node.excluded.flag.tooltip");
  }

  public static boolean isExcluded(final VirtualFile vFile, final Project project) {
    return vFile != null
           && ServiceManager.getService(project, FileIndexFacade.class).isInSource(vFile)
           && CompilerConfiguration.getInstance(project).isExcludedFromCompilation(vFile);
  }
}
