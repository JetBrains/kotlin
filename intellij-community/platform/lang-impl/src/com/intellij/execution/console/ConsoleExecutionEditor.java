// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.DocumentUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.editor.actions.IncrementalFindAction.SEARCH_DISABLED;

public class ConsoleExecutionEditor implements Disposable {
  private final EditorEx myConsoleEditor;
  private EditorEx myCurrentEditor;
  private final Document myEditorDocument;
  private final LanguageConsoleImpl.Helper myHelper;
  private final MessageBusConnection myBusConnection;
  private final ConsolePromptDecorator myConsolePromptDecorator;

  public ConsoleExecutionEditor(@NotNull LanguageConsoleImpl.Helper helper)  {
    myHelper = helper;
    EditorFactory editorFactory = EditorFactory.getInstance();
    myEditorDocument = helper.getDocument();
    myConsoleEditor = (EditorEx)editorFactory.createEditor(myEditorDocument, helper.project);
    myConsoleEditor.getScrollPane().getHorizontalScrollBar().setEnabled(false);
    myConsoleEditor.addFocusListener(myFocusListener);
    myConsoleEditor.getSettings().setVirtualSpace(false);
    myCurrentEditor = myConsoleEditor;
    myConsoleEditor.putUserData(SEARCH_DISABLED, true);

    myConsolePromptDecorator = new ConsolePromptDecorator(myConsoleEditor);
    myConsoleEditor.getGutter().registerTextAnnotation(myConsolePromptDecorator);

    myBusConnection = getProject().getMessageBus().connect();
    // action shortcuts are not yet registered
    ApplicationManager.getApplication().invokeLater(() -> installEditorFactoryListener(), getProject().getDisposed());
  }

  private final FocusChangeListener myFocusListener = new FocusChangeListener() {
    @Override
    public void focusGained(@NotNull Editor editor) {
      myCurrentEditor = (EditorEx)editor;
      if (GeneralSettings.getInstance().isSaveOnFrameDeactivation()) {
        TransactionGuard.submitTransaction(ConsoleExecutionEditor.this, () -> FileDocumentManager.getInstance().saveAllDocuments()); // PY-12487
      }
    }

    @Override
    public void focusLost(@NotNull Editor editor) {
    }
  };

  public void initComponent() {
    myConsoleEditor.setContextMenuGroupId(IdeActions.GROUP_CONSOLE_EDITOR_POPUP);
    myConsoleEditor.setHighlighter(
      EditorHighlighterFactory.getInstance().createEditorHighlighter(getVirtualFile(), myConsoleEditor.getColorsScheme(), getProject()));
    myConsolePromptDecorator.update();
  }

  @NotNull
  public final VirtualFile getVirtualFile() {
    return myHelper.virtualFile;
  }

  public EditorEx getEditor() {
    return myConsoleEditor;
  }

  @NotNull
  public EditorEx getCurrentEditor() {
    return ObjectUtils.notNull(myCurrentEditor, myConsoleEditor);
  }

  public Document getDocument() {
    return myEditorDocument;
  }

  public JComponent getComponent() {
    return myConsoleEditor.getComponent();
  }

  @NotNull
  public ConsolePromptDecorator getConsolePromptDecorator() {
    return myConsolePromptDecorator;
  }

  public void setConsoleEditorEnabled(boolean consoleEditorEnabled) {
    if (isConsoleEditorEnabled() == consoleEditorEnabled) {
      return;
    }
    if (consoleEditorEnabled) {
      FileEditorManager.getInstance(getProject()).closeFile(getVirtualFile());
      myCurrentEditor = myConsoleEditor;
    }
    myConsoleEditor.getComponent().setVisible(consoleEditorEnabled);
  }

  private Project getProject() {
    return myHelper.project;
  }

  public final boolean isConsoleEditorEnabled() {
    return myConsoleEditor.getComponent().isVisible();
  }

  @Nullable
  public String getPrompt() {
    return myConsolePromptDecorator.getMainPrompt();
  }

  @NotNull
  public ConsoleViewContentType getPromptAttributes() {
    return myConsolePromptDecorator.getPromptAttributes();
  }

  public void setPromptAttributes(@NotNull ConsoleViewContentType textAttributes) {
    myConsolePromptDecorator.setPromptAttributes(textAttributes);
  }

  public void setPrompt(@Nullable String prompt) {
    // always add space to the prompt otherwise it may look ugly
    setPromptInner(prompt != null && !prompt.endsWith(" ") ? prompt + " " : prompt);
  }


  public void setEditable(boolean editable) {
    myConsoleEditor.setRendererMode(!editable);
    myConsolePromptDecorator.update();
  }

  public boolean isEditable() {
    return !myConsoleEditor.isRendererMode();
  }


  private void setPromptInner(@Nullable final String prompt) {
    if (!myConsoleEditor.isDisposed()) {
      myConsolePromptDecorator.setMainPrompt(prompt != null ? prompt : "");
    }
  }

  private void installEditorFactoryListener() {
    FileEditorManagerListener fileEditorListener = new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (myConsoleEditor == null || !Comparing.equal(file, getVirtualFile())) {
          return;
        }

        Editor selectedTextEditor = source.getSelectedTextEditor();
        for (FileEditor fileEditor : source.getAllEditors(file)) {
          if (!(fileEditor instanceof TextEditor)) {
            continue;
          }

          final EditorEx editor = (EditorEx)((TextEditor)fileEditor).getEditor();
          editor.addFocusListener(myFocusListener);
          if (selectedTextEditor == editor) { // already focused
            myCurrentEditor = editor;
          }
          EmptyAction.registerActionShortcuts(editor.getComponent(), myConsoleEditor.getComponent());
        }
      }

      @Override
      public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (!Comparing.equal(file, getVirtualFile())) {
          return;
        }
        if (!Boolean.TRUE.equals(file.getUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN))) {
          if (myCurrentEditor != null && myCurrentEditor.isDisposed()) {
            myCurrentEditor = null;
          }
        }
      }
    };
    myBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorListener);
    FileEditorManager editorManager = FileEditorManager.getInstance(getProject());
    if (editorManager.isFileOpen(getVirtualFile())) {
      fileEditorListener.fileOpened(editorManager, getVirtualFile());
    }
  }

  @Override
  public void dispose() {
    myBusConnection.deliverImmediately();
    Disposer.dispose(myBusConnection);
    EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.releaseEditor(myConsoleEditor);

  }

  public void setInputText(@NotNull final String query) {
    DocumentUtil.writeInRunUndoTransparentAction(() -> myConsoleEditor.getDocument().setText(StringUtil.convertLineSeparators(query)));
  }
}
