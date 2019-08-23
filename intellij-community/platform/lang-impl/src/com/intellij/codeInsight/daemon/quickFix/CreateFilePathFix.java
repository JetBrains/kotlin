// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
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
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Quick fix that creates a new file in one of the target directories. Automatically creates all intermediate directories of
 * {@link TargetDirectory#getPathToCreate()} and {@link NewFileLocation#getSubPath()}. If there are multiple target directories it shows
 * a popup where users can select desired target directory.
 */
public class CreateFilePathFix extends AbstractCreateFileFix {
  private final String myText;
  @Nullable
  private Supplier<String> myFileTextSupplier;

  // invoked from other module
  @SuppressWarnings("WeakerAccess")
  public CreateFilePathFix(@NotNull PsiElement psiElement,
                           @NotNull NewFileLocation newFileLocation,
                           @Nullable String fileText,
                           @NotNull String fixLocaleKey) {
    super(psiElement, newFileLocation, fixLocaleKey);

    myText = fileText;
    myIsAvailable = !FileTypeManager.getInstance().getFileTypeByFileName(myNewFileName).isBinary();
    myIsAvailableTimeStamp = System.currentTimeMillis();
  }

  public CreateFilePathFix(@NotNull PsiElement psiElement,
                           @NotNull NewFileLocation newFileLocation) {
    this(psiElement, newFileLocation, null, "create.file.text");
  }

  public CreateFilePathFix(@NotNull PsiElement psiElement,
                           @NotNull NewFileLocation newFileLocation,
                           @NotNull Supplier<String> fileTextSupplier) {
    this(psiElement, newFileLocation, null, "create.file.text");

    myFileTextSupplier = fileTextSupplier;
  }

  private void createFile(@NotNull Project project, @NotNull PsiDirectory currentDirectory, @NotNull String fileName)
    throws IncorrectOperationException {

    String newFileName = fileName;
    String newDirectories = null;
    if (fileName.contains("/")) {
      int pos = fileName.lastIndexOf('/');
      newFileName = fileName.substring(pos + 1);
      newDirectories = fileName.substring(0, pos);
    }
    PsiDirectory directory = currentDirectory;
    if (newDirectories != null) {
      try {
        VfsUtil.createDirectoryIfMissing(currentDirectory.getVirtualFile(), newDirectories);
        VirtualFile vfsDir = VfsUtil.findRelativeFile(currentDirectory.getVirtualFile(),
                                                      ArrayUtilRt.toStringArray(StringUtil.split(newDirectories, "/")));

        if (vfsDir == null) {
          Logger.getInstance(AbstractCreateFileFix.class)
            .warn("Unable to find relative file" + currentDirectory.getVirtualFile().getPath());
          return;
        }

        directory = currentDirectory.getManager().findDirectory(vfsDir);
        if (directory == null) throw new IOException("Couldn't create directory '" + newDirectories + "'");
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

  @Nullable
  protected String getFileText() {
    if (myFileTextSupplier != null) {
      return myFileTextSupplier.get();
    }
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
  protected void apply(@NotNull Project project, @NotNull PsiDirectory targetDirectory, @Nullable Editor editor)
    throws IncorrectOperationException {

    createFile(project, targetDirectory, myNewFileName);
  }
}
