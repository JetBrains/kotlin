// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.highlighting;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.extensions.ExtensionPointUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class BraceHighlighter implements StartupActivity.DumbAware {
  private final Alarm myAlarm = new Alarm();

  @Override
  public void runActivity(@NotNull final Project project) {
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) return; // sorry, upsource

    Disposable activityDisposable = ExtensionPointUtil.createExtensionDisposable(this, StartupActivity.POST_STARTUP_ACTIVITY);
    Disposer.register(project, activityDisposable);

    final EditorEventMulticaster eventMulticaster = EditorFactory.getInstance().getEventMulticaster();

    eventMulticaster.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(@NotNull CaretEvent e) {
        if (e.getCaret() != e.getEditor().getCaretModel().getPrimaryCaret()) return;
        onCaretUpdate(e.getEditor(), project);
      }

      @Override
      public void caretAdded(@NotNull CaretEvent e) {
        if (e.getCaret() != e.getEditor().getCaretModel().getPrimaryCaret()) return;
        onCaretUpdate(e.getEditor(), project);
      }

      @Override
      public void caretRemoved(@NotNull CaretEvent e) {
        onCaretUpdate(e.getEditor(), project);
      }
    }, activityDisposable);

    final SelectionListener selectionListener = new SelectionListener() {
      @Override
      public void selectionChanged(@NotNull SelectionEvent e) {
        myAlarm.cancelAllRequests();
        Editor editor = e.getEditor();
        if (editor.getProject() != project) {
          return;
        }

        final TextRange oldRange = e.getOldRange();
        final TextRange newRange = e.getNewRange();
        if (oldRange != null && newRange != null && oldRange.isEmpty() == newRange.isEmpty()) {
          // Don't perform braces update in case of active/absent selection.
          return;
        }
        updateBraces(editor, myAlarm);
      }
    };
    eventMulticaster.addSelectionListener(selectionListener, activityDisposable);

    DocumentListener documentListener = new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent e) {
        myAlarm.cancelAllRequests();
        EditorFactory.getInstance().editors(e.getDocument(), project).forEach(editor -> updateBraces(editor, myAlarm));
      }
    };
    eventMulticaster.addDocumentListener(documentListener, activityDisposable);

    project.getMessageBus().connect(activityDisposable)
      .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent e) {
          myAlarm.cancelAllRequests();
          FileEditor oldEditor = e.getOldEditor();
          if (oldEditor instanceof TextEditor) {
            clearBraces(((TextEditor)oldEditor).getEditor());
          }
          FileEditor newEditor = e.getNewEditor();
          if (newEditor instanceof TextEditor) {
            updateBraces(((TextEditor)newEditor).getEditor(), myAlarm);
          }
        }
      });
  }

  private void onCaretUpdate(@NotNull Editor editor, @NotNull Project project) {
    myAlarm.cancelAllRequests();
    SelectionModel selectionModel = editor.getSelectionModel();
    // Don't update braces in case of the active selection.
    if (editor.getProject() != project || selectionModel.hasSelection()) {
      return;
    }
    updateBraces(editor, myAlarm);
  }

  private static void updateBraces(@NotNull Editor editor, @NotNull Alarm alarm) {
    if (editor.getDocument().isInBulkUpdate()) {
      return;
    }

    BraceHighlightingHandler.lookForInjectedAndMatchBracesInOtherThread(editor, alarm, handler -> {
      handler.updateBraces();
      return false;
    });
  }

  private void clearBraces(@NotNull Editor editor) {
    BraceHighlightingHandler.lookForInjectedAndMatchBracesInOtherThread(editor, myAlarm, handler -> {
      handler.clearBraceHighlighters();
      return false;
    });
  }

  @NotNull
  public static Alarm getAlarm() {
    return Objects.requireNonNull(POST_STARTUP_ACTIVITY.findExtension(BraceHighlighter.class)).myAlarm;
  }
}
