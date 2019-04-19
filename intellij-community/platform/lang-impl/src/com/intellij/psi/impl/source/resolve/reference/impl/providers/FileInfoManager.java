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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.file.FileLookupInfoProvider;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * @author spleaner
 */
public class FileInfoManager implements Disposable {
  private final Map<FileType, FileLookupInfoProvider> myFileType2InfoProvider = new HashMap<>();

  public FileInfoManager() {
    final FileLookupInfoProvider[] providers = FileLookupInfoProvider.EP_NAME.getExtensions();
    for (final FileLookupInfoProvider provider : providers) {
      final FileType[] types = provider.getFileTypes();
      for (FileType type : types) {
        myFileType2InfoProvider.put(type, provider);
      }
    }
  }

  public static FileInfoManager getFileInfoManager() {
    return ServiceManager.getService(FileInfoManager.class);
  }

  public static Object getFileLookupItem(PsiElement psiElement) {
    if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
      return psiElement;
    }

    final PsiFile file = (PsiFile)psiElement;
    return getFileInfoManager()._getLookupItem(file, file.getName(), file.getIcon(0));
  }

  @Nullable
  public static String getFileAdditionalInfo(PsiElement psiElement) {
    return getFileInfoManager()._getInfo(psiElement);
  }

  @Nullable
  private String _getInfo(PsiElement psiElement) {
    if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
      return null;
    }

    final PsiFile psiFile = (PsiFile)psiElement;
    final FileLookupInfoProvider provider = myFileType2InfoProvider.get(psiFile.getFileType());
    if (provider != null) {
      final VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        final Pair<String, String> info = provider.getLookupInfo(virtualFile, psiElement.getProject());
        return Pair.getSecond(info);
      }
    }

    return null;
  }

  public static LookupElementBuilder getFileLookupItem(PsiElement psiElement, String encoded, Icon icon) {
    if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
      return LookupElementBuilder.create(psiElement, encoded).withIcon(icon);
    }

    return getFileInfoManager()._getLookupItem((PsiFile)psiElement, encoded, icon);
  }

  public LookupElementBuilder _getLookupItem(@NotNull final PsiFile file, String name, Icon icon) {
    LookupElementBuilder builder = LookupElementBuilder.create(file, name).withIcon(icon);

    final String info = _getInfo(file);
    if (info != null) {
      return builder.withTailText(String.format(" (%s)", info), true);
    }

    return builder;
  }

  @Override
  public void dispose() {
    myFileType2InfoProvider.clear();
  }
}
