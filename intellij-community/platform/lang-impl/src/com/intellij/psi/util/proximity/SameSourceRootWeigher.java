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
package com.intellij.psi.util.proximity;

import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.NullableLazyKey;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.ProximityLocation;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
*/
public class SameSourceRootWeigher extends ProximityWeigher {
  private static final NullableLazyKey<VirtualFile, ProximityLocation> SOURCE_ROOT_KEY = NullableLazyKey.create("sourceRoot",
                                                                                                                 proximityLocation -> findSourceRoot(proximityLocation.getPosition()));

  @Override
  public Comparable weigh(@NotNull final PsiElement element, @NotNull final ProximityLocation location) {
    if (location.getPosition() == null){
      return null;
    }
    final VirtualFile sourceRoot = SOURCE_ROOT_KEY.getValue(location);
    if (sourceRoot == null) {
      return false;
    }

    return sourceRoot.equals(findSourceRoot(element));
  }

  private static VirtualFile findSourceRoot(PsiElement element) {
    if (element == null) return null;

    final PsiFile psiFile = element.getContainingFile();
    if (psiFile == null) return null;

    final VirtualFile file = psiFile.getOriginalFile().getVirtualFile();
    if (file == null) return null;

    return ProjectFileIndex.getInstance(element.getProject()).getSourceRootForFile(file);
  }
}