/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.hints;

import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.util.Key;
import com.intellij.util.DocumentUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class ParameterHintsUpdater {
  private static final Key<Boolean> HINT_REMOVAL_DELAYED = Key.create("hint.removal.delayed");
  private static final Key<Boolean> REPEATED_PASS = Key.create("RepeatedParameterHintsPass");

  private final ParameterHintsPresentationManager myHintsManager = ParameterHintsPresentationManager.getInstance();
  private final TIntObjectHashMap<Caret> myCaretMap;
  
  private final TIntObjectHashMap<List<ParameterHintsPass.HintData>> myNewHints;
  private final TIntObjectHashMap<String> myHintsToPreserve;
  private final boolean myForceImmediateUpdate;

  private final Editor myEditor;
  private final List<Inlay> myEditorInlays;
  private List<InlayUpdateInfo> myUpdateList;

  public ParameterHintsUpdater(@NotNull Editor editor,
                               @NotNull List<Inlay> editorInlays,
                               @NotNull TIntObjectHashMap<List<ParameterHintsPass.HintData>> newHints,
                               @NotNull TIntObjectHashMap<String> hintsToPreserve, 
                               boolean forceImmediateUpdate) {
    myEditor = editor;
    myNewHints = newHints;
    myHintsToPreserve = hintsToPreserve;
    myForceImmediateUpdate = forceImmediateUpdate;

    myCaretMap = new TIntObjectHashMap<>();
    List<Caret> allCarets = myEditor.getCaretModel().getAllCarets();
    allCarets.forEach((caret) -> myCaretMap.put(caret.getOffset(), caret));

    myEditorInlays = editorInlays;
  }
  
  
  private List<InlayUpdateInfo> getInlayUpdates(List<Inlay> editorHints) {
    myEditor.putUserData(HINT_REMOVAL_DELAYED, Boolean.FALSE);

    List<InlayUpdateInfo> updates = ContainerUtil.newArrayList();

    editorHints.forEach(editorHint -> {
      int offset = editorHint.getOffset();
      ParameterHintsPass.HintData newHint = findAndRemoveMatchingHint(offset, editorHint.isRelatedToPrecedingText(), myNewHints);
      if (!myForceImmediateUpdate && delayRemoval(editorHint)) {
        myEditor.putUserData(HINT_REMOVAL_DELAYED, Boolean.TRUE);
        return;
      }
      String newText = newHint == null ? null : newHint.presentationText;
      if (isPreserveHint(editorHint, newText)) return;
      updates.add(new InlayUpdateInfo(offset, editorHint, newHint));
    });

    Arrays.stream(myNewHints.keys()).forEach((offset) -> {
      for (ParameterHintsPass.HintData hint : myNewHints.get(offset)) {
        updates.add(new InlayUpdateInfo(offset, null, hint));
      }
    });

    updates.sort(Comparator.comparing((update) -> update.offset));
    return updates;
  }

  public static boolean hintRemovalDelayed(@NotNull Editor editor) {
    return editor.getUserData(HINT_REMOVAL_DELAYED) == Boolean.TRUE;
  }

  @Nullable
  private static ParameterHintsPass.HintData findAndRemoveMatchingHint(int offset, boolean relatesToPrecedingText,
                                                                       TIntObjectHashMap<List<ParameterHintsPass.HintData>> data) {
    List<ParameterHintsPass.HintData> newHintList = data.get(offset);
    ParameterHintsPass.HintData newHint = null;
    if (newHintList != null) {
      for (Iterator<ParameterHintsPass.HintData> iterator = newHintList.iterator(); iterator.hasNext(); ) {
        ParameterHintsPass.HintData hint = iterator.next();
        if (hint.relatesToPrecedingText == relatesToPrecedingText) {
          newHint = hint;
          iterator.remove();
          break;
        }
      }
      if (newHintList.isEmpty()) data.remove(offset);
    }
    return newHint;
  }
  
  private boolean isPreserveHint(@NotNull Inlay inlay, @Nullable String newText) {
    if (newText == null) {
      newText = myHintsToPreserve.get(inlay.getOffset());
    }
    String oldText = myHintsManager.getHintText(inlay);
    return Objects.equals(newText, oldText);
  }
  

  public void update() {
    myUpdateList = getInlayUpdates(myEditorInlays);
    boolean firstTime = myEditor.getUserData(REPEATED_PASS) == null;
    boolean isUpdateInBulkMode = myUpdateList.size() > 1000;
    DocumentUtil.executeInBulk(myEditor.getDocument(), isUpdateInBulkMode, () -> performHintsUpdate(firstTime, isUpdateInBulkMode));
    myEditor.putUserData(REPEATED_PASS, Boolean.TRUE);
  }

  private void performHintsUpdate(boolean firstTime, boolean isInBulkMode) {
    for (int infoIndex = 0; infoIndex < myUpdateList.size(); infoIndex++) {
      InlayUpdateInfo info = myUpdateList.get(infoIndex);
      String oldText = info.oldText;
      String newText = info.newText;

      InlayUpdateInfo.Action action = info.action();
      if (action == InlayUpdateInfo.Action.ADD) {
        boolean useAnimation = !myForceImmediateUpdate && !firstTime && !isSameHintRemovedNear(newText, infoIndex) && !isInBulkMode;
        Inlay inlay = myHintsManager.addHint(myEditor, info.offset, info.relatesToPrecedingText, newText, info.widthAdjustment, useAnimation);
        if (inlay != null && !((DocumentEx)myEditor.getDocument()).isInBulkUpdate()) {
          VisualPosition inlayPosition = inlay.getVisualPosition();
          VisualPosition visualPosition = new VisualPosition(inlayPosition.line, 
                                                             inlayPosition.column + (info.relatesToPrecedingText ? 1 : 0));
          Caret caret = myEditor.getCaretModel().getCaretAt(visualPosition);
          if (caret != null) caret.moveToVisualPosition(new VisualPosition(inlayPosition.line, 
                                                                           inlayPosition.column + (info.relatesToPrecedingText ? 0 : 1)));
        }
      }
      else if (action == InlayUpdateInfo.Action.DELETE) {
        boolean useAnimation = !myForceImmediateUpdate && oldText != null && !isSameHintAddedNear(oldText, infoIndex) && !isInBulkMode;
        myHintsManager.deleteHint(myEditor, info.inlay, useAnimation);
      }
      else if (action == InlayUpdateInfo.Action.REPLACE) {
        myHintsManager.replaceHint(myEditor, info.inlay, newText, info.widthAdjustment, !myForceImmediateUpdate);
      }
    }
  }

  private boolean isSameHintRemovedNear(@NotNull String text, int index) {
    return getInfosNear(index).anyMatch((info) -> text.equals(info.oldText));
  }


  private boolean isSameHintAddedNear(@NotNull String text, int index) {
    return getInfosNear(index).anyMatch((info) -> text.equals(info.newText));
  }
  
  
  private Stream<InlayUpdateInfo> getInfosNear(int index) {
    List<InlayUpdateInfo> result = ContainerUtil.newArrayList();
    if (index > 0) {
      result.add(myUpdateList.get(index - 1));
    }
    if (index + 1 < myUpdateList.size()) {
      result.add(myUpdateList.get(index + 1));
    }
    return result.stream();
  }


  private boolean delayRemoval(Inlay inlay) {
    int offset = inlay.getOffset();
    Caret caret = myCaretMap.get(offset);
    if (caret == null) return false;
    CharSequence text = myEditor.getDocument().getImmutableCharSequence();
    if (offset >= text.length()) return false;
    char afterCaret = text.charAt(offset);
    if (afterCaret != ',' && afterCaret != ')') return false;
    VisualPosition afterInlayPosition = myEditor.offsetToVisualPosition(offset, true, false);
    // check whether caret is to the right of inlay
    if (!caret.getVisualPosition().equals(afterInlayPosition)) return false;
    return true;
  }

  
  private static class InlayUpdateInfo {
    public enum Action {
      ADD, DELETE, REPLACE, SKIP
    }

    public final int offset;
    public final Inlay inlay;
    public final String newText;
    public final String oldText;
    public final boolean relatesToPrecedingText;
    public final HintWidthAdjustment widthAdjustment;

    InlayUpdateInfo(int offset, @Nullable Inlay current, @Nullable ParameterHintsPass.HintData newHintData) {
      this.offset = offset;
      inlay = current;
      oldText = inlay == null ? null : ParameterHintsPresentationManager.getInstance().getHintText(inlay);
      if (newHintData == null) {
        newText = null;
        relatesToPrecedingText = false;
        widthAdjustment = null;
      }
      else {
        newText = newHintData.presentationText;
        relatesToPrecedingText = newHintData.relatesToPrecedingText;
        widthAdjustment = newHintData.widthAdjustment;
      }
    }

    public Action action() {
      if (inlay == null) {
        return newText != null ? Action.ADD : Action.SKIP;
      }
      else {
        return newText != null ? Action.REPLACE : Action.DELETE;
      }
    }
  }
}