// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class CodeInsightUtilBase extends CodeInsightUtilCore {
  private CodeInsightUtilBase() {
  }

  @Override
  public boolean prepareFileForWrite(@Nullable final PsiFile psiFile) {
    if (psiFile == null) return false;
    final VirtualFile file = psiFile.getVirtualFile();
    final Project project = psiFile.getProject();

    if (ReadonlyStatusHandler.ensureFilesWritable(project, file)) {
      return true;
    }
    ApplicationManager.getApplication().invokeLater(() -> {
      final Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, file), true);
      if (editor != null && editor.getComponent().isDisplayable()) {
        HintManager.getInstance().showErrorHint(editor, CodeInsightBundle.message("error.hint.file.is.readonly", file.getPresentableUrl()));
      }
    }, project.getDisposed());

    return false;
  }

  @Override
  public boolean preparePsiElementForWrite(@Nullable PsiElement element) {
    PsiFile file = element == null ? null : element.getContainingFile();
    return prepareFileForWrite(file);
  }

  @Override
  public boolean preparePsiElementsForWrite(@NotNull PsiElement... elements) {
    return preparePsiElementsForWrite(Arrays.asList(elements));
  }

  @Override
  public boolean preparePsiElementsForWrite(@NotNull Collection<? extends PsiElement> elements) {
    if (elements.isEmpty()) return true;
    Set<VirtualFile> files = new THashSet<>();
    Project project = null;
    for (PsiElement element : elements) {
      if (element == null) continue;
      PsiFile file = element.getContainingFile();
      if (file == null || !file.isPhysical()) continue;
      project = file.getProject();
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile == null) continue;
      files.add(virtualFile);
    }
    if (!files.isEmpty()) {
      ReadonlyStatusHandler.OperationStatus status = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files);
      return !status.hasReadonlyFiles();
    }
    return true;
  }

  @Override
  public boolean prepareVirtualFilesForWrite(@NotNull Project project, @NotNull Collection<VirtualFile> files) {
    ReadonlyStatusHandler.OperationStatus status = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files);
    return !status.hasReadonlyFiles();
  }

  // returns true on success
  @Deprecated
  public static boolean prepareEditorForWrite(@NotNull Editor editor) {
    return EditorModificationUtil.checkModificationAllowed(editor);
  }
}
