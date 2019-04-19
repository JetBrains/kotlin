// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template;

import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.diagnostic.AttachmentFactory;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eugene.Kudelevsky
 */
public class CustomTemplateCallback {
  private static final Logger LOGGER = Logger.getInstance(CustomTemplateCallback.class);
  private final TemplateManager myTemplateManager;
  @NotNull private final Editor myEditor;
  @NotNull private final PsiFile myFile;
  private final int myOffset;
  @NotNull private final Project myProject;
  private final boolean myInInjectedFragment;
  protected Set<TemplateContextType> myApplicableContextTypes;

  public CustomTemplateCallback(@NotNull Editor editor, @NotNull PsiFile file) {
    myProject = file.getProject();
    myTemplateManager = TemplateManager.getInstance(myProject);

    int parentEditorOffset = getOffset(editor);
    PsiElement element = InjectedLanguageManager.getInstance(file.getProject()).findInjectedElementAt(file, parentEditorOffset);
    myFile = element != null ? element.getContainingFile() : file;

    myInInjectedFragment = InjectedLanguageManager.getInstance(myProject).isInjectedFragment(myFile);
    myEditor = myInInjectedFragment ? InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file, parentEditorOffset) : editor;
    myOffset = myInInjectedFragment ? getOffset(myEditor) : parentEditorOffset;
  }

  public TemplateManager getTemplateManager() {
    return myTemplateManager;
  }

  @NotNull
  public PsiFile getFile() {
    return myFile;
  }

  @NotNull
  public PsiElement getContext() {
    return getContext(myFile, getOffset(), myInInjectedFragment);
  }

  public int getOffset() {
    return myOffset;
  }

  public static int getOffset(@NotNull Editor editor) {
    SelectionModel selectionModel = editor.getSelectionModel();
    return selectionModel.hasSelection() ? selectionModel.getSelectionStart() : Math.max(editor.getCaretModel().getOffset() - 1, 0);
  }

  @Nullable
  public TemplateImpl findApplicableTemplate(@NotNull String key) {
    return ContainerUtil.getFirstItem(findApplicableTemplates(key));
  }

  @NotNull
  public List<TemplateImpl> findApplicableTemplates(@NotNull String key) {
    List<TemplateImpl> result = new ArrayList<>();
    for (TemplateImpl candidate : getMatchingTemplates(key)) {
      if (isAvailableTemplate(candidate)) {
        result.add(candidate);
      }
    }
    return result;
  }

  private boolean isAvailableTemplate(@NotNull TemplateImpl template) {
    if (myApplicableContextTypes == null) {
      myApplicableContextTypes = TemplateManagerImpl.getApplicableContextTypes(myFile, myOffset);
    }
    return !template.isDeactivated() && TemplateManagerImpl.isApplicable(template, myApplicableContextTypes);
  }

  public void startTemplate(@NotNull Template template, Map<String, String> predefinedValues, TemplateEditingListener listener) {
    if (myInInjectedFragment) {
      template.setToReformat(false);
    }
    myTemplateManager.startTemplate(myEditor, template, false, predefinedValues, listener);
  }

  @NotNull
  private static List<TemplateImpl> getMatchingTemplates(@NotNull String templateKey) {
    TemplateSettings settings = TemplateSettings.getInstance();
    List<TemplateImpl> candidates = new ArrayList<>();
    for (TemplateImpl template : settings.getTemplates(templateKey)) {
      if (!template.isDeactivated()) {
        candidates.add(template);
      }
    }
    return candidates;
  }

  @NotNull
  public Editor getEditor() {
    return myEditor;
  }

  @NotNull
  public FileType getFileType() {
    return myFile.getFileType();
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  public void deleteTemplateKey(@NotNull String key) {
    int caretAt = myEditor.getCaretModel().getOffset();
    int templateStart = caretAt - key.length();
    myEditor.getDocument().deleteString(templateStart, caretAt);
    myEditor.getCaretModel().moveToOffset(templateStart);
    myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    myEditor.getSelectionModel().removeSelection();
  }

  @NotNull
  public static PsiElement getContext(@NotNull PsiFile file, int offset) {
    return getContext(file, offset, true);
  }

  @NotNull
  public static PsiElement getContext(@NotNull PsiFile file, int offset, boolean searchInInjectedFragment) {
    PsiElement element = null;
    if (searchInInjectedFragment && !InjectedLanguageManager.getInstance(file.getProject()).isInjectedFragment(file)) {
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
      Document document = documentManager.getDocument(file);
      if (document != null && !documentManager.isCommitted(document)) {
        LOGGER.error("Trying to access to injected template context on uncommited document, offset = " + offset,
                     AttachmentFactory.createAttachment(file.getVirtualFile()));
      }
      else {
        element = InjectedLanguageManager.getInstance(file.getProject()).findInjectedElementAt(file, offset);
      }
    }
    if (element == null) {
      element = PsiUtilCore.getElementAtOffset(file, offset);
    }
    return element;
  }

  public boolean isInInjectedFragment() {
    return myInInjectedFragment;
  }
}
