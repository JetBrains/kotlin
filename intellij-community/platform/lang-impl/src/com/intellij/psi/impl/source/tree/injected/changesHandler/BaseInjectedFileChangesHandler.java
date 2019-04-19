// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.injected.changesHandler;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.InjectedFileChangesHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import org.jetbrains.annotations.NotNull;

public abstract class BaseInjectedFileChangesHandler implements InjectedFileChangesHandler {

  /**
   * Host editor
   */
  protected final Editor myEditor;

  /**
   * Host document
   */
  protected final Document myOrigDocument;

  /**
   * Document created for the fragment editor
   */
  protected final Document myNewDocument;
  protected final Project myProject;

  /**
   * File injected in the Host
   * <p>
   * NOTE: implementations rarely need to access this field.
   * It is useful mostly only for {@link PsiFile#isValid()} check but implementations could work even having {@link #myInjectedFile} invalidated
   */
  protected PsiFile myInjectedFile;

  public BaseInjectedFileChangesHandler(Editor editor, Document newDocument, PsiFile injectedFile) {
    myProject = editor.getProject();
    myEditor = editor;
    myOrigDocument = editor.getDocument();
    myNewDocument = newDocument;
    myInjectedFile = injectedFile;
  }


  @Override
  public boolean tryReuse(@NotNull PsiFile newInjectedFile, @NotNull TextRange newHostRange) {
    if (myInjectedFile == newInjectedFile) return handlesRange(newHostRange);

    if (myInjectedFile == null || !myInjectedFile.isValid()) {
      DocumentWindow documentWindow = InjectedLanguageUtil.getDocumentWindow(newInjectedFile);
      if (documentWindow != null && handlesRange(newHostRange)) {
        myInjectedFile = newInjectedFile;
        return true;
      }
    }

    return false;
  }

  @Override
  public void dispose() {
  }
}
