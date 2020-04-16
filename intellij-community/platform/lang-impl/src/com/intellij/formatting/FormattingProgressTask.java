// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.SequentialModalProgressTask;
import com.intellij.util.SequentialTask;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FormattingProgressTask extends SequentialModalProgressTask implements FormattingProgressCallback {

  public static final ThreadLocal<Boolean> FORMATTING_CANCELLED_FLAG = ThreadLocal.withInitial(() -> false);

  private static final double MAX_PROGRESS_VALUE = 1;
  private static final double TOTAL_WEIGHT =
    Arrays.stream(FormattingStateId.values()).mapToDouble(FormattingStateId::getProgressWeight).sum();

  private final ConcurrentMap<EventType, Collection<Runnable>> myCallbacks = new ConcurrentHashMap<>();

  private final WeakReference<VirtualFile> myFile;
  private final WeakReference<Document>    myDocument;
  private final int                        myFileTextLength;

  private @NotNull FormattingStateId myLastState                       = FormattingStateId.WRAPPING_BLOCKS;
  private long              myDocumentModificationStampBefore = -1;

  private int myBlocksToModifyNumber;
  private int myModifiedBlocksNumber;

  public FormattingProgressTask(@Nullable Project project, @NotNull PsiFile file, @NotNull Document document) {
    super(project, getTitle(file));
    myFile = new WeakReference<>(file.getVirtualFile());
    myDocument = new WeakReference<>(document);
    myFileTextLength = file.getTextLength();
    addCallback(EventType.CANCEL, new MyCancelCallback());
  }

  private static @NotNull String getTitle(@NotNull PsiFile file) {
    VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      return CodeInsightBundle.message("reformat.progress.common.text");
    }
    else {
      return CodeInsightBundle.message("reformat.progress.file.with.known.name.text", virtualFile.getName());
    }
  }

  @Override
  protected void prepare(final @NotNull SequentialTask task) {
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
      Document document = myDocument.get();
      if (document != null) {
        myDocumentModificationStampBefore = document.getModificationStamp();
      }
      task.prepare();
    });
  }

  @Override
  public boolean addCallback(@NotNull EventType eventType, @NotNull Runnable callback) {
    return getCallbacks(eventType).add(callback);
  }

  @Override
  public void onSuccess() {
    for (Runnable callback : getCallbacks(EventType.SUCCESS)) {
      callback.run();
    }
  }

  @Override
  public void onCancel() {
    for (Runnable callback : getCallbacks(EventType.CANCEL)) {
      callback.run();
    }
  }

  @Override
  public void onThrowable(@NotNull Throwable error) {
    super.onThrowable(error);
    for (Runnable callback : getCallbacks(EventType.CANCEL)) {
      callback.run();
    }
  }

  private Collection<Runnable> getCallbacks(@NotNull EventType eventType) {
    Collection<Runnable> result = myCallbacks.get(eventType);
    if (result == null) {
      Collection<Runnable> candidate = myCallbacks.putIfAbsent(eventType, result = ContainerUtil.newConcurrentSet());
      if (candidate != null) {
        result = candidate;
      }
    }
    return result;
  }

  @Override
  public void afterWrappingBlock(@NotNull LeafBlockWrapper wrapped) {
    update(FormattingStateId.WRAPPING_BLOCKS, MAX_PROGRESS_VALUE * wrapped.getEndOffset() / myFileTextLength);
  }

  @Override
  public void afterProcessingBlock(@NotNull LeafBlockWrapper block) {
    update(FormattingStateId.PROCESSING_BLOCKS, MAX_PROGRESS_VALUE * block.getEndOffset() / myFileTextLength);
  }

  @Override
  public void beforeApplyingFormatChanges(@NotNull Collection<LeafBlockWrapper> modifiedBlocks) {
    myBlocksToModifyNumber = modifiedBlocks.size();
    updateTextIfNecessary(FormattingStateId.APPLYING_CHANGES);
    setCancelText(IdeBundle.message("action.stop"));
  }

  @Override
  public void afterApplyingChange(@NotNull LeafBlockWrapper block) {
    if (myModifiedBlocksNumber++ >= myBlocksToModifyNumber) {
      return;
    }

    update(FormattingStateId.APPLYING_CHANGES, MAX_PROGRESS_VALUE * myModifiedBlocksNumber / myBlocksToModifyNumber);
  }

  private void update(@NotNull FormattingStateId state, double completionRate) {
    ProgressIndicator indicator = getIndicator();
    if (indicator == null) {
      return;
    }

    updateTextIfNecessary(state);

    myLastState = state;
    double newFraction = 0;
    for (FormattingStateId prevState : state.getPreviousStates()) {
      newFraction += MAX_PROGRESS_VALUE * prevState.getProgressWeight() / TOTAL_WEIGHT;
    }
    newFraction += completionRate * state.getProgressWeight() / TOTAL_WEIGHT;

    double currentFraction = indicator.getFraction();
    if (newFraction - currentFraction < MAX_PROGRESS_VALUE / 100) {
      return;
    }

    indicator.setFraction(newFraction);
  }

  private void updateTextIfNecessary(@NotNull FormattingStateId currentState) {
    ProgressIndicator indicator = getIndicator();
    if (myLastState != currentState && indicator != null) {
      indicator.setText(currentState.getDescription());
    }
  }

  private class MyCancelCallback implements Runnable {
    @Override
    public void run() {
      FORMATTING_CANCELLED_FLAG.set(true);
      VirtualFile file = myFile.get();
      Document document = myDocument.get();
      if (file == null || document == null || myDocumentModificationStampBefore < 0) {
        return;
      }
      FileEditor editor = FileEditorManager.getInstance(myProject).getSelectedEditor(file);
      if (editor == null) {
        return;
      }

      UndoManager manager = UndoManager.getInstance(myProject);
      while (manager.isUndoAvailable(editor) && document.getModificationStamp() != myDocumentModificationStampBefore) {
        manager.undo(editor);
      }
    }
  }
}
