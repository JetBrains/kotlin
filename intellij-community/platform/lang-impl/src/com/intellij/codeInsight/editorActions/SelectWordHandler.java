// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.DataManager;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.CompositeLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.tree.injected.InjectedCaret;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SelectWordHandler extends EditorActionHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.editorActions.SelectWordHandler");

  private final EditorActionHandler myOriginalHandler;

  public SelectWordHandler(EditorActionHandler originalHandler) {
    super(true);
    myOriginalHandler = originalHandler;
  }

  @Override
  public void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    assert caret != null;
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: execute(editor='" + editor + "')");
    }
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, caret, dataContext);
      }
      return;
    }
    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

    TextRange range = selectWord(caret, project);
    if (editor instanceof EditorWindow) {
      if (range == null || !isInsideEditableInjection((EditorWindow)editor, range, project) || TextRange.from(0, editor.getDocument().getTextLength()).equals(
        new TextRange(caret.getSelectionStart(), caret.getSelectionEnd()))) {
        editor = ((EditorWindow)editor).getDelegate();
        caret = ((InjectedCaret)caret).getDelegate();
        range = selectWord(caret, project);
      }
    }
    if (range == null) {
      if (myOriginalHandler != null) {
        myOriginalHandler.execute(editor, caret, dataContext);
      }
    }
    else {
      caret.setSelection(range.getStartOffset(), range.getEndOffset());
    }
  }

  private static boolean isInsideEditableInjection(EditorWindow editor, TextRange range, Project project) {
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return true;
    List<TextRange> editables = InjectedLanguageManager.getInstance(project).intersectWithAllEditableFragments(file, range);

    return editables.size() == 1 && range.equals(editables.get(0));
  }

  @Nullable("null means unable to select")
  private static TextRange selectWord(@NotNull Caret caret, @NotNull Project project) {
    Document document = caret.getEditor().getDocument();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file == null) return null;

    FeatureUsageTracker.getInstance().triggerFeatureUsed("editing.select.word");

    int caretOffset = adjustCaretOffset(caret);

    PsiElement element = findElementAt(file, caretOffset);

    if (element instanceof PsiWhiteSpace && caretOffset > 0) {
      PsiElement anotherElement = findElementAt(file, caretOffset - 1);

      if (!(anotherElement instanceof PsiWhiteSpace)) {
        element = anotherElement;
      }
    }

    while (element instanceof PsiWhiteSpace || element != null && StringUtil.isEmptyOrSpaces(element.getText())) {
      while (element.getNextSibling() == null) {
        if (element instanceof PsiFile) return null;
        final PsiElement parent = element.getParent();
        final PsiElement[] children = parent.getChildren();

        if (children.length > 0 && children[children.length - 1] == element) {
          element = parent;
        }
        else {
          element = parent;
          break;
        }
      }

      if (element instanceof PsiFile) return null;
      element = element.getNextSibling();
      if (element == null) return null;
      TextRange range = element.getTextRange();
      if (range == null) return null; // Fix NPE (EA-29110)
      caretOffset = range.getStartOffset();
    }

    if (element instanceof OuterLanguageElement) {
      PsiElement elementInOtherTree = file.getViewProvider().findElementAt(element.getTextOffset(), element.getLanguage());
      if (elementInOtherTree != null && elementInOtherTree.getContainingFile() != element.getContainingFile()) {
        while (elementInOtherTree != null && elementInOtherTree.getPrevSibling() == null) {
          elementInOtherTree = elementInOtherTree.getParent();
        }

        if (elementInOtherTree != null) {
          if (elementInOtherTree.getTextOffset() == caretOffset) element = elementInOtherTree;
        }
      }
    }

    checkElementRange(document, element);

    final TextRange selectionRange = new TextRange(caret.getSelectionStart(), caret.getSelectionEnd());

    final Ref<TextRange> minimumRange = new Ref<>(new TextRange(0, document.getTextLength()));

    SelectWordUtil.processRanges(element, document.getCharsSequence(), caretOffset, caret.getEditor(), range -> {
      if (range.contains(selectionRange) && !range.equals(selectionRange)) {
        if (minimumRange.get().contains(range)) {
          minimumRange.set(range);
          return true;
        }
      }
      return false;
    });

    return minimumRange.get();
  }

  private static void checkElementRange(Document document, PsiElement element) {
    if (element != null && element.getTextRange().getEndOffset() > document.getTextLength()) {
      throw new AssertionError(DebugUtil.diagnosePsiDocumentInconsistency(element, document));
    }
  }

  private static int adjustCaretOffset(@NotNull Caret caret) {
    int caretOffset = caret.getOffset();
    if (caretOffset == 0) {
      return caretOffset;
    }

    CharSequence text = caret.getEditor().getDocument().getCharsSequence();
    char prev = text.charAt(caretOffset - 1);
    if (caretOffset < text.length() &&
        !Character.isJavaIdentifierPart(text.charAt(caretOffset)) && Character.isJavaIdentifierPart(prev)) {
      return caretOffset - 1;
    }
    if ((caretOffset == text.length() || Character.isWhitespace(text.charAt(caretOffset))) && !Character.isWhitespace(prev)) {
      return caretOffset - 1;
    }
    return caretOffset;
  }

  @Nullable
  private static PsiElement findElementAt(@NotNull final PsiFile file, final int caretOffset) {
    PsiElement elementAt = file.findElementAt(caretOffset);
    if (elementAt != null && isLanguageExtension(file, elementAt)) {
      return file.getViewProvider().findElementAt(caretOffset, file.getLanguage());
    }
    return elementAt;
  }

  private static boolean isLanguageExtension(@NotNull final PsiFile file, @NotNull final PsiElement elementAt) {
    final Language elementLanguage = elementAt.getLanguage();
    if (file.getLanguage() instanceof CompositeLanguage) {
      CompositeLanguage compositeLanguage = (CompositeLanguage) file.getLanguage();
      final Language[] extensions = compositeLanguage.getLanguageExtensionsForFile(file);
      for(Language extension: extensions) {
        if (extension == elementLanguage) {
          return true;
        }
      }
    }
    return false;
  }

}