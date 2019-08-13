// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

class CaretModelWindow implements CaretModel {
  private final CaretModel myDelegate;
  private final EditorEx myHostEditor;
  private final EditorWindow myEditorWindow;
  private final Map<Caret, InjectedCaret> myInjectedCaretMap = new WeakHashMap<>();

  CaretModelWindow(CaretModel delegate, EditorWindow editorWindow) {
    myDelegate = delegate;
    myHostEditor = (EditorEx)editorWindow.getDelegate();
    myEditorWindow = editorWindow;
  }

  @Override
  public void moveCaretRelatively(final int columnShift,
                                  final int lineShift,
                                  final boolean withSelection,
                                  final boolean blockSelection,
                                  final boolean scrollToCaret) {
    myDelegate.moveCaretRelatively(columnShift, lineShift, withSelection, blockSelection, scrollToCaret);
  }

  @Override
  public void moveToLogicalPosition(@NotNull final LogicalPosition pos) {
    LogicalPosition hostPos = myEditorWindow.injectedToHost(pos);
    myDelegate.moveToLogicalPosition(hostPos);
  }

  @Override
  public void moveToVisualPosition(@NotNull final VisualPosition pos) {
    LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
    myDelegate.moveToLogicalPosition(hostPos);
  }

  @Override
  public void moveToOffset(int offset) {
    moveToOffset(offset, false);
  }

  @Override
  public void moveToOffset(final int offset, boolean locateBeforeSoftWrap) {
    int hostOffset = myEditorWindow.getDocument().injectedToHost(offset);
    myDelegate.moveToOffset(hostOffset, locateBeforeSoftWrap);
  }

  @Override
  @NotNull
  public LogicalPosition getLogicalPosition() {
    LogicalPosition hostPos = myDelegate.getLogicalPosition();
    return myEditorWindow.hostToInjected(hostPos);
  }

  @Override
  @NotNull
  public VisualPosition getVisualPosition() {
    LogicalPosition logicalPosition = getLogicalPosition();
    return myEditorWindow.logicalToVisualPosition(logicalPosition);
  }

