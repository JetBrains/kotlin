/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.psi.util.proximity;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.util.NullableLazyKey;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.ProximityLocation;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class SameDirectoryWeigher extends ProximityWeigher {
  private static final NullableLazyKey<PsiDirectory, ProximityLocation>
    PLACE_DIRECTORY = NullableLazyKey.create("placeDirectory", location -> getParentDirectory(location.getPosition()));

  private static PsiDirectory getParentDirectory(PsiElement element) {
    PsiFile file = InjectedLanguageManager.getInstance(element.getProject()).getTopLevelFile(element);
    if (file != null) {
      element = file.getOriginalFile();
    }
    while (element != null && !(element instanceof PsiDirectory)) {
      element = element.getParent();
    }
    return (PsiDirectory)element;
  }

  @Override
  public Boolean weigh(@NotNull final PsiElement element, @NotNull final ProximityLocation location) {
    if (location.getPosition() == null) {
      return Boolean.TRUE;
    }
    final PsiDirectory placeDirectory = PLACE_DIRECTORY.getValue(location);
    return placeDirectory != null && placeDirectory.equals(getParentDirectory(element));
  }
}
