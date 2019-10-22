// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight;

import com.intellij.codeInsight.completion.CompletionPhase;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.editorActions.CompletionAutoPopupHandler;
import com.intellij.codeInsight.hint.ShowParameterInfoHandler;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.testFramework.TestModeFlags;
import com.intellij.util.Alarm;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

public class AutoPopupControllerImpl extends AutoPopupController {
  private final Project myProject;
  private final Alarm myAlarm = new Alarm(this);

  public AutoPopupControllerImpl(Project project) {
    myProject = project;
    setupListeners();
  }

  private void setupListeners() {
    ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AnActionListener.TOPIC, new AnActionListener() {
      @Override
      public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
        cancelAllRequests();
      }

      @Override
      public void beforeEditorTyping(char c, @NotNull DataContext dataContext) {
        cancelAllRequests();
      }
    });

    IdeEventQueue.getInstance().addActivityListener(this::cancelAllRequests, this);
  }

  @Override
  public void autoPopupMemberLookup(final Editor editor, @Nullable final Condition<? super PsiFile> condition){
    autoPopupMemberLookup(editor, CompletionType.BASIC, condition);
  }

  @Override
  public void autoPopupMemberLookup(final Editor editor, CompletionType completionType, @Nullable final Condition<? super PsiFile> condition){
    scheduleAutoPopup(editor, completionType, condition);
  }

  @Override
  public void scheduleAutoPopup(@NotNull Editor editor, @NotNull CompletionType completionType, @Nullable final Condition<? super PsiFile> condition) {
    if (ApplicationManager.getApplication().isUnitTestMode() && !TestModeFlags.is(CompletionAutoPopupHandler.ourTestingAutopopup)) {
      return;
    }

    boolean alwaysAutoPopup = Boolean.TRUE.equals(editor.getUserData(ALWAYS_AUTO_POPUP));
    if (!CodeInsightSettings.getInstance().AUTO_POPUP_COMPLETION_LOOKUP && !alwaysAutoPopup) {
      return;
    }
    if (PowerSaveMode.isEnabled()) {
      return;
    }

    if (!CompletionServiceImpl.isPhase(CompletionPhase.CommittingDocuments.class, CompletionPhase.NoCompletion.getClass())) {
      return;
    }

    final CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    if (currentCompletion != null) {
      currentCompletion.closeAndFinish(true);
    }

    CompletionPhase.CommittingDocuments.scheduleAsyncCompletion(editor, completionType, condition, myProject, null);
  }

  @Override
  public void scheduleAutoPopup(final Editor editor) {
    scheduleAutoPopup(editor, CompletionType.BASIC, null);
  }

  private void addRequest(final Runnable request, final int delay) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!myAlarm.isDisposed()) myAlarm.addRequest(request, delay);
    });
  }

  @Override
  public void cancelAllRequests() {
    myAlarm.cancelAllRequests();
  }

  @Override
  public void autoPopupParameterInfo(@NotNull final Editor editor, @Nullable final PsiElement highlightedMethod){
    if (DumbService.isDumb(myProject)) return;
    if (PowerSaveMode.isEnabled()) return;

    ApplicationManager.getApplication().assertIsDispatchThread();
    final CodeInsightSettings settings = CodeInsightSettings.getInstance();
    if (settings.AUTO_POPUP_PARAMETER_INFO) {
      final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
      PsiFile file = documentManager.getPsiFile(editor.getDocument());
      if (file == null) return;

      if (!documentManager.isUncommited(editor.getDocument())) {
        file = documentManager.getPsiFile(InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file).getDocument());
        if (file == null) return;
      }

      Runnable request = () -> {
        if (!myProject.isDisposed() && !DumbService.isDumb(myProject) && !editor.isDisposed() &&
            (ApplicationManager.getApplication().isHeadlessEnvironment() || editor.getComponent().isShowing())) {
          int lbraceOffset = editor.getCaretModel().getOffset() - 1;
          try {
            PsiFile file1 = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
            if (file1 != null) {
              ShowParameterInfoHandler.invoke(myProject, editor, file1, lbraceOffset, highlightedMethod, false, true);
            }
          }
          catch (IndexNotReadyException ignored) { //anything can happen on alarm
          }
        }
      };

      addRequest(() -> documentManager.performLaterWhenAllCommitted(request), settings.PARAMETER_INFO_DELAY);
    }
  }

  @Override
  public void dispose() {
  }

  @Override
  @TestOnly
  public void waitForDelayedActions(long timeout, @NotNull TimeUnit unit) throws TimeoutException {
    long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
    while (System.currentTimeMillis() < deadline) {
      if (myAlarm.isEmpty()) return;
      LockSupport.parkNanos(10_000_000);
      UIUtil.dispatchAllInvocationEvents();
    }
    throw new TimeoutException();
  }

  /**
   * @deprecated can be emulated with {@link AppUIExecutor}
   */
  @Deprecated
  public static void runTransactionWithEverythingCommitted(@NotNull final Project project, @NotNull final Runnable runnable) {
    AppUIExecutor.onUiThread().later().withDocumentsCommitted(project).inTransaction(project).execute(runnable);
  }
}