  @Override
  public int getOffset() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getOffset());
  }

  @Override
  public boolean isUpToDate() {
    return myDelegate.isUpToDate();
  }

  private final ListenerWrapperMap<CaretListener> myCaretListeners = new ListenerWrapperMap<>();
  @Override
  public void addCaretListener(@NotNull final CaretListener listener) {
    CaretListener wrapper = new CaretListener() {
      @Override
      public void caretPositionChanged(@NotNull CaretEvent e) {
        if (!myEditorWindow.getDocument().isValid()) return; // injected document can be destroyed by now
        Caret caret = e.getCaret();
        assert caret != null;
        CaretEvent event = new CaretEvent(createInjectedCaret(caret),
                                          myEditorWindow.hostToInjected(e.getOldPosition()),
                                          myEditorWindow.hostToInjected(e.getNewPosition()));
        listener.caretPositionChanged(event);
      }
    };
    myCaretListeners.registerWrapper(listener, wrapper);
    myDelegate.addCaretListener(wrapper);
  }

  @Override
  public void removeCaretListener(@NotNull final CaretListener listener) {
    CaretListener wrapper = myCaretListeners.removeWrapper(listener);
    if (wrapper != null) {
      myDelegate.removeCaretListener(wrapper);
    }
  }

  public void disposeModel() {
    for (CaretListener wrapper : myCaretListeners.wrappers()) {
      myDelegate.removeCaretListener(wrapper);
    }
    myCaretListeners.clear();
  }

  @Override
  public int getVisualLineStart() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineStart());
  }

  @Override
  public int getVisualLineEnd() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineEnd());
  }

  @Override
  public TextAttributes getTextAttributes() {
    return myDelegate.getTextAttributes();
  }

  @Override
  public boolean supportsMultipleCarets() {
    return myDelegate.supportsMultipleCarets();
  }

  @NotNull
  @Override
  public Caret getCurrentCaret() {
    return createInjectedCaret(myDelegate.getCurrentCaret());
  }

  @NotNull
  @Override
  public Caret getPrimaryCaret() {
    return createInjectedCaret(myDelegate.getPrimaryCaret());
  }

  @Override
  public int getCaretCount() {
    return myDelegate.getCaretCount();
  }

  @NotNull
  @Override
  public List<Caret> getAllCarets() {
    List<Caret> hostCarets = myDelegate.getAllCarets();
    List<Caret> carets = new ArrayList<>(hostCarets.size());
    for (Caret hostCaret : hostCarets) {
      carets.add(createInjectedCaret(hostCaret));
    }
    return carets;
  }

  @Nullable
  @Override
  public Caret getCaretAt(@NotNull VisualPosition pos) {
    LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
    Caret caret = myDelegate.getCaretAt(myHostEditor.logicalToVisualPosition(hostPos));
    return createInjectedCaret(caret);
  }

  @Nullable
  @Override
  public Caret addCaret(@NotNull VisualPosition pos) {
    return addCaret(pos, true);
  }

  @Nullable
  @Override
  public Caret addCaret(@NotNull VisualPosition pos, boolean makePrimary) {
    LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
    Caret caret = myDelegate.addCaret(myHostEditor.logicalToVisualPosition(hostPos));
    return createInjectedCaret(caret);
  }

  @Override
  public boolean removeCaret(@NotNull Caret caret) {
    if (caret instanceof InjectedCaret) {
      caret = ((InjectedCaret)caret).myDelegate;
    }
    return myDelegate.removeCaret(caret);
  }

  @Override
  public void removeSecondaryCarets() {
    myDelegate.removeSecondaryCarets();
  }

  @Override
  public void setCaretsAndSelections(@NotNull List<? extends CaretState> caretStates) {
    List<CaretState> convertedStates = convertCaretStates(caretStates);
    myDelegate.setCaretsAndSelections(convertedStates);
  }

  @Override
  public void setCaretsAndSelections(@NotNull List<? extends CaretState> caretStates, boolean updateSystemSelection) {
    List<CaretState> convertedStates = convertCaretStates(caretStates);
    myDelegate.setCaretsAndSelections(convertedStates, updateSystemSelection);
  }

  private List<CaretState> convertCaretStates(List<? extends CaretState> caretStates) {
    List<CaretState> convertedStates = new ArrayList<>(caretStates.size());
    for (CaretState state : caretStates) {
      convertedStates.add(new CaretState(injectedToHost(state.getCaretPosition()),
                                         injectedToHost(state.getSelectionStart()),
                                         injectedToHost(state.getSelectionEnd())));
    }
    return convertedStates;
  }

  private LogicalPosition injectedToHost(@Nullable LogicalPosition position) {
    return position == null ? null : myEditorWindow.injectedToHost(position);
  }

  @NotNull
  @Override
  public List<CaretState> getCaretsAndSelections() {
    List<CaretState> caretsAndSelections = myDelegate.getCaretsAndSelections();
    List<CaretState> convertedStates = new ArrayList<>(caretsAndSelections.size());
    for (CaretState state : caretsAndSelections) {
      convertedStates.add(new CaretState(hostToInjected(state.getCaretPosition()),
                                         hostToInjected(state.getSelectionStart()),
                                         hostToInjected(state.getSelectionEnd())));
    }
    return convertedStates;
  }

  private LogicalPosition hostToInjected(@Nullable LogicalPosition position) {
    return position == null ? null : myEditorWindow.hostToInjected(position);
  }

  @Contract("null -> null; !null -> !null")
  private InjectedCaret createInjectedCaret(Caret caret) {
    if (caret == null) {
      return null;
    }
    synchronized (myInjectedCaretMap) {
      InjectedCaret injectedCaret = myInjectedCaretMap.get(caret);
      if (injectedCaret == null) {
        injectedCaret = new InjectedCaret(myEditorWindow, caret);
        myInjectedCaretMap.put(caret, injectedCaret);
      }
      return injectedCaret;
    }
  }

  @Override
  public void runForEachCaret(final @NotNull CaretAction action) {
    myDelegate.runForEachCaret(caret -> action.perform(createInjectedCaret(caret)));
  }

  @Override
  public void runForEachCaret(@NotNull final CaretAction action, boolean reverseOrder) {
    myDelegate.runForEachCaret(caret -> action.perform(createInjectedCaret(caret)), reverseOrder);
  }

  @Override
  public void addCaretActionListener(@NotNull CaretActionListener listener, @NotNull Disposable disposable) {
    myDelegate.addCaretActionListener(listener, disposable);
  }

  @Override
  public void runBatchCaretOperation(@NotNull Runnable runnable) {
    myDelegate.runBatchCaretOperation(runnable);
  }
}
