// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.*;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author yole
 */
public class PsiImplementationViewSession implements ImplementationViewSession {
  private static final Logger LOG = Logger.getInstance(PsiImplementationViewSession.class);

  @NotNull private final Project myProject;
  @Nullable private final PsiElement myElement;
  private final PsiElement[] myImpls;
  private final String myText;
  @Nullable private final Editor myEditor;
  @Nullable private final VirtualFile myFile;
  private final boolean myIsSearchDeep;
  private final boolean myAlwaysIncludeSelf;

  public PsiImplementationViewSession(@NotNull Project project, @Nullable PsiElement element, PsiElement[] impls, String text,
                                      @Nullable Editor editor,
                                      @Nullable VirtualFile file,
                                      boolean isSearchDeep,
                                      boolean alwaysIncludeSelf) {
    myProject = project;
    myElement = element;
    myImpls = impls;
    myText = text;
    myEditor = editor;
    myFile = file;
    myIsSearchDeep = isSearchDeep;
    myAlwaysIncludeSelf = alwaysIncludeSelf;
  }

  @NotNull
  @Override
  public ImplementationViewSessionFactory getFactory() {
    return ImplementationViewSessionFactory.EP_NAME.findExtensionOrFail(PsiImplementationSessionViewFactory.class);
  }

  @Override
  @NotNull
  public Project getProject() {
    return myProject;
  }

  @Nullable
  public PsiElement getElement() {
    return myElement;
  }

  @Override
  @NotNull
  public List<ImplementationViewElement> getImplementationElements() {
    return ContainerUtil.map(myImpls, PsiImplementationViewElement::new);
  }

  @Override
  public String getText() {
    return myText;
  }

  @Override
  @Nullable
  public Editor getEditor() {
    return myEditor;
  }

  @Override
  @Nullable
  public VirtualFile getFile() {
    return myFile;
  }

  @Override
  public boolean elementRequiresIncludeSelf() {
    return !(myElement instanceof PomTargetPsiElement);
  }

  @Override
  public boolean needUpdateInBackground() {
    return myElement != null;
  }

  @Override
  public void dispose() {
  }

  @NotNull
  static PsiElement[] filterElements(@NotNull final PsiElement[] targetElements) {
    final Set<PsiElement> unique = new LinkedHashSet<>(Arrays.asList(targetElements));
    for (final PsiElement elt : targetElements) {
      ApplicationManager.getApplication().runReadAction(() -> {
        final PsiFile containingFile = elt.getContainingFile();
        LOG.assertTrue(containingFile != null, elt);
        PsiFile psiFile = containingFile.getOriginalFile();
        if (psiFile.getVirtualFile() == null) unique.remove(elt);
      });
    }
    // special case for Python (PY-237)
    // if the definition is the tree parent of the target element, filter out the target element
    for (int i = 1; i < targetElements.length; i++) {
      final PsiElement targetElement = targetElements[i];
      if (ReadAction.compute(() -> PsiTreeUtil.isAncestor(targetElement, targetElements[0], true))) {
        unique.remove(targetElements[0]);
        break;
      }
    }
    return PsiUtilCore.toPsiElementArray(unique);
  }

  @NotNull
  public static ImplementationSearcher createImplementationsSearcher(final boolean searchDeep) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return new ImplementationSearcher() {
        @Override
        protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
          return PsiImplementationViewSession.filterElements(targetElements);
        }
      };
    }
    return new ImplementationSearcher.FirstImplementationsSearcher() {
      @Override
      protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
        return PsiImplementationViewSession.filterElements(targetElements);
      }

      @Override
      protected boolean isSearchDeep() {
        return searchDeep;
      }
    };
  }

  @NotNull
  static PsiElement[] getSelfAndImplementations(Editor editor,
                                                @NotNull PsiElement element,
                                                @NotNull ImplementationSearcher handler) {
    return getSelfAndImplementations(editor, element, handler, !(element instanceof PomTargetPsiElement));
  }

  @NotNull
  public static PsiElement[] getSelfAndImplementations(Editor editor,
                                                        @NotNull PsiElement element,
                                                        @NotNull ImplementationSearcher handler,
                                                final boolean includeSelfAlways) {
    final PsiElement[] handlerImplementations = handler.searchImplementations(element, editor, includeSelfAlways, true);
    if (handlerImplementations.length > 0) return handlerImplementations;

    return ReadAction.compute(() -> {
      PsiElement psiElement = element;
      PsiFile psiFile = psiElement.getContainingFile();
      if (psiFile == null) {
        // Magically, it's null for ant property declarations.
        psiElement = psiElement.getNavigationElement();
        psiFile = psiElement.getContainingFile();
        if (psiFile == null) {
          return PsiElement.EMPTY_ARRAY;
        }
      }
      if (psiFile.getVirtualFile() != null && (psiElement.getTextRange() != null || psiElement instanceof PsiFile)) {
        return new PsiElement[]{psiElement};
      }
      return PsiElement.EMPTY_ARRAY;
    });
  }

  @NotNull
  @Override
  public List<ImplementationViewElement> searchImplementationsInBackground(@NotNull ProgressIndicator indicator,
                                                                           @NotNull final Processor<? super ImplementationViewElement> processor) {
    final ImplementationSearcher.BackgroundableImplementationSearcher implementationSearcher =
      new ImplementationSearcher.BackgroundableImplementationSearcher() {
        @Override
        protected boolean isSearchDeep() {
          return myIsSearchDeep;
        }

        @Override
        protected void processElement(PsiElement element) {
          if (!processor.process(new PsiImplementationViewElement(element))) {
            indicator.cancel();
          }
          indicator.checkCanceled();
        }

        @Override
        protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
          return PsiImplementationViewSession.filterElements(targetElements);
        }
      };
    PsiElement[] psiElements;
    if (!myAlwaysIncludeSelf) {
      psiElements = getSelfAndImplementations(myEditor, myElement, implementationSearcher, false);
    }
    else {
      psiElements = getSelfAndImplementations(myEditor, myElement, implementationSearcher);
    }
    return ContainerUtil.map(psiElements, PsiImplementationViewElement::new);
  }

  @Nullable
  public static Editor getEditor(@NotNull DataContext dataContext) {
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

    if (editor == null) {
      final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
      if (file != null) {
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
          final FileEditor fileEditor = FileEditorManager.getInstance(file.getProject()).getSelectedEditor(virtualFile);
          if (fileEditor instanceof TextEditor) {
            editor = ((TextEditor)fileEditor).getEditor();
          }
        }
      }
    }
    return editor;
  }

  @Nullable
  public static PsiImplementationViewSession create(@NotNull DataContext dataContext,
                                                     Project project,
                                                     boolean searchDeep,
                                                     boolean alwaysIncludeSelf) {
    PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
    Editor editor = getEditor(dataContext);

    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    element = getElement(project, file, editor, element);

    if (element == null && file == null) return null;
    PsiFile containingFile = element != null ? element.getContainingFile() : file;
    if (containingFile == null || !containingFile.getViewProvider().isPhysical()) return null;


    PsiReference ref = null;
    if (editor != null) {
      ref = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());
      if (element == null && ref != null) {
        element = TargetElementUtil.getInstance().adjustReference(ref);
      }
    }

    //check attached sources if any
    if (element instanceof PsiCompiledElement) {
      element = element.getNavigationElement();
    }

    String text = "";
    PsiElement[] impls = PsiElement.EMPTY_ARRAY;
    if (element != null) {
      impls = getSelfAndImplementations(editor, element, createImplementationsSearcher(searchDeep));
      text = SymbolPresentationUtil.getSymbolPresentableText(element);
    }

    if (impls.length == 0 && ref instanceof PsiPolyVariantReference) {
      final PsiPolyVariantReference polyReference = (PsiPolyVariantReference)ref;
      PsiElement refElement = polyReference.getElement();
      TextRange rangeInElement = polyReference.getRangeInElement();
      String refElementText = refElement.getText();
      LOG.assertTrue(rangeInElement.getEndOffset() <= refElementText.length(),
                     "Ref:" + polyReference + "; refElement: " + refElement + "; refText:" + refElementText);
      text = rangeInElement.substring(refElementText);
      final ResolveResult[] results = polyReference.multiResolve(false);
      final List<PsiElement> implsList = new ArrayList<>(results.length);

      for (ResolveResult result : results) {
        final PsiElement resolvedElement = result.getElement();

        if (resolvedElement != null && resolvedElement.isPhysical()) {
          implsList.add(resolvedElement);
        }
      }

      if (!implsList.isEmpty()) {
        impls = implsList.toArray(PsiElement.EMPTY_ARRAY);
      }
    }

    return new PsiImplementationViewSession(project, element, impls, text, editor,
                                            file != null ? file.getVirtualFile() : null,
                                            searchDeep, alwaysIncludeSelf);
  }

  public static PsiElement getElement(@NotNull Project project, PsiFile file, Editor editor, PsiElement element) {
    if (element == null && editor != null) {
      element = TargetElementUtil.findTargetElement(editor, TargetElementUtil.getInstance().getAllAccepted());
      final PsiElement adjustedElement =
        TargetElementUtil.getInstance().adjustElement(editor, TargetElementUtil.getInstance().getAllAccepted(), element, null);
      if (adjustedElement != null) {
        element = adjustedElement;
      }
      else if (file != null) {
        element = DocumentationManager.getInstance(project).getElementFromLookup(editor, file);
      }
    }
    return element;
  }
}
