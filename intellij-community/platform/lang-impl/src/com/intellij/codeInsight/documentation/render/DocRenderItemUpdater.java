// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

@Service
public final class DocRenderItemUpdater implements Runnable {
  private static final long MAX_UPDATE_DURATION_MS = 50;
  private final List<Inlay<DocRenderer>> myQueue = new ArrayList<>();

  static DocRenderItemUpdater getInstance() {
    return ApplicationManager.getApplication().getService(DocRenderItemUpdater.class);
  }

  boolean updateInlays(@NotNull Collection<Inlay<DocRenderer>> inlays) {
    if (inlays.isEmpty()) return false;
    boolean wasEmpty = myQueue.isEmpty();
    myQueue.addAll(0, inlays);
    return wasEmpty && processChunk();
  }

  @Override
  public void run() {
    processChunk();
  }

  private boolean processChunk() {
    long deadline = System.currentTimeMillis() + MAX_UPDATE_DURATION_MS;
    Map<Editor, EditorScrollingPositionKeeper> keepers = new HashMap<>();
    // This is a heuristic to lessen visual 'jumping' on editor opening. We'd like inlays visible at target opening location to be updated
    // first, and all the rest - later. We're not specifically optimizing for the case when multiple editors are opened simultaneously now,
    // opening several editors in succession should work fine with this logic though (by the time a new editor is opened, 'high-priority'
    // inlays from the previous editor are likely to have been processed already).
    myQueue.sort(Comparator.comparingInt(i -> -Math.abs(i.getOffset() - i.getEditor().getCaretModel().getOffset())));
    do {
      Inlay<DocRenderer> inlay = myQueue.remove(myQueue.size() - 1);
      if (inlay.isValid()) {
        Editor editor = inlay.getEditor();
        keepers.computeIfAbsent(editor, e -> {
          EditorScrollingPositionKeeper keeper = new EditorScrollingPositionKeeper(editor);
          keeper.savePosition();
          return keeper;
        });
        inlay.getRenderer().updateContent();
      }
    }
    while (!myQueue.isEmpty() && System.currentTimeMillis() < deadline);
    keepers.values().forEach(k -> k.restorePosition(false));
    if (!myQueue.isEmpty()) SwingUtilities.invokeLater(this);
    return !keepers.isEmpty();
  }
}
