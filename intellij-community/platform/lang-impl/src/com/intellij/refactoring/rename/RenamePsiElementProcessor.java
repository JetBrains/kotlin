// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.util.MoveRenameUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public abstract class RenamePsiElementProcessor {
  private static final Logger LOG = Logger.getInstance(RenamePsiElementProcessor.class);

  public static final ExtensionPointName<RenamePsiElementProcessor> EP_NAME = ExtensionPointName.create("com.intellij.renamePsiElementProcessor");

  public abstract boolean canProcessElement(@NotNull PsiElement element);

  @NotNull
  public RenameDialog createRenameDialog(@NotNull Project project,
                                         @NotNull PsiElement element,
                                         @Nullable PsiElement nameSuggestionContext,
                                         @Nullable Editor editor) {
    return new RenameDialog(project, element, nameSuggestionContext, editor);
  }

  public void renameElement(@NotNull PsiElement element,
                            @NotNull String newName,
                            @NotNull UsageInfo[] usages,
                            @Nullable RefactoringElementListener listener) throws IncorrectOperationException {
    RenameUtil.doRenameGenericNamedElement(element, newName, usages, listener);
  }

  /** @deprecated use {@link RenamePsiElementProcessor#findReferences(PsiElement, SearchScope, boolean)} instead */
  @Deprecated
  @NotNull
  public Collection<PsiReference> findReferences(@NotNull PsiElement element, boolean searchInCommentsAndStrings) {
    return myOldFindMethodsImplemented
           ? findReferences(element)
           : findReferences(element, GlobalSearchScope.projectScope(element.getProject()), searchInCommentsAndStrings);
  }

  /** @deprecated use {@link RenamePsiElementProcessor#findReferences(PsiElement, SearchScope, boolean)} instead */
  @Deprecated
  @NotNull
  public Collection<PsiReference> findReferences(@NotNull PsiElement element) {
    return myOldFindMethodsImplemented
           ? ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.getProject())).findAll()
           : findReferences(element, GlobalSearchScope.projectScope(element.getProject()), false);
  }

  @NotNull
  public Collection<PsiReference> findReferences(@NotNull PsiElement element,
                                                 @NotNull SearchScope searchScope,
                                                 boolean searchInCommentsAndStrings) {
    if (myOldFindMethodsImplemented) {
      Collection<PsiReference> refs = findReferences(element, searchInCommentsAndStrings);
      if (!searchScope.equals(GlobalSearchScope.projectScope(element.getProject()))) {
        ArrayList<PsiReference> result = new ArrayList<>();
        for (PsiReference ref : refs) {
          VirtualFile file = PsiUtilCore.getVirtualFile(ref.getElement());
          if (file == null || searchScope.contains(file)) result.add(ref);
        }
        return result;
      }
      return refs;
    }
    return ReferencesSearch.search(element, searchScope).findAll();
  }

  @Nullable
  public Pair<String, String> getTextOccurrenceSearchStrings(@NotNull PsiElement element, @NotNull String newName) {
    return null;
  }

  @Nullable
  public String getQualifiedNameAfterRename(@NotNull PsiElement element, @NotNull String newName, final boolean nonJava) {
    return null;
  }

  /**
   * Builds the complete set of elements to be renamed during the refactoring.
   *
   * @param element the base element for the refactoring.
   * @param newName the name into which the element is being renamed.
   * @param allRenames the map (from element to its new name) into which all additional elements to be renamed should be stored.
   */
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    prepareRenaming(element, newName, allRenames, element.getUseScope());
  }

  public void prepareRenaming(@NotNull PsiElement element,
                              @NotNull String newName,
                              @NotNull Map<PsiElement, String> allRenames,
                              @NotNull SearchScope scope) {
  }

  public void findExistingNameConflicts(@NotNull PsiElement element,
                                        @NotNull String newName,
                                        @NotNull MultiMap<PsiElement, String> conflicts) {
  }

  public void findExistingNameConflicts(@NotNull PsiElement element,
                                        @NotNull String newName,
                                        @NotNull MultiMap<PsiElement, String> conflicts,
                                        @NotNull Map<PsiElement, String> allRenames) {
    findExistingNameConflicts(element, newName, conflicts);
  }

  public boolean isInplaceRenameSupported() {
    return true;
  }

  @NotNull
  public static List<RenamePsiElementProcessor> allForElement(@NotNull PsiElement element) {
    final List<RenamePsiElementProcessor> result = new ArrayList<>();
    for (RenamePsiElementProcessor processor : EP_NAME.getExtensions()) {
      if (processor.canProcessElement(element)) {
        result.add(processor);
      }
    }
    return result;
  }

  @NotNull
  public static RenamePsiElementProcessor forElement(@NotNull PsiElement element) {
    for (RenamePsiElementProcessor processor : EP_NAME.getExtensionList()) {
      if (processor.canProcessElement(element)) {
        return processor;
      }
    }
    return DEFAULT;
  }

  @Nullable
  public Runnable getPostRenameCallback(@NotNull PsiElement element,
                                        @NotNull String newName,
                                        @NotNull RefactoringElementListener elementListener) {
    return null;
  }

  @Nullable
  @NonNls
  public String getHelpID(final PsiElement element) {
    if (element instanceof PsiFile) {
      return "refactoring.renameFile";
    }
    return "refactoring.renameDialogs";
  }

  public boolean isToSearchInComments(@NotNull PsiElement element) {
    return element instanceof PsiFileSystemItem && RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE;
  }

  public void setToSearchInComments(@NotNull PsiElement element, boolean enabled) {
    if (element instanceof PsiFileSystemItem) {
      RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE = enabled;
    }
  }

  public boolean isToSearchForTextOccurrences(@NotNull PsiElement element) {
    return element instanceof PsiFileSystemItem && RefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_FILE;
  }

  public void setToSearchForTextOccurrences(@NotNull PsiElement element, boolean enabled) {
    if (element instanceof PsiFileSystemItem) {
      RefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_FILE = enabled;
    }
  }

  public boolean showRenamePreviewButton(@NotNull PsiElement psiElement){
    return true;
  }

  /**
   * Returns the element to be renamed instead of the element on which the rename refactoring was invoked (for example, a super method
   * of an inherited method).
   *
   * @param element the element on which the refactoring was invoked.
   * @param editor the editor in which the refactoring was invoked.
   * @return the element to rename, or null if the rename refactoring should be canceled.
   */
  @Nullable
  public PsiElement substituteElementToRename(@NotNull PsiElement element, @Nullable Editor editor) {
    return element;
  }

  /**
   * Substitutes element to be renamed and initiate rename procedure. Should be used in order to prevent modal dialogs to appear during inplace rename
   * @param element the element on which refactoring was invoked
   * @param editor the editor in which inplace refactoring was invoked
   * @param renameCallback rename procedure which should be called on the chosen substitution
   */
  public void substituteElementToRename(@NotNull final PsiElement element, @NotNull Editor editor, @NotNull Pass<PsiElement> renameCallback) {
    final PsiElement psiElement = substituteElementToRename(element, editor);
    if (psiElement == null) return;
    if (!PsiElementRenameHandler.canRename(psiElement.getProject(), editor, psiElement)) return;
    renameCallback.pass(psiElement);
  }

  public void findCollisions(@NotNull PsiElement element,
                             @NotNull String newName,
                             @NotNull Map<? extends PsiElement, String> allRenames,
                             @NotNull List<UsageInfo> result) {
  }

  public static final RenamePsiElementProcessor DEFAULT = new RenamePsiElementProcessor() {
    @Override
    public boolean canProcessElement(@NotNull final PsiElement element) {
      return true;
    }
  };

  /**
   * Use this method to force showing preview for custom processors.
   * This method is always called after prepareRenaming()
   * @return force show preview
   */
  public boolean forcesShowPreview() {
    return false;
  }

  @Nullable
  public PsiElement getElementToSearchInStringsAndComments(@NotNull PsiElement element) {
    return element;
  }

  @NotNull
  public UsageInfo createUsageInfo(@NotNull PsiElement element, @NotNull PsiReference ref, @NotNull PsiElement referenceElement) {
    return new MoveRenameUsageInfo(referenceElement, ref,
                                   ref.getRangeInElement().getStartOffset(),
                                   ref.getRangeInElement().getEndOffset(),
                                   element,
                                   ref.resolve() == null && !(ref instanceof PsiPolyVariantReference && ((PsiPolyVariantReference)ref).multiResolve(true).length > 0));
  }

  final boolean myOldFindMethodsImplemented;
  {
    boolean implemented;
    try {
      Method find1 = getClass().getMethod("findReferences", PsiElement.class);
      Method find2 = getClass().getMethod("findReferences", PsiElement.class, Boolean.TYPE);
      implemented = !RenamePsiElementProcessor.class.equals(find1.getDeclaringClass()) ||
                    !RenamePsiElementProcessor.class.equals(find2.getDeclaringClass());
      if (implemented) {
        LOG.warn(getClass().getName() + " overrides deprecated findReferences(..).\n" +
                 "Override findReferences(PsiElement, SearchScope, boolean) instead.");
      }
    }
    catch (NoSuchMethodException e) {
      implemented = false;
      LOG.warn(e);
    }
    myOldFindMethodsImplemented = implemented;
  }

}
