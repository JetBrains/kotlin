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
package com.intellij.ide.util.gotoByName;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ProjectBaseDirNavigationContributor implements ChooseByNameContributor, DumbAware {

  @Override
  @NotNull
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    final VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) return ArrayUtil.EMPTY_STRING_ARRAY;
    final VirtualFile[] files = baseDir.getChildren();
    final ArrayList<String> list = new ArrayList<>();
    for (VirtualFile file : files) {
      if (!file.isDirectory()) {
        list.add(file.getName());
      }
    }
    return ArrayUtil.toStringArray(list);
  }

  @Override
  @NotNull
  public NavigationItem[] getItemsByName(String name, final String pattern, Project project, boolean includeNonProjectItems) {
    final PsiManager psiManager = PsiManager.getInstance(project);
    final VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) return NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY;
    final VirtualFile[] files = baseDir.getChildren();
    final ArrayList<PsiFile> list = new ArrayList<>();
    for (VirtualFile file : files) {
      if (isEditable(file, includeNonProjectItems) && Comparing.strEqual(name, file.getName())) {
        final PsiFile psiFile = psiManager.findFile(file);
        if (psiFile != null) {
          list.add(psiFile);
        }
      }
    }
    return PsiUtilCore.toPsiFileArray(list);
  }

  private static boolean isEditable(VirtualFile file, final boolean checkboxState) {
    FileType type = file.getFileType();
    if (!checkboxState && type == StdFileTypes.JAVA) return false;
    return type != StdFileTypes.CLASS;
  }
}