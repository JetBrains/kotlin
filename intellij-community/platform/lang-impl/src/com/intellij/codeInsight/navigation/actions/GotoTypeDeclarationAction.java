// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.navigation.actions;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class GotoTypeDeclarationAction extends BaseCodeInsightAction implements CodeInsightActionHandler, DumbAware {

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler(){
    return this;
  }

  @Override
  protected boolean isValidForLookup() {
    return true;
  }

  @Override
  public void update(@NotNull final AnActionEvent event) {
    if (TypeDeclarationProvider.EP_NAME.getExtensionList().size() == 0) {
      event.getPresentation().setVisible(false);
      return;
    }
    for (TypeDeclarationProvider provider : TypeDeclarationProvider.EP_NAME.getExtensionList()) {
      String text = provider.getActionText(event.getDataContext());
      if (text != null) {
        Presentation presentation = event.getPresentation();
        presentation.setText(text);
        break;
      }
    }
    super.update(event);
  }

  @Override
  public void invoke(@NotNull final Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    DumbService.getInstance(project).setAlternativeResolveEnabled(true);
    try {
      int offset = editor.getCaretModel().getOffset();
      PsiElement[] symbolTypes = GotoDeclarationAction.underModalProgress(project, "Resolving Reference...",
                                                                          () -> findSymbolTypes(editor, offset));
      if (symbolTypes == null || symbolTypes.length == 0) return;
      if (symbolTypes.length == 1) {
        navigate(project, symbolTypes[0]);
      }
      else {
        NavigationUtil.getPsiElementPopup(symbolTypes, CodeInsightBundle.message("choose.type.popup.title")).showInBestPositionFor(editor);
      }
    }
    catch (IndexNotReadyException e) {
      DumbService.getInstance(project).showDumbModeNotification("Navigation is not available here during index update");
    }
    finally {
      DumbService.getInstance(project).setAlternativeResolveEnabled(false);
    }
  }

  private static void navigate(@NotNull Project project, @NotNull PsiElement symbolType) {
    PsiElement element = symbolType.getNavigationElement();
    assert element != null : "SymbolType :"+symbolType+"; file: "+symbolType.getContainingFile();
    VirtualFile file = element.getContainingFile().getVirtualFile();
    if (file != null) {
      OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, element.getTextOffset());
      FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public boolean startInTransaction() {
    return true;
  }

  @Nullable
  public static PsiElement findSymbolType(@NotNull Editor editor, int offset) {
    final PsiElement[] psiElements = findSymbolTypes(editor, offset);
    if (psiElements != null && psiElements.length > 0) return psiElements[0];
    return null;
  }

  @Nullable
  @VisibleForTesting
  public static PsiElement[] findSymbolTypes(@NotNull Editor editor, int offset) {
    PsiElement targetElement = TargetElementUtil.getInstance().findTargetElement(editor,
                                                                                     TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED |
                                                                                     TargetElementUtil.ELEMENT_NAME_ACCEPTED |
                                                                                     TargetElementUtil.LOOKUP_ITEM_ACCEPTED,
                                                                                     offset);

    if (targetElement != null) {
      final PsiElement[] symbolType = getSymbolTypeDeclarations(targetElement, editor, offset);
      return symbolType == null ? PsiElement.EMPTY_ARRAY : symbolType;
    }

    final PsiReference psiReference = TargetElementUtil.findReference(editor, offset);
    if (psiReference instanceof PsiPolyVariantReference) {
      final ResolveResult[] results = ((PsiPolyVariantReference)psiReference).multiResolve(false);
      Set<PsiElement> types = new THashSet<>();

      for(ResolveResult r: results) {
        PsiElement element = r.getElement();
        if (element == null) continue;
        final PsiElement[] declarations = getSymbolTypeDeclarations(element, editor, offset);
        if (declarations != null) {
          for (PsiElement declaration : declarations) {
            assert declaration != null;
            types.add(declaration);
          }
        }
      }

      if (!types.isEmpty()) return PsiUtilCore.toPsiElementArray(types);
    }

    return null;
  }

  @Nullable
  private static PsiElement[] getSymbolTypeDeclarations(@NotNull PsiElement targetElement, Editor editor, int offset) {
    for(TypeDeclarationProvider provider: TypeDeclarationProvider.EP_NAME.getExtensionList()) {
      PsiElement[] result;
      if (provider instanceof TypeDeclarationPlaceAwareProvider) {
        result = ((TypeDeclarationPlaceAwareProvider)provider).getSymbolTypeDeclarations(targetElement, editor, offset);
      }
      else {
        result = provider.getSymbolTypeDeclarations(targetElement);
      }
      if (result != null) return result;
    }

    return null;
  }

}
