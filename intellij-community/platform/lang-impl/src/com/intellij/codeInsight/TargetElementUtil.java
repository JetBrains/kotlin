// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TargetElementUtil  {
  /**
   * @see TargetElementUtilBase#REFERENCED_ELEMENT_ACCEPTED
   */
  public static final int REFERENCED_ELEMENT_ACCEPTED = TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED;

  /**
   * @see TargetElementUtilBase#ELEMENT_NAME_ACCEPTED
   */
  public static final int ELEMENT_NAME_ACCEPTED = TargetElementUtilBase.ELEMENT_NAME_ACCEPTED;

  /**
   * A flag used in {@link #findTargetElement(Editor, int, int)} indicating that if a lookup (e.g. completion) is shown in the editor,
   * the PSI element corresponding to the selected lookup item should be returned.
   */
  public static final int LOOKUP_ITEM_ACCEPTED = 0x08;

  public static TargetElementUtil getInstance() {
    return ServiceManager.getService(TargetElementUtil.class);
  }

  public int getAllAccepted() {
    int result = REFERENCED_ELEMENT_ACCEPTED | ELEMENT_NAME_ACCEPTED | LOOKUP_ITEM_ACCEPTED;
    for (TargetElementUtilExtender each : TargetElementUtilExtender.EP_NAME.getExtensionList()) {
      result |= each.getAllAdditionalFlags();
    }
    return result;
  }

  public int getDefinitionSearchFlags() {
    int result = getAllAccepted();
    for (TargetElementUtilExtender each : TargetElementUtilExtender.EP_NAME.getExtensionList()) {
      result |= each.getAdditionalDefinitionSearchFlags();
    }
    return result;
  }

  public int getReferenceSearchFlags() {
    int result = getAllAccepted();
    for (TargetElementUtilExtender each : TargetElementUtilExtender.EP_NAME.getExtensionList()) {
      result |= each.getAdditionalReferenceSearchFlags();
    }
    return result;
  }

  @Nullable
  public static PsiReference findReference(@NotNull Editor editor) {
    int offset = editor.getCaretModel().getOffset();
    PsiReference result = findReference(editor, offset);
    if (result == null) {
      int expectedCaretOffset = editor instanceof EditorEx ? ((EditorEx)editor).getExpectedCaretOffset() : offset;
      if (expectedCaretOffset != offset) {
        result = findReference(editor, expectedCaretOffset);
      }
    }
    return result;
  }

  @Nullable
  public static PsiReference findReference(@NotNull Editor editor, int offset) {
    return TargetElementUtilBase.findReference(editor, offset);
  }

  public static int adjustOffset(@Nullable PsiFile file, Document document, final int offset) {
    return TargetElementUtilBase.adjustOffset(file, document, offset);
  }

  public static boolean inVirtualSpace(@NotNull Editor editor, int offset) {
    return offset == editor.getCaretModel().getOffset()
           && EditorUtil.inVirtualSpace(editor, editor.getCaretModel().getLogicalPosition());
  }

  /**
   * Note: this method can perform slow PSI activity (e.g. {@link PsiReference#resolve()}, so please avoid calling it from Swing thread.
   * @param editor editor
   * @param flags a combination of {@link #REFERENCED_ELEMENT_ACCEPTED}, {@link #ELEMENT_NAME_ACCEPTED}, {@link #LOOKUP_ITEM_ACCEPTED}
   * @return a PSI element declared or referenced at the editor caret position, or selected in the {@link Lookup} if shown in the editor,
   * depending on the flags passed.
   * @see #findTargetElement(Editor, int, int)
   */
  @Nullable
  public static PsiElement findTargetElement(Editor editor, int flags) {
    int offset = editor.getCaretModel().getOffset();
    final PsiElement result = getInstance().findTargetElement(editor, flags, offset);
    if (result != null) return result;

    int expectedCaretOffset = editor instanceof EditorEx ? ((EditorEx)editor).getExpectedCaretOffset() : offset;
    if (expectedCaretOffset != offset) {
      return getInstance().findTargetElement(editor, flags, expectedCaretOffset);
    }
    return null;
  }

  /**
   * Note: this method can perform slow PSI activity (e.g. {@link PsiReference#resolve()}, so please avoid calling it from Swing thread.
   * @param editor editor
   * @param flags a combination of {@link #REFERENCED_ELEMENT_ACCEPTED}, {@link #ELEMENT_NAME_ACCEPTED}, {@link #LOOKUP_ITEM_ACCEPTED}
   * @param offset offset in the editor's document
   * @return a PSI element declared or referenced at the specified offset in the editor, or selected in the {@link Lookup} if shown in the editor,
   * depending on the flags passed.
   * @see #findTargetElement(Editor, int)
   */
  @Nullable
  public PsiElement findTargetElement(@NotNull Editor editor, int flags, int offset) {
    Project project = editor.getProject();
    if (project == null) return null;

    if (BitUtil.isSet(flags, LOOKUP_ITEM_ACCEPTED)) {
      PsiElement element = getTargetElementFromLookup(project);
      if (element != null) {
        return element;
      }
    }

    return TargetElementUtilBase.findTargetElement(editor, flags, offset);
  }

  @Internal
  public static @Nullable PsiElement getTargetElementFromLookup(Project project) {
    Lookup activeLookup = LookupManager.getInstance(project).getActiveLookup();
    if (activeLookup != null) {
      LookupElement item = activeLookup.getCurrentItem();
      if (item != null && item.isValid()) {
        final PsiElement psi = CompletionUtil.getTargetElement(item);
        if (psi != null && psi.isValid()) {
          return psi;
        }
      }
    }
    return null;
  }

  @Nullable
  public PsiElement adjustElement(final Editor editor, final int flags, @Nullable PsiElement element, @Nullable PsiElement contextElement) {
    PsiElement langElement = element == null ? contextElement : element;
    TargetElementEvaluatorEx2 evaluator = langElement != null ? TargetElementUtilBase.getElementEvaluatorsEx2(langElement.getLanguage()) : null;
    if (evaluator != null) {
      element = evaluator.adjustElement(editor, flags, element, contextElement);
    }
    return element;
  }

  @Nullable
  public PsiElement adjustReference(@NotNull PsiReference ref) {
    PsiElement element = ref.getElement();
    TargetElementEvaluatorEx2 evaluator = TargetElementUtilBase.getElementEvaluatorsEx2(element.getLanguage());
    return evaluator != null ? evaluator.adjustReference(ref) : null;
  }

  @Nullable
  public PsiElement getNamedElement(@Nullable final PsiElement element, final int offsetInElement) {
    return TargetElementUtilBase.getNamedElement(element, offsetInElement);
  }

  @Internal
  @Nullable
  public static PsiElement getNamedElement(@Nullable final PsiElement element) {
    return TargetElementUtilBase.getNamedElement(element);
  }

  @NotNull
  public Collection<PsiElement> getTargetCandidates(@NotNull PsiReference reference) {
    PsiElement refElement = reference.getElement();
    TargetElementEvaluatorEx2 evaluator = TargetElementUtilBase.getElementEvaluatorsEx2(refElement.getLanguage());
    if (evaluator != null) {
      Collection<PsiElement> candidates = evaluator.getTargetCandidates(reference);
      if (candidates != null) return candidates;
    }

    if (reference instanceof PsiPolyVariantReference) {
      final ResolveResult[] results = ((PsiPolyVariantReference)reference).multiResolve(false);
      List<PsiElement> navigatableResults = new ArrayList<>(results.length);

      for (ResolveResult r : results) {
        PsiElement element = r.getElement();
        if (isNavigatableSource(element)) {
          navigatableResults.add(element);
        }
      }

      return navigatableResults;
    }
    PsiElement resolved = reference.resolve();
    if (resolved instanceof NavigationItem) {
      return Collections.singleton(resolved);
    }
    return Collections.emptyList();
  }


  @Contract("null -> false")
  public boolean isNavigatableSource(@Nullable PsiElement element) {
    return EditSourceUtil.canNavigate(element) || element instanceof Navigatable && ((Navigatable)element).canNavigateToSource();
  }

  public PsiElement getGotoDeclarationTarget(final PsiElement element, final PsiElement navElement) {
    TargetElementEvaluatorEx2 evaluator = element != null ? TargetElementUtilBase.getElementEvaluatorsEx2(element.getLanguage()) : null;
    if (evaluator != null) {
      PsiElement result = evaluator.getGotoDeclarationTarget(element, navElement);
      if (result != null) return result;
    }
    return navElement;
  }

  public boolean includeSelfInGotoImplementation(@NotNull final PsiElement element) {
    TargetElementEvaluator evaluator = TargetElementUtilBase.TARGET_ELEMENT_EVALUATOR.forLanguage(element.getLanguage());
    return evaluator == null || evaluator.includeSelfInGotoImplementation(element);
  }

  public boolean acceptImplementationForReference(@Nullable PsiReference reference, @Nullable PsiElement element) {
    TargetElementEvaluatorEx2 evaluator = element != null ? TargetElementUtilBase.getElementEvaluatorsEx2(ReadAction.compute(element::getLanguage)) : null;
    return evaluator == null || evaluator.acceptImplementationForReference(reference, element);
  }

  @NotNull
  public SearchScope getSearchScope(Editor editor, @NotNull PsiElement element) {
    TargetElementEvaluatorEx2 evaluator = TargetElementUtilBase.getElementEvaluatorsEx2(element.getLanguage());
    SearchScope result = evaluator != null ? evaluator.getSearchScope(editor, element) : null;
    if (result != null) return result;

    PsiFile file = element.getContainingFile();
    return PsiSearchHelper.getInstance(element.getProject()).getUseScope(file != null ? file : element);
  }
}
