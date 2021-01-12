// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.reference.SoftReference;
import com.intellij.ui.LightweightHint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.ref.WeakReference;

public class ShowContainerInfoHandler implements CodeInsightActionHandler {
  private static final Key<WeakReference<LightweightHint>> MY_LAST_HINT_KEY = Key.create("MY_LAST_HINT_KEY");
  private static final Key<PsiElement> CONTAINER_KEY = Key.create("CONTAINER_KEY");

  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file) {

    PsiElement container = getProcessedHint(editor);

    StructureViewBuilder builder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file);
    if (builder instanceof TreeBasedStructureViewBuilder) {
      StructureViewModel model = ((TreeBasedStructureViewBuilder) builder).createStructureViewModel(editor);
      boolean goOneLevelUp = true;
      try {
        if (container == null) {
          goOneLevelUp = false;
          Object element = model.getCurrentEditorElement();
          if (element instanceof PsiElement) {
            container = (PsiElement) element;
          }
        }
      }
      finally {
        Disposer.dispose(model);
      }
      while(true) {
        while(container != null && DeclarationRangeUtil.getPossibleDeclarationAtRange(container) == null) {
          container = container.getParent();
          if (container instanceof PsiFile) return;
        }
        if (container == null || container instanceof PsiFile) {
          return;
        }
        if (goOneLevelUp) {
          goOneLevelUp = false;
        }
        else {
          if (!isDeclarationVisible(container, editor)) {
            break;
          }
        }

        container = container.getParent();
      }
    }
    if (container == null) {
      return;
    }

    final TextRange range = DeclarationRangeUtil.getPossibleDeclarationAtRange(container);
    if (range == null) {
      return;
    }
    final PsiElement _container = container;
    ApplicationManager.getApplication().invokeLater(() -> {
      LightweightHint hint1 = EditorFragmentComponent.showEditorFragmentHint(editor, range, true, true);
      if (hint1 != null) {
        hint1.putUserData(CONTAINER_KEY, _container);
        editor.putUserData(MY_LAST_HINT_KEY, new WeakReference<>(hint1));
      }
    });
  }

  /**
   * If context info was already called before, this method will return PsiElement, that was shown (userData by CONTAINER_KEY)
   *
   * null if context info was new executed, or not actual anymore
   */
  @Nullable
  public static PsiElement getProcessedHint(@NotNull Editor editor) {
    WeakReference<LightweightHint> ref = editor.getUserData(MY_LAST_HINT_KEY);
    LightweightHint hint = SoftReference.dereference(ref);
    if (hint != null && hint.isVisible()){
      hint.hide();
      PsiElement container = hint.getUserData(CONTAINER_KEY);
      if (container != null && container.isValid()){
        return container;
      }
    }
    return null;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  private static boolean isDeclarationVisible(PsiElement container, Editor editor) {
    Rectangle viewRect = editor.getScrollingModel().getVisibleArea();
    final TextRange range = DeclarationRangeUtil.getPossibleDeclarationAtRange(container);
    if (range == null) {
      return false;
    }

    LogicalPosition pos = editor.offsetToLogicalPosition(range.getStartOffset());
    Point loc = editor.logicalPositionToXY(pos);
    return loc.y >= viewRect.y;
  }
}
