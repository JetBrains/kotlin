// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.Navigatable;
import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PsiDeclaredTarget;
import com.intellij.pom.references.PomService;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTreeUtilKt;
import com.intellij.util.BitUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class TargetElementUtil extends TargetElementUtilBase {
  /**
   * A flag used in {@link #findTargetElement(Editor, int, int)} indicating that if a reference is found at the specified offset,
   * it should be resolved and the result returned.
   */
  public static final int REFERENCED_ELEMENT_ACCEPTED = 0x01;

  /**
   * A flag used in {@link #findTargetElement(Editor, int, int)} indicating that if a element declaration name (e.g. class name identifier)
   * is found at the specified offset, the declared element should be returned.
   */
  public static final int ELEMENT_NAME_ACCEPTED = 0x02;

  /**
   * A flag used in {@link #findTargetElement(Editor, int, int)} indicating that if a lookup (e.g. completion) is shown in the editor,
   * the PSI element corresponding to the selected lookup item should be returned.
   */
  public static final int LOOKUP_ITEM_ACCEPTED = 0x08;

  public static TargetElementUtil getInstance() {
    return ServiceManager.getService(TargetElementUtil.class);
  }

  @Override
  public int getAllAccepted() {
    int result = REFERENCED_ELEMENT_ACCEPTED | ELEMENT_NAME_ACCEPTED | LOOKUP_ITEM_ACCEPTED;
    for (TargetElementUtilExtender each : TargetElementUtilExtender.EP_NAME.getExtensionList()) {
      result |= each.getAllAdditionalFlags();
    }
    return result;
  }

  @Override
  public int getDefinitionSearchFlags() {
    int result = getAllAccepted();
    for (TargetElementUtilExtender each : TargetElementUtilExtender.EP_NAME.getExtensionList()) {
      result |= each.getAdditionalDefinitionSearchFlags();
    }
    return result;
  }

  @Override
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
    Project project = editor.getProject();
    if (project == null) return null;

    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return null;

    PsiReference ref = file.findReferenceAt(adjustOffset(file, document, offset));
    if (ref == null) return null;
    int elementOffset = ref.getElement().getTextRange().getStartOffset();

    for (TextRange range : ReferenceRange.getRanges(ref)) {
      if (range.shiftRight(elementOffset).containsOffset(offset)) {
        return ref;
      }
    }

    return null;
  }

  /**
   * Attempts to adjust the {@code offset} in the {@code file} to point to an {@link #isIdentifierPart(PsiFile, CharSequence, int) identifier},
   * single quote, double quote, closing bracket or parentheses by moving it back by a single character. Does nothing if there are no
   * identifiers around, or the {@code offset} is already in one.
   *
   * @param file language source for the {@link #isIdentifierPart(com.intellij.psi.PsiFile, java.lang.CharSequence, int)}
   * @see PsiTreeUtilKt#elementsAroundOffsetUp(PsiFile, int)
   */
  public static int adjustOffset(@Nullable PsiFile file, Document document, final int offset) {
    CharSequence text = document.getCharsSequence();
    int correctedOffset = offset;
    int textLength = document.getTextLength();
    if (offset >= textLength) {
      correctedOffset = textLength - 1;
    }
    else if (!isIdentifierPart(file, text, offset)) {
      correctedOffset--;
    }
    if (correctedOffset >= 0) {
      char charAt = text.charAt(correctedOffset);
      if (charAt == '\'' || charAt == '"' || charAt == ')' || charAt == ']' ||
          isIdentifierPart(file, text, correctedOffset)) {
        return correctedOffset;
      }
    }
    return offset;
  }

  /**
   * @return true iff character at the offset may be a part of an identifier.
   * @see Character#isJavaIdentifierPart(char)
   * @see TargetElementEvaluatorEx#isIdentifierPart(com.intellij.psi.PsiFile, java.lang.CharSequence, int)
   */
  private static boolean isIdentifierPart(@Nullable PsiFile file, CharSequence text, int offset) {
    if (file != null) {
      TargetElementEvaluatorEx evaluator = getElementEvaluatorsEx(file.getLanguage());
      if (evaluator != null && evaluator.isIdentifierPart(file, text, offset)) return true;
    }
    return Character.isJavaIdentifierPart(text.charAt(offset));
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
  @Override
  @Nullable
  public PsiElement findTargetElement(@NotNull Editor editor, int flags, int offset) {
    PsiElement result = doFindTargetElement(editor, flags, offset);
    TargetElementEvaluatorEx2 evaluator = result != null ? getElementEvaluatorsEx2(result.getLanguage()) : null;
    if (evaluator != null) {
      result = evaluator.adjustTargetElement(editor, offset, flags, result);
    }
    return result;
  }

  @Nullable
  private PsiElement doFindTargetElement(@NotNull Editor editor, int flags, int offset) {
    Project project = editor.getProject();
    if (project == null) return null;

    if (BitUtil.isSet(flags, LOOKUP_ITEM_ACCEPTED)) {
      PsiElement element = getTargetElementFromLookup(project);
      if (element != null) {
        return element;
      }
    }

    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return null;

    int adjusted = adjustOffset(file, document, offset);

    PsiElement element = file.findElementAt(adjusted);
    if (BitUtil.isSet(flags, REFERENCED_ELEMENT_ACCEPTED)) {
      final PsiElement referenceOrReferencedElement = getReferenceOrReferencedElement(file, editor, flags, offset);
      //if (referenceOrReferencedElement == null) {
      //  return getReferenceOrReferencedElement(file, editor, flags, offset);
      //}
      if (isAcceptableReferencedElement(element, referenceOrReferencedElement)) {
        return referenceOrReferencedElement;
      }
    }

    if (element == null) return null;

    if (BitUtil.isSet(flags, ELEMENT_NAME_ACCEPTED)) {
      if (element instanceof PsiNamedElement) return element;
      return getNamedElement(element, adjusted - element.getTextRange().getStartOffset());
    }
    return null;
  }

  @Nullable
  private static PsiElement getTargetElementFromLookup(Project project) {
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

  private boolean isAcceptableReferencedElement(@Nullable PsiElement element, @Nullable PsiElement referenceOrReferencedElement) {
    if (referenceOrReferencedElement == null || !referenceOrReferencedElement.isValid()) return false;

    TargetElementEvaluatorEx2 evaluator = element != null ? getElementEvaluatorsEx2(element.getLanguage()) : null;
    if (evaluator != null) {
      ThreeState answer = evaluator.isAcceptableReferencedElement(element, referenceOrReferencedElement);
      if (answer == ThreeState.YES) return true;
      if (answer == ThreeState.NO) return false;
    }

    return true;
  }

  @Nullable
  public PsiElement adjustElement(final Editor editor, final int flags, @Nullable PsiElement element, @Nullable PsiElement contextElement) {
    PsiElement langElement = element == null ? contextElement : element;
    TargetElementEvaluatorEx2 evaluator = langElement != null ? getElementEvaluatorsEx2(langElement.getLanguage()) : null;
    if (evaluator != null) {
      element = evaluator.adjustElement(editor, flags, element, contextElement);
    }
    return element;
  }

  @Nullable
  public PsiElement adjustReference(@NotNull PsiReference ref) {
    PsiElement element = ref.getElement();
    TargetElementEvaluatorEx2 evaluator = getElementEvaluatorsEx2(element.getLanguage());
    return evaluator != null ? evaluator.adjustReference(ref) : null;
  }

  @Override
  @Nullable
  public PsiElement getNamedElement(@Nullable final PsiElement element, final int offsetInElement) {
    if (element == null) return null;

    final List<PomTarget> targets = new ArrayList<>();
    final Consumer<PomTarget> consumer = target -> {
      if (target instanceof PsiDeclaredTarget) {
        final PsiDeclaredTarget declaredTarget = (PsiDeclaredTarget)target;
        final PsiElement navigationElement = declaredTarget.getNavigationElement();
        final TextRange range = declaredTarget.getNameIdentifierRange();
        if (range != null && !range.shiftRight(navigationElement.getTextRange().getStartOffset())
          .contains(element.getTextRange().getStartOffset() + offsetInElement)) {
          return;
        }
      }
      targets.add(target);
    };

    PsiElement parent = element;

    int offset = offsetInElement;
    while (parent != null && !(parent instanceof PsiFileSystemItem)) {
      for (PomDeclarationSearcher searcher : PomDeclarationSearcher.EP_NAME.getExtensions()) {
        searcher.findDeclarationsAt(parent, offset, consumer);
        if (!targets.isEmpty()) {
          final PomTarget target = targets.get(0);
          return target == null ? null : PomService.convertToPsi(element.getProject(), target);
        }
      }
      offset += parent.getStartOffsetInParent();
      parent = parent.getParent();
    }

    return getNamedElement(element);
  }


  @Nullable
  private static PsiElement getNamedElement(@Nullable final PsiElement element) {
    if (element == null) return null;

    TargetElementEvaluatorEx2 evaluator = getElementEvaluatorsEx2(element.getLanguage());
    if (evaluator != null) {
      PsiElement result = evaluator.getNamedElement(element);
      if (result != null) return result;
    }

    PsiElement parent;
    if ((parent = PsiTreeUtil.getParentOfType(element, PsiNamedElement.class, false)) != null) {
      // A bit hacky: depends on the named element's text offset being overridden correctly
      if (!(parent instanceof PsiFile) && parent.getTextOffset() == element.getTextRange().getStartOffset()) {
        if (evaluator == null || evaluator.isAcceptableNamedParent(parent)) {
          return parent;
        }
      }
    }

    return null;
  }

  @Nullable
  private static PsiElement getReferenceOrReferencedElement(@NotNull PsiFile file, @NotNull Editor editor, int flags, int offset) {
    PsiElement result = doGetReferenceOrReferencedElement(editor, flags, offset);
    PsiElement languageElement = file.findElementAt(offset);
    Language language = languageElement != null ? languageElement.getLanguage() : file.getLanguage();
    TargetElementEvaluatorEx2 evaluator = getElementEvaluatorsEx2(language);
    if (evaluator != null) {
      result = evaluator.adjustReferenceOrReferencedElement(file, editor, offset, flags, result);
    }
    return result;
  }

  @Nullable
  private static PsiElement doGetReferenceOrReferencedElement(@NotNull Editor editor, int flags, int offset) {
    PsiReference ref = findReference(editor, offset);
    if (ref == null) return null;

    final Language language = ref.getElement().getLanguage();
    TargetElementEvaluator evaluator = TARGET_ELEMENT_EVALUATOR.forLanguage(language);
    if (evaluator != null) {
      final PsiElement element = evaluator.getElementByReference(ref, flags);
      if (element != null) return element;
    }

    return ref.resolve();
  }

  @Override
  @NotNull
  public Collection<PsiElement> getTargetCandidates(@NotNull PsiReference reference) {
    PsiElement refElement = reference.getElement();
    TargetElementEvaluatorEx2 evaluator = getElementEvaluatorsEx2(refElement.getLanguage());
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

  @Override
  public PsiElement getGotoDeclarationTarget(final PsiElement element, final PsiElement navElement) {
    TargetElementEvaluatorEx2 evaluator = element != null ? getElementEvaluatorsEx2(element.getLanguage()) : null;
    if (evaluator != null) {
      PsiElement result = evaluator.getGotoDeclarationTarget(element, navElement);
      if (result != null) return result;
    }
    return navElement;
  }

  @Override
  public boolean includeSelfInGotoImplementation(@NotNull final PsiElement element) {
    TargetElementEvaluator evaluator = TARGET_ELEMENT_EVALUATOR.forLanguage(element.getLanguage());
    return evaluator == null || evaluator.includeSelfInGotoImplementation(element);
  }

  public boolean acceptImplementationForReference(@Nullable PsiReference reference, @Nullable PsiElement element) {
    TargetElementEvaluatorEx2 evaluator = element != null ? getElementEvaluatorsEx2(ReadAction.compute(element::getLanguage)) : null;
    return evaluator == null || evaluator.acceptImplementationForReference(reference, element);
  }

  @Override
  @NotNull
  public SearchScope getSearchScope(Editor editor, @NotNull PsiElement element) {
    TargetElementEvaluatorEx2 evaluator = getElementEvaluatorsEx2(element.getLanguage());
    SearchScope result = evaluator != null ? evaluator.getSearchScope(editor, element) : null;
    if (result != null) return result;

    PsiFile file = element.getContainingFile();
    return PsiSearchHelper.getInstance(element.getProject()).getUseScope(file != null ? file : element);
  }

  private static final LanguageExtension<TargetElementEvaluator> TARGET_ELEMENT_EVALUATOR =
    new LanguageExtension<>("com.intellij.targetElementEvaluator");
  @Nullable
  private static TargetElementEvaluatorEx getElementEvaluatorsEx(@NotNull Language language) {
    TargetElementEvaluator result = TARGET_ELEMENT_EVALUATOR.forLanguage(language);
    return result instanceof TargetElementEvaluatorEx ? (TargetElementEvaluatorEx)result : null;
  }
  @Nullable
  private static TargetElementEvaluatorEx2 getElementEvaluatorsEx2(@NotNull Language language) {
    TargetElementEvaluator result = TARGET_ELEMENT_EVALUATOR.forLanguage(language);
    return result instanceof TargetElementEvaluatorEx2 ? (TargetElementEvaluatorEx2)result : null;
  }
}
