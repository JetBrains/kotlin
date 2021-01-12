// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author: Eugene Zhuravlev
 */
package com.intellij.compiler.progress;

import com.intellij.compiler.CompilerManagerImpl;
import com.intellij.compiler.impl.ExitStatus;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.AppIconScheme;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.pom.Navigatable;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.ui.AppIcon;
import com.intellij.ui.GuiUtils;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class CompilerTask extends Task.Backgroundable {
  private static final String APP_ICON_ID = "compiler";
  @NotNull
  private final Object myContentId;
  @NotNull
  private Object mySessionId;
  private final boolean myModal;
  private final boolean myHeadlessMode;
  private final boolean myForceAsyncExecution;
  private final boolean myWaitForPreviousSession;
  private int myErrorCount = 0;
  private int myWarningCount = 0;

  private volatile ProgressIndicator myIndicator = new EmptyProgressIndicator();
  private Runnable myCompileWork;
  private Runnable myRestartWork;
  private final boolean myCompilationStartedAutomatically;
  private final BuildViewService myBuildViewService;
  private long myStartCompilationStamp;
  private long myEndCompilationStamp;
  private ExitStatus myExitStatus;

  public CompilerTask(@NotNull Project project, String contentName, final boolean headlessMode, boolean forceAsync,
                      boolean waitForPreviousSession, boolean compilationStartedAutomatically) {
    this(project, contentName, headlessMode, forceAsync, waitForPreviousSession, compilationStartedAutomatically, false);
  }

  public CompilerTask(@NotNull Project project, String contentName, final boolean headlessMode, boolean forceAsync,
                      boolean waitForPreviousSession, boolean compilationStartedAutomatically, boolean modal) {
    super(project, contentName);
    myHeadlessMode = headlessMode;
    myForceAsyncExecution = forceAsync;
    myWaitForPreviousSession = waitForPreviousSession;
    myCompilationStartedAutomatically = compilationStartedAutomatically;
    myModal = modal;
    myContentId = new IDObject("content_id");
    mySessionId = myContentId; // by default sessionID should be unique, just as content ID

    if (Registry.is("ide.jps.use.build.tool.window", true)) {
      myBuildViewService = new BuildOutputService(project, contentName);
    } else {
      myBuildViewService = new CompilerMessagesService(project, myContentId, contentName, headlessMode);
    }
  }

  @NotNull
  public Object getSessionId() {
    return mySessionId;
  }

  public void setSessionId(@NotNull Object sessionId) {
    mySessionId = sessionId;
  }

  @NotNull
  public Object getContentId() {
    return myContentId;
  }

  public void setStartCompilationStamp(long startCompilationStamp) {
    myStartCompilationStamp = startCompilationStamp;
  }

  public void setEndCompilationStamp(ExitStatus exitStatus, long endCompilationStamp) {
    myExitStatus = exitStatus;
    myEndCompilationStamp = endCompilationStamp;
  }

  public void registerCloseAction(final Runnable onClose) {
    myBuildViewService.registerCloseAction(onClose);
  }

  @Override
  public boolean shouldStartInBackground() {
    return !myModal;
  }

  @Override
  public boolean isConditionalModal() {
    return myModal;
  }

  public ProgressIndicator getIndicator() {
    return myIndicator;
  }

  @NotNull
  @Override
  public NotificationInfo getNotificationInfo() {
    return new NotificationInfo(myErrorCount > 0 ? "Compiler (errors)" : "Compiler (success)",
                                JavaCompilerBundle.message("compilation.finished"),
                                JavaCompilerBundle.message("0.errors.1.warnings", myErrorCount, myWarningCount), true);
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    myIndicator = indicator;
    myIndicator.setIndeterminate(false);

    myBuildViewService.onStart(mySessionId, myStartCompilationStamp, myRestartWork, indicator);
    Semaphore semaphore = ((CompilerManagerImpl)CompilerManager.getInstance(myProject)).getCompilationSemaphore();
    boolean acquired = false;
    try {
      try {
        while (!acquired) {
          acquired = semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);
          if (!acquired && !myWaitForPreviousSession) {
            return;
          }
          if (indicator.isCanceled()) {
            // give up obtaining the semaphore,
            // let compile work begin in order to stop gracefuly on cancel event
            break;
          }
        }
      }
      catch (InterruptedException ignored) {
      }

      if (!isHeadless()) {
        addIndicatorDelegate();
      }
      myCompileWork.run();
    }
    catch (ProcessCanceledException ignored) {
    }
    finally {
      try {
        indicator.stop();
        myBuildViewService.onEnd(mySessionId, myExitStatus, myEndCompilationStamp);
      }
      finally {
        if (acquired) {
          semaphore.release();
        }
      }
    }
  }

  private void addIndicatorDelegate() {
    ProgressIndicator indicator = myIndicator;
    if (!(indicator instanceof ProgressIndicatorEx)) {
      return;
    }
    ((ProgressIndicatorEx)indicator).addStateDelegate(new AbstractProgressIndicatorExBase() {

      @Override
      public void cancel() {
        super.cancel();
        stopAppIconProgress();
      }

      @Override
      public void stop() {
        super.stop();
        stopAppIconProgress();
      }

      private void stopAppIconProgress() {
        UIUtil.invokeLaterIfNeeded(() -> {
          if (myProject != null && myProject.isDisposed()) {
            return;
          }
          final AppIcon appIcon = AppIcon.getInstance();
          if (appIcon.hideProgress(myProject, APP_ICON_ID)) {
            if (myErrorCount > 0) {
              appIcon.setErrorBadge(myProject, String.valueOf(myErrorCount));
              appIcon.requestAttention(myProject, true);
            }
            else if (!myCompilationStartedAutomatically) {
              appIcon.setOkBadge(myProject, true);
              appIcon.requestAttention(myProject, false);
            }
          }
        });
      }

      @Override
      public void setFraction(final double fraction) {
        super.setFraction(fraction);
        GuiUtils.invokeLaterIfNeeded(
          () -> AppIcon.getInstance().setProgress(myProject, APP_ICON_ID, AppIconScheme.Progress.BUILD, fraction, true),
          ModalityState.any()
        );
      }

      @Override
      protected void onProgressChange() {
        myBuildViewService.onProgressChange(mySessionId, this);
      }
    });
  }

  public void cancel() {
    if (!myIndicator.isCanceled()) {
      myIndicator.cancel();
    }
  }

  public void addMessage(final CompilerMessage message) {
    final CompilerMessageCategory messageCategory = message.getCategory();
    if (CompilerMessageCategory.WARNING.equals(messageCategory)) {
      myWarningCount += 1;
    }
    else if (CompilerMessageCategory.ERROR.equals(messageCategory)) {
      myErrorCount += 1;
      ReadAction.run(() -> informWolf(message));
    }
    myBuildViewService.addMessage(mySessionId, message);
  }

  private void informWolf(final CompilerMessage message) {
    if (myProject.isDisposed()) return;
    WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(myProject);
    VirtualFile file = getVirtualFile(message);
    if (file != null) {
      wolf.queue(file);
    }
  }

  public static int translateCategory(CompilerMessageCategory category) {
    return CompilerMessagesService.translateCategory(category);
  }

  public void start(Runnable compileWork, Runnable restartWork) {
    myCompileWork = compileWork;
    myRestartWork = restartWork;
    queue();
  }

  public void run(Runnable compileWork, Runnable restartWork, ProgressIndicator progressIndicator) {
    myCompileWork = compileWork;
    myRestartWork = restartWork;
    run(progressIndicator);
  }

  @Override
  public boolean isHeadless() {
    return myHeadlessMode && !myForceAsyncExecution;
  }

  private static VirtualFile getVirtualFile(final CompilerMessage message) {
    VirtualFile virtualFile = message.getVirtualFile();
    if (virtualFile == null) {
      Navigatable navigatable = message.getNavigatable();
      if (navigatable instanceof OpenFileDescriptor) {
        virtualFile = ((OpenFileDescriptor)navigatable).getFile();
      }
    }
    return virtualFile;
  }

  public static TextRange getTextRange(final CompilerMessage message) {
    Navigatable navigatable = message.getNavigatable();
    if (navigatable instanceof OpenFileDescriptor) {
      int offset = ((OpenFileDescriptor)navigatable).getOffset();
      return new TextRange(offset, offset);
    }
    return TextRange.EMPTY_RANGE;
  }

  public static final class IDObject {
    private final String myDisplayName;

    public IDObject(@NotNull String displayName) {
      myDisplayName = displayName;
    }

    @Override
    public String toString() {
      return myDisplayName;
    }
  }
}
