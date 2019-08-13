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

package com.intellij.refactoring.rename.inplace;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.lang.LanguageRefactoringSupport;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import com.intellij.refactoring.rename.RenameHandler;
import com.intellij.refactoring.rename.RenameHandlerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VariableInplaceRenameHandler implements RenameHandler {
  private static final ThreadLocal<String> ourPreventInlineRenameFlag = new ThreadLocal<>();
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler");

  @Override
  public final boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
    final PsiElement element = PsiElementRenameHandler.getElement(dataContext);
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
    if (editor == null || file == null) return false;

    if (ourPreventInlineRenameFlag.get() != null) {
      return false;
    }
    return isAvailable(element, editor, file);
  }

  protected boolean isAvailable(@Nullable PsiElement element,
                                @NotNull Editor editor,
                                @NotNull PsiFile file) {
    final PsiElement nameSuggestionContext = file.findElementAt(editor.getCaretModel().getOffset());

    RefactoringSupportProvider supportProvider =
      element == null ? null : LanguageRefactoringSupport.INSTANCE.forLanguage(element.getLanguage());
    return supportProvider != null &&
           editor.getSettings().isVariableInplaceRenameEnabled() &&
           supportProvider.isInplaceRenameAvailable(element, nameSuggestionContext);
  }

  @Override
  public final boolean isRenaming(@NotNull DataContext dataContext) {
    return isAvailableOnDataContext(dataContext);
  }

  @Override
  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file, final DataContext dataContext) {
    PsiElement element = PsiElementRenameHandler.getElement(dataContext);
    if (element == null) {
      if (LookupManager.getActiveLookup(editor) != null) {
        final PsiElement elementUnderCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementUnderCaret != null) {
          final PsiElement parent = elementUnderCaret.getParent();
          if (parent instanceof PsiReference) {
            element = ((PsiReference)parent).resolve();
          } else {
            element = PsiTreeUtil.getParentOfType(elementUnderCaret, PsiNamedElement.class);
          }
        }
        if (element == null) return;
      } else {
        return;
      }
    }
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    if (checkAvailable(element, editor, dataContext)) {
      doRename(element, editor, dataContext);
    }
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiElement[] elements,
                     @NotNull DataContext dataContext) {
    PsiElement element = elements.length == 1 ? elements[0] : null;
    if (element == null) element = PsiElementRenameHandler.getElement(dataContext);
    LOG.assertTrue(element != null);
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (checkAvailable(element, editor, dataContext)) {
      assert editor != null;
      doRename(element, editor, dataContext);
    }
  }

  protected boolean checkAvailable(final PsiElement elementToRename, final Editor editor, final DataContext dataContext) {
    if (!isAvailableOnDataContext(dataContext)) {
      LOG.error("Recursive invocation");
      RenameHandler handler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext);
      if (handler == null) return false;

      handler.invoke(
        elementToRename.getProject(),
        editor,
        elementToRename.getContainingFile(), dataContext
      );
      return false;
    }
    FeatureUsageTracker.getInstance().triggerFeatureUsed("refactoring.rename");
    return true;
  }

  @Nullable
  public InplaceRefactoring doRename(@NotNull PsiElement elementToRename,
                                     @NotNull Editor editor,
                                     @Nullable DataContext dataContext) {
    VariableInplaceRenamer renamer = createRenamer(elementToRename, editor);
    boolean startedRename = renamer != null && renamer.performInplaceRename();

    if (!startedRename) {
      performDialogRename(elementToRename, editor, dataContext, renamer != null ? renamer.myInitialName : null);
    }
    return renamer;
  }

  protected static void performDialogRename(PsiElement elementToRename, Editor editor, DataContext dataContext, String initialName) {
    try {
      ourPreventInlineRenameFlag.set(initialName == null ? "" : initialName);
      RenameHandler handler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext);
      assert handler != null : elementToRename.getClass().getName() + ":" + elementToRename;
      handler.invoke(
        elementToRename.getProject(),
        editor,
        elementToRename.getContainingFile(), dataContext
      );
    } finally {
      ourPreventInlineRenameFlag.set(null);
    }
  }
  
  @Nullable
  public static String getInitialName() {
    final String str = ourPreventInlineRenameFlag.get();
    return StringUtil.isEmpty(str) ? null : str;
  }

  @Nullable
  protected VariableInplaceRenamer createRenamer(@NotNull PsiElement elementToRename, @NotNull Editor editor) {
    return new VariableInplaceRenamer((PsiNamedElement)elementToRename, editor);
  }
}
