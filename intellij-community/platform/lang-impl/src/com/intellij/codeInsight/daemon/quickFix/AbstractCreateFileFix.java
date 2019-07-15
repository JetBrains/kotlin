// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.file.PsiDirectoryImpl;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public abstract class AbstractCreateFileFix extends LocalQuickFixAndIntentionActionOnPsiElement {

  protected AbstractCreateFileFix(@Nullable PsiElement element) {
    super(element);
  }

  protected void createFile(@NotNull Project project, @Nullable PsiDirectory myDirectory, String fileName) throws IncorrectOperationException {
    if (myDirectory == null) {
      return;
    }

    String newFileName = fileName;
    String newDirectories = null;
    if (fileName.contains("/")) {
      int pos = fileName.lastIndexOf('/');
      newFileName = fileName.substring(pos + 1);
      newDirectories = fileName.substring(0, pos);
    }
    PsiDirectory directory = myDirectory;
    if (newDirectories != null) {
      try {
        VfsUtil.createDirectoryIfMissing(myDirectory.getVirtualFile(), newDirectories);
        VirtualFile vfsDir = VfsUtil.findRelativeFile(myDirectory.getVirtualFile(),
                                                      ArrayUtilRt.toStringArray(StringUtil.split(newDirectories, "/")));

        if (vfsDir == null) {
          Logger.getInstance(AbstractCreateFileFix.class)
            .warn("Unable to find relative file" + myDirectory.getVirtualFile().getPath());
          return;
        }

        directory = new PsiDirectoryImpl((PsiManagerImpl)myDirectory.getManager(), vfsDir);
      }
      catch (IOException e) {
        throw new IncorrectOperationException(e.getMessage());
      }
    }
    PsiFile newFile = directory.createFile(newFileName);
    String text = getFileText();

    if (text != null) {
      FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(newFileName);
      PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("_" + newFileName, type, text);
      PsiElement psiElement = CodeStyleManager.getInstance(project).reformat(psiFile);
      text = psiElement.getText();
    }

    openFile(project, directory, newFile, text);
  }

  @Nullable
  protected abstract String getFileText();

  protected void openFile(@NotNull Project project, PsiDirectory directory, PsiFile newFile, String text) {
    FileEditorManager editorManager = FileEditorManager.getInstance(directory.getProject());
    FileEditor[] fileEditors = editorManager.openFile(newFile.getVirtualFile(), true);

    if (text != null) {
      for (FileEditor fileEditor : fileEditors) {
        if (fileEditor instanceof TextEditor) { // JSP is not safe to edit via Psi
          Document document = ((TextEditor)fileEditor).getEditor().getDocument();
          document.setText(text);

          if (ApplicationManager.getApplication().isUnitTestMode()) {
            FileDocumentManager.getInstance().saveDocument(document);
          }
          PsiDocumentManager.getInstance(project).commitDocument(document);
          break;
        }
      }
    }
  }
}
