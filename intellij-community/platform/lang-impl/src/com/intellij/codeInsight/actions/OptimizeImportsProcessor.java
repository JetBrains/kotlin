// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.LanguageImportStatements;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;

import static com.intellij.codeInsight.actions.OptimizeImportsProcessor.NotificationInfo.NOTHING_CHANGED_NOTIFICATION;
import static com.intellij.codeInsight.actions.OptimizeImportsProcessor.NotificationInfo.SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION;

public class OptimizeImportsProcessor extends AbstractLayoutCodeProcessor {
  private static final String PROGRESS_TEXT = CodeInsightBundle.message("progress.text.optimizing.imports");
  public static final String COMMAND_NAME = CodeInsightBundle.message("process.optimize.imports");
  private final List<NotificationInfo> myOptimizerNotifications = new SmartList<>();

  public OptimizeImportsProcessor(@NotNull Project project) {
    super(project, COMMAND_NAME, PROGRESS_TEXT, false);
  }

  public OptimizeImportsProcessor(@NotNull Project project, Module module) {
    super(project, module, COMMAND_NAME, PROGRESS_TEXT, false);
  }

  public OptimizeImportsProcessor(@NotNull Project project, PsiDirectory directory, boolean includeSubdirs) {
    super(project, directory, includeSubdirs, PROGRESS_TEXT, COMMAND_NAME, false);
  }

  public OptimizeImportsProcessor(@NotNull Project project, PsiDirectory directory, boolean includeSubdirs, boolean processOnlyVcsChangedFiles) {
    super(project, directory, includeSubdirs, PROGRESS_TEXT, COMMAND_NAME, processOnlyVcsChangedFiles);
  }

  public OptimizeImportsProcessor(@NotNull Project project, PsiFile file) {
    super(project, file, PROGRESS_TEXT, COMMAND_NAME, false);
  }

  public OptimizeImportsProcessor(@NotNull Project project, PsiFile[] files, Runnable postRunnable) {
    this(project, files, COMMAND_NAME, postRunnable);
  }

  public OptimizeImportsProcessor(@NotNull Project project, PsiFile[] files, String commandName, Runnable postRunnable) {
    super(project, files, PROGRESS_TEXT, commandName, postRunnable, false);
  }

  public OptimizeImportsProcessor(@NotNull AbstractLayoutCodeProcessor processor) {
    super(processor, COMMAND_NAME, PROGRESS_TEXT);
  }

  @Override
  @NotNull
  protected FutureTask<Boolean> prepareTask(@NotNull PsiFile file, boolean processChangedTextOnly) {
    if (DumbService.isDumb(file.getProject())) {
      return new FutureTask<>(EmptyRunnable.INSTANCE, true);
    }

    final Set<ImportOptimizer> optimizers = LanguageImportStatements.INSTANCE.forFile(file);
    final List<Runnable> runnables = new ArrayList<>();
    List<PsiFile> files = file.getViewProvider().getAllFiles();
    for (ImportOptimizer optimizer : optimizers) {
      for (PsiFile psiFile : files) {
        if (optimizer.supports(psiFile)) {
          runnables.add(optimizer.processFile(psiFile));
        }
      }
    }

    Runnable runnable = !runnables.isEmpty() ? () -> {
      CodeStyleManagerImpl.setSequentialProcessingAllowed(false);
      try {
        for (Runnable runnable1 : runnables) {
          runnable1.run();
          retrieveAndStoreNotificationInfo(runnable1);
        }
        putNotificationInfoIntoCollector();
      }
      finally {
        CodeStyleManagerImpl.setSequentialProcessingAllowed(true);
      }
    } : EmptyRunnable.getInstance();

    return new FutureTask<>(runnable, true);
  }

  private void retrieveAndStoreNotificationInfo(@NotNull Runnable runnable) {
    if (runnable instanceof ImportOptimizer.CollectingInfoRunnable) {
      String optimizerMessage = ((ImportOptimizer.CollectingInfoRunnable)runnable).getUserNotificationInfo();
      myOptimizerNotifications.add(optimizerMessage != null ? new NotificationInfo(optimizerMessage) : NOTHING_CHANGED_NOTIFICATION);
    }
    else if (runnable == EmptyRunnable.getInstance()) {
      myOptimizerNotifications.add(NOTHING_CHANGED_NOTIFICATION);
    }
    else {
      myOptimizerNotifications.add(SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION);
    }
  }

  private void putNotificationInfoIntoCollector() {
    LayoutCodeInfoCollector collector = getInfoCollector();
    if (collector == null) {
      return;
    }

    boolean atLeastOneOptimizerChangedSomething = false;
    for (NotificationInfo info : myOptimizerNotifications) {
      atLeastOneOptimizerChangedSomething |= info.isSomethingChanged();
      if (info.getMessage() != null) {
        collector.setOptimizeImportsNotification(info.getMessage());
        return;
      }
    }

    collector.setOptimizeImportsNotification(atLeastOneOptimizerChangedSomething ? "imports optimized" : null);
  }

  static class NotificationInfo {
    public static final NotificationInfo NOTHING_CHANGED_NOTIFICATION = new NotificationInfo(false, null);
    public static final NotificationInfo SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION = new NotificationInfo(true, null);

    private final boolean mySomethingChanged;
    private final String myMessage;

    NotificationInfo(@NotNull String message) {
      this(true, message);
    }

    public boolean isSomethingChanged() {
      return mySomethingChanged;
    }

    public String getMessage() {
      return myMessage;
    }

    private NotificationInfo(boolean isSomethingChanged, @Nullable String message) {
      mySomethingChanged = isSomethingChanged;
      myMessage = message;
    }
  }
}
