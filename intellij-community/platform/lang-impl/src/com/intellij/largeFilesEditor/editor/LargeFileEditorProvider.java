// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.impl.DefaultPlatformFileEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public final class LargeFileEditorProvider implements DefaultPlatformFileEditorProvider, DumbAware {
  public static final String PROVIDER_ID = "LargeFileEditorProvider";

  private static final String CARET_PAGE_NUMBER_ATTR = "caret-page-number";
  private static final String CARET_PAGE_SYMBOL_OFFSET_ATTR = "caret-page-symbol-offset";

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    return TextEditorProvider.isTextFile(file)
           && SingleRootFileViewProvider.isTooLargeForContentLoading(file)
           && (Experiments.getInstance().isFeatureEnabled("new.large.text.file.viewer")
               && !file.getFileType().isBinary()
               && file.isInLocalFileSystem());
  }

  @NotNull
  @Override
  public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    return new LargeFileEditorImpl(project, file);
  }

  @NotNull
  @Override
  public String getEditorTypeId() {
    return PROVIDER_ID;
  }

  @NotNull
  @Override
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.NONE;
  }

  @Override
  public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
    if (state instanceof LargeFileEditorState) {
      targetElement.setAttribute(CARET_PAGE_NUMBER_ATTR,
                                 Long.toString(((LargeFileEditorState)state).caretPageNumber));
      targetElement.setAttribute(CARET_PAGE_SYMBOL_OFFSET_ATTR,
                                 Integer.toString(((LargeFileEditorState)state).caretSymbolOffsetInPage));
    }
  }

  @NotNull
  @Override
  public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
    LargeFileEditorState state = new LargeFileEditorState();
    if (JDOMUtil.isEmpty(sourceElement)) {
      return state;
    }
    state.caretPageNumber = StringUtilRt.parseLong(sourceElement.getAttributeValue(CARET_PAGE_NUMBER_ATTR), 0);
    state.caretSymbolOffsetInPage = StringUtilRt.parseInt(sourceElement.getAttributeValue(CARET_PAGE_SYMBOL_OFFSET_ATTR), 0);
    return state;
  }
}
