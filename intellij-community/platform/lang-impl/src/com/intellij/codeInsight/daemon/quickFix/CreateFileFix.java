// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import java.io.IOException;

/**
 * @author peter
 * @deprecated Use {@link CreateDirectoryPathFix} or {@link CreateFilePathFix} instead.
*/
@Deprecated
public class CreateFileFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private static final int REFRESH_INTERVAL = 1000;

  private final boolean myIsDirectory;
  private final String myNewFileName;
  private final String myText;
  @PropertyKey(resourceBundle = CodeInsightBundle.BUNDLE) @NotNull private final String myKey;
  private boolean myIsAvailable;
  private long myIsAvailableTimeStamp;

  // invoked from other module
  @SuppressWarnings("WeakerAccess")
  public CreateFileFix(boolean isDirectory,
                       @NotNull String newFileName,
                       @NotNull PsiDirectory directory,
                       @Nullable String text,
                       @PropertyKey(resourceBundle = CodeInsightBundle.BUNDLE) @NotNull String key) {
    super(directory);

    myIsDirectory = isDirectory;
    myNewFileName = newFileName;
    myText = text;
    myKey = key;
    myIsAvailable = isDirectory || !FileTypeManager.getInstance().getFileTypeByFileName(newFileName).isBinary();
    myIsAvailableTimeStamp = System.currentTimeMillis();
  }

  public CreateFileFix(@NotNull String newFileName, @NotNull PsiDirectory directory, String text) {
    this(false,newFileName,directory, text, "create.file.text");
  }

  public CreateFileFix(final boolean isDirectory, @NotNull String newFileName, @NotNull PsiDirectory directory) {
    this(isDirectory,newFileName,directory,null, isDirectory ? "create.directory.text":"create.file.text" );
  }

  @Nullable
  protected String getFileText() {
    return myText;
  }

  @Override
  @NotNull
  public String getText() {
    return CodeInsightBundle.message(myKey, myNewFileName);
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return CodeInsightBundle.message("create.file.family");
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@NotNull PsiFile file) {
    return null;
  }

  @Override
  public void invoke(@NotNull final Project project,
                     @NotNull PsiFile file,
                     Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    if (isAvailable(project, null, file)) {
      invoke(project, (PsiDirectory)startElement);
    }
  }

  @Override
  public void applyFix() {
    invoke(myStartElement.getProject(), (PsiDirectory)myStartElement.getElement());
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {
    final PsiDirectory myDirectory = (PsiDirectory)startElement;
    long current = System.currentTimeMillis();

    if (ApplicationManager.getApplication().isUnitTestMode() || current - myIsAvailableTimeStamp > REFRESH_INTERVAL) {
      myIsAvailable &= myDirectory.getVirtualFile().findChild(myNewFileName) == null;
      myIsAvailableTimeStamp = current;
    }

    return myIsAvailable;
  }

  private void invoke(@NotNull Project project, PsiDirectory myDirectory) throws IncorrectOperationException {
    myIsAvailableTimeStamp = 0; // to revalidate applicability

    try {
      if (myIsDirectory) {
        myDirectory.createSubdirectory(myNewFileName);
      }
      else {
        String newFileName = myNewFileName;
        String newDirectories = null;
        if (myNewFileName.contains("/")) {
          int pos = myNewFileName.lastIndexOf('/');
          newFileName = myNewFileName.substring(pos + 1);
          newDirectories = myNewFileName.substring(0, pos);
        }
        PsiDirectory directory = myDirectory;
        if (newDirectories != null) {
          try {
            VfsUtil.createDirectoryIfMissing(myDirectory.getVirtualFile(), newDirectories);
            VirtualFile vfsDir = VfsUtil.findRelativeFile(myDirectory.getVirtualFile(),
                                                          ArrayUtil.toStringArray(StringUtil.split(newDirectories, "/")));
            directory = vfsDir == null ? null : myDirectory.getManager().findDirectory(vfsDir);
            if (directory == null) throw new IOException("Couldn't create directory '" + newDirectories + "'");
          }
          catch (IOException e) {
            throw new IncorrectOperationException(e.getMessage());
          }
        }
        final PsiFile newFile = directory.createFile(newFileName);
        String text = getFileText();

        if (text != null) {
          final FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(newFileName);
          final PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("_" + newFileName, type, text);
          final PsiElement psiElement = CodeStyleManager.getInstance(project).reformat(psiFile);
          text = psiElement.getText();
        }

        openFile(project, directory, newFile, text);
      }
    }
    catch (IncorrectOperationException e) {
      myIsAvailable = false;
    }
  }

  protected void openFile(@NotNull Project project, PsiDirectory directory, PsiFile newFile, String text) {
    final FileEditorManager editorManager = FileEditorManager.getInstance(directory.getProject());
    final FileEditor[] fileEditors = editorManager.openFile(newFile.getVirtualFile(), true);

    if (text != null) {
      for(FileEditor fileEditor: fileEditors) {
        if (fileEditor instanceof TextEditor) { // JSP is not safe to edit via Psi
          final Document document = ((TextEditor)fileEditor).getEditor().getDocument();
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
