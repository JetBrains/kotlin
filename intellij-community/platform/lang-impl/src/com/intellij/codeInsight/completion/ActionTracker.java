// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
class ActionTracker {
  private boolean myActionsHappened;
  private final Editor myEditor;
  private final Project myProject;
  private boolean myIgnoreDocumentChanges;

  ActionTracker(Editor editor, Disposable parentDisposable) {
    myEditor = editor;
    myProject = editor.getProject();
    ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(AnActionListener.TOPIC, new AnActionListener() {
      @Override
      public void beforeEditorTyping(char c, @NotNull DataContext dataContext) {
        myActionsHappened = true;
      }

      @Override
      public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
        myActionsHappened = true;
      }
    });
    myEditor.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent e) {
        if (!myIgnoreDocumentChanges) {
          myActionsHappened = true;
        }
      }
    }, parentDisposable);
    myEditor.getCaretModel().addCaretListener(new CaretListener() {
      @Override
      public void caretAdded(@NotNull CaretEvent event) {
        myActionsHappened = true;
      }

      @Override
      public void caretRemoved(@NotNull CaretEvent event) {
        myActionsHappened = true;
      }

      @Override
      public void caretPositionChanged(@NotNull CaretEvent event) {
        if (!myIgnoreDocumentChanges) {
          myActionsHappened = true;
        }
      }
    }, parentDisposable);
  }

  void ignoreCurrentDocumentChange() {
    if (CommandProcessor.getInstance().getCurrentCommand() == null) {
      return;
    }

    myIgnoreDocumentChanges = true;
    final Disposable disposable = Disposer.newDisposable();
    Disposer.register(myProject, disposable);
    myProject.getMessageBus().connect(disposable).subscribe(CommandListener.TOPIC, new CommandListener() {
      @Override
      public void commandFinished(@NotNull CommandEvent event) {
        Disposer.dispose(disposable);
        myIgnoreDocumentChanges = false;
      }
    });
  }

  boolean hasAnythingHappened() {
    return myActionsHappened || DumbService.getInstance(myProject).isDumb() ||
           myEditor.isDisposed() ||
           (myEditor instanceof EditorWindow && !((EditorWindow)myEditor).isValid());
  }
}
