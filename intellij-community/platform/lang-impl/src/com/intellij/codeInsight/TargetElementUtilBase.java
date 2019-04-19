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
package com.intellij.codeInsight;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @deprecated Use {@link TargetElementUtil} instead. To be removed in IntelliJ IDEA 16.
 */
@Deprecated
public abstract class TargetElementUtilBase {
  public static TargetElementUtilBase getInstance() {
    return TargetElementUtil.getInstance();
  }

  public abstract int getAllAccepted();

  public abstract int getDefinitionSearchFlags();

  public abstract int getReferenceSearchFlags();

  @Nullable
  public static PsiReference findReference(Editor editor) {
    return TargetElementUtil.findReference(editor);
  }

  @Nullable
  public static PsiReference findReference(@NotNull Editor editor, int offset) {
    return TargetElementUtil.findReference(editor, offset);
  }

  public static int adjustOffset(Document document, final int offset) {
    return TargetElementUtil.adjustOffset(null, document, offset);
  }

  public static int adjustOffset(@Nullable PsiFile file, Document document, final int offset) {
    return TargetElementUtil.adjustOffset(file, document, offset);
  }

  public static boolean inVirtualSpace(@NotNull Editor editor, int offset) {
    return TargetElementUtil.inVirtualSpace(editor, offset);
  }

  @Nullable
  public static PsiElement findTargetElement(Editor editor, int flags) {
    return TargetElementUtil.findTargetElement(editor, flags);
  }

  @Nullable
  public abstract PsiElement findTargetElement(@NotNull Editor editor, int flags, int offset);


  @Nullable
  public abstract PsiElement getNamedElement(@Nullable final PsiElement element, final int offsetInElement);

  @NotNull
  public abstract Collection<PsiElement> getTargetCandidates(@NotNull PsiReference reference);

  public abstract PsiElement getGotoDeclarationTarget(final PsiElement element, final PsiElement navElement);

  public abstract boolean includeSelfInGotoImplementation(@NotNull final PsiElement element);

  /**
   * @return a scope where element's implementations (Goto/Show Implementations) should be searched
   */
  @NotNull
  public abstract SearchScope getSearchScope(Editor editor, @NotNull PsiElement element);
}
