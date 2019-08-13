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

package com.intellij.openapi.paths;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class StaticPathReferenceProvider extends PathReferenceProviderBase {

  private boolean myEndingSlashNotAllowed;
  private boolean myRelativePathsAllowed;
  private final FileType[] mySuitableFileTypes;

  public StaticPathReferenceProvider(@Nullable final FileType[] suitableFileTypes) {
    mySuitableFileTypes = suitableFileTypes;
  }

  @Override
  public boolean createReferences(@NotNull final PsiElement psiElement,
                                  final int offset,
                                  final String text,
                                  final @NotNull List<PsiReference> references,
                                  final boolean soft) {

    FileReferenceSet set = new FileReferenceSet(text, psiElement, offset, null, true, myEndingSlashNotAllowed, mySuitableFileTypes) {
      @Override
      protected boolean isUrlEncoded() {
        return true;
      }

      @Override
      protected boolean isSoft() {
        return soft;
      }
    };
    if (!myRelativePathsAllowed) {
      set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, FileReferenceSet.ABSOLUTE_TOP_LEVEL);
    }
    Collections.addAll(references, set.getAllReferences());
    return true;
  }

  @Override
  @Nullable
  public PathReference getPathReference(@NotNull final String path, @NotNull final PsiElement element) {
    final List<PsiReference> list = new SmartList<>();
    createReferences(element, list, true);
    if (list.isEmpty()) return null;

    final PsiElement target = list.get(list.size() - 1).resolve();
    if (target == null) return null;

    return new PathReference(path, PathReference.ResolveFunction.NULL_RESOLVE_FUNCTION) {
      @Override
      public PsiElement resolve() {
        return target;
      }
    };

  }

  public void setEndingSlashNotAllowed(final boolean endingSlashNotAllowed) {
    myEndingSlashNotAllowed = endingSlashNotAllowed;
  }

  public void setRelativePathsAllowed(final boolean relativePathsAllowed) {
    myRelativePathsAllowed = relativePathsAllowed;
  }
}
