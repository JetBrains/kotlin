// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.engine;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.TextChange;
import com.intellij.openapi.editor.impl.BulkChangesMerger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CaretOffsetUpdater {
  private final Map<Editor, Integer> myCaretOffsets = new HashMap<>();

  CaretOffsetUpdater(@NotNull Document document) {
    EditorFactory.getInstance().editors(document).forEach(editor -> myCaretOffsets.put(editor, editor.getCaretModel().getOffset()));
  }

  void update(@NotNull List<? extends TextChange> changes) {
    BulkChangesMerger merger = BulkChangesMerger.INSTANCE;
    for (Map.Entry<Editor, Integer> entry : myCaretOffsets.entrySet()) {
      entry.setValue(merger.updateOffset(entry.getValue(), changes));
    }
  }

  void restoreCaretLocations() {
    for (Map.Entry<Editor, Integer> entry : myCaretOffsets.entrySet()) {
      entry.getKey().getCaretModel().moveToOffset(entry.getValue());
    }
  }
}