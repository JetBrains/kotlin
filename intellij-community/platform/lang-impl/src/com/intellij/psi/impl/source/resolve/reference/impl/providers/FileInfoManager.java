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

package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.file.FileLookupInfoProvider;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author spleaner
 */
public class FileInfoManager {
  public static FileInfoManager getFileInfoManager() {
    return ServiceManager.getService(FileInfoManager.class);
  }

  public static Object getFileLookupItem(PsiElement psiElement) {
    if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
      return psiElement;
    }

    final PsiFile file = (PsiFile)psiElement;
    return _getLookupItem(file, file.getName(), file.getIcon(0));
  }

  @Nullable
  public static String getFileAdditionalInfo(PsiElement psiElement) {
    return _getInfo(psiElement);
  }

  @Nullable
  private static String _getInfo(PsiElement psiElement) {
    if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
      return null;
    }

    final PsiFile psiFile = (PsiFile)psiElement;

    FileLookupInfoProvider provider =
      ContainerUtil.find(FileLookupInfoProvider.EP_NAME.getExtensionList(),
                         p -> ArrayUtil.find(p.getFileTypes(), psiFile.getFileType()) != -1);

    if (provider != null) {
      VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        Pair<String, String> info = provider.getLookupInfo(virtualFile, psiElement.getProject());
        return Pair.getSecond(info);
      }
    }

    return null;
  }

  public static LookupElementBuilder getFileLookupItem(PsiElement psiElement, String encoded, Icon icon) {
    if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
      return LookupElementBuilder.create(psiElement, encoded).withIcon(icon);
    }
    return _getLookupItem((PsiFile)psiElement, encoded, icon);
  }

  public static LookupElementBuilder _getLookupItem(@NotNull final PsiFile file, String name, Icon icon) {
    LookupElementBuilder builder = LookupElementBuilder.create(file, name).withIcon(icon);

    final String info = _getInfo(file);
    if (info != null) {
      return builder.withTailText(String.format(" (%s)", info), true);
    }

    return builder;
  }
}
