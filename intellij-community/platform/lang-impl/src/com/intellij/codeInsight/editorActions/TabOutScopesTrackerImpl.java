// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.DocumentBulkUpdateListener;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TabOutScopesTrackerImpl implements TabOutScopesTracker {
  @Override
  public void registerEmptyScope(@NotNull Editor editor, int offset) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    assert !editor.isDisposed() : "Disposed editor";

    if (!CodeInsightSettings.getInstance().TAB_EXITS_BRACKETS_AND_QUOTES) return;

    if (editor instanceof EditorWindow) {
      DocumentWindow documentWindow = ((EditorWindow)editor).getDocument();
      offset = documentWindow.injectedToHost(offset);
      editor = ((EditorWindow)editor).getDelegate();
    }
    if (!(editor instanceof EditorImpl)) return;

    Tracker tracker = Tracker.forEditor((EditorImpl)editor, true);
    tracker.registerScope(offset);
  }

  @Override
  public boolean hasScopeEndingAt(@NotNull Editor editor, int offset) {
    return checkOrRemoveScopeEndingAt(editor, offset, false);
  }

  @Override
  public boolean removeScopeEndingAt(@NotNull Editor editor, int offset) {
    return checkOrRemoveScopeEndingAt(editor, offset, true);
  }

  private static boolean checkOrRemoveScopeEndingAt(@NotNull Editor editor, int offset, boolean removeScope) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (!CodeInsightSettings.getInstance().TAB_EXITS_BRACKETS_AND_QUOTES) return false;

    if (editor instanceof EditorWindow) {
      DocumentWindow documentWindow = ((EditorWindow)editor).getDocument();
      offset = documentWindow.injectedToHost(offset);
      editor = ((EditorWindow)editor).getDelegate();
    }
    if (!(editor instanceof EditorImpl)) return false;

    Tracker tracker = Tracker.forEditor((EditorImpl)editor, false);
    if (tracker == null) return false;

    return tracker.hasScopeEndingAt(offset, removeScope);
  }

  private static class Tracker extends DocumentBulkUpdateListener.Adapter implements DocumentListener {
    private static final Key<Tracker> TRACKER = Key.create("tab.out.scope.tracker");
    private static final Key<List<RangeMarker>> TRACKED_SCOPES = Key.create("tab.out.scopes");

    private final Editor myEditor;

    private static Tracker forEditor(@NotNull EditorImpl editor, boolean createIfAbsent) {
      Tracker tracker = editor.getUserData(TRACKER);
      if (tracker == null && createIfAbsent) {
        editor.putUserData(TRACKER, tracker = new Tracker(editor));
      }
      return tracker;
    }

    private Tracker(@NotNull EditorImpl editor) {
      myEditor = editor;
      Disposable editorDisposable = editor.getDisposable();
      myEditor.getDocument().addDocumentListener(this, editorDisposable);
      ApplicationManager.getApplication().getMessageBus().connect(editorDisposable).subscribe(DocumentBulkUpdateListener.TOPIC, this);
    }

    private List<RangeMarker> getCurrentScopes(boolean create) {
      Caret currentCaret = myEditor.getCaretModel().getCurrentCaret();
      List<RangeMarker> result = currentCaret.getUserData(TRACKED_SCOPES);
      if (result == null && create) {
        currentCaret.putUserData(TRACKED_SCOPES, result = new ArrayList<>());
      }
      return result;
    }

    private void registerScope(int offset) {
      RangeMarker marker = myEditor.getDocument().createRangeMarker(offset, offset);
      marker.setGreedyToLeft(true);
      marker.setGreedyToRight(true);
      getCurrentScopes(true).add(marker);
    }

    private boolean hasScopeEndingAt(int offset, boolean remove) {
      List<RangeMarker> scopes = getCurrentScopes(false);
      if (scopes == null) return false;
      for (Iterator<RangeMarker> it = scopes.iterator(); it.hasNext(); ) {
        RangeMarker scope = it.next();
        if (offset == scope.getEndOffset()) {
          if (remove) it.remove();
          return true;
        }
      }
      return false;
    }

    @Override
    public void beforeDocumentChange(@NotNull DocumentEvent event) {
      List<RangeMarker> scopes = getCurrentScopes(false);
      if (scopes == null) return;
      int caretOffset = myEditor.getCaretModel().getOffset();
      int changeStart = event.getOffset();
      int changeEnd = event.getOffset() + event.getOldLength();
      for (Iterator<RangeMarker> it = scopes.iterator(); it.hasNext(); ) {
        RangeMarker scope = it.next();
        // We don't reset scope if the change is completely inside our scope, or if caret is inside, but the change is outside
        if ((changeStart < scope.getStartOffset() || changeEnd > scope.getEndOffset()) &&
            (caretOffset < scope.getStartOffset() || caretOffset > scope.getEndOffset() ||
             (changeEnd >= scope.getStartOffset() && changeStart <= scope.getEndOffset()))) {
          it.remove();
        }
      }
    }

    @Override
    public void updateStarted(@NotNull Document doc) {
      for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
        caret.putUserData(TRACKED_SCOPES, null);
      }
    }
  }
}
