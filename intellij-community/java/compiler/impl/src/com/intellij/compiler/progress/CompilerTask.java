// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author: Eugene Zhuravlev
 */
package com.intellij.compiler.progress;

import com.intellij.compiler.CompilerManagerImpl;
import com.intellij.compiler.HelpID;
import com.intellij.compiler.impl.CompilerErrorTreeView;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.ide.errorTreeView.impl.ErrorTreeViewConfiguration;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.AppIconScheme;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.pom.Navigatable;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.ui.AppIcon;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.content.*;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ui.MessageCategory;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompilerTask extends Task.Backgroundable {
  private static final Logger LOG = Logger.getInstance(CompilerTask.class);
  private static final Key<Object> CONTENT_ID_KEY = Key.create("CONTENT_ID");
  private static final Key<Object> SESSION_ID_KEY = Key.create("SESSION_ID");
  private static final String APP_ICON_ID = "compiler";
  @NotNull
  private final Object myContentId = new IDObject("content_id");
  private final boolean myModal;

  @NotNull
  private Object mySessionId = myContentId; // by default sessionID should be unique, just as content ID
  private NewErrorTreeViewPanel myErrorTreeView;
  private final Object myMessageViewLock = new Object();
  private final String myContentName;
  private final boolean myHeadlessMode;
  private final boolean myForceAsyncExecution;
  private final boolean myWaitForPreviousSession;
  private int myErrorCount = 0;
  private int myWarningCount = 0;
  private boolean myMessagesAutoActivated = false;

  private volatile ProgressIndicator myIndicator = new EmptyProgressIndicator();
  private Runnable myCompileWork;
  private final AtomicBoolean myMessageViewWasPrepared = new AtomicBoolean(false);
  private Runnable myRestartWork;
  private final boolean myCompilationStartedAutomatically;

  public CompilerTask(@NotNull Project project, String contentName, final boolean headlessMode, boolean forceAsync,
                      boolean waitForPreviousSession, boolean compilationStartedAutomatically) {
    this(project, contentName, headlessMode, forceAsync, waitForPreviousSession, compilationStartedAutomatically, false);
  }

  public CompilerTask(@NotNull Project project, String contentName, final boolean headlessMode, boolean forceAsync,
                       boolean waitForPreviousSession, boolean compilationStartedAutomatically, boolean modal) {
    super(project, contentName);
    myContentName = contentName;
    myHeadlessMode = headlessMode;
    myForceAsyncExecution = forceAsync;
    myWaitForPreviousSession = waitForPreviousSession;
    myCompilationStartedAutomatically = compilationStartedAutomatically;
    myModal = modal;
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

  public void registerCloseAction(final Runnable onClose) {
    synchronized (myMessageViewLock) {
      if (myErrorTreeView != null) {
        Disposer.register(myErrorTreeView, new Disposable() {
          @Override
          public void dispose() {
            onClose.run();
          }
        });
        return;
      }
    }
    onClose.run();
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

  @Override
  @Nullable
  public NotificationInfo getNotificationInfo() {
    return new NotificationInfo(myErrorCount > 0? "Compiler (errors)" : "Compiler (success)", "Compilation Finished", myErrorCount + " Errors, " + myWarningCount + " Warnings", true);
  }

  private CloseListener myCloseListener;

  @Override
  public void run(@NotNull final ProgressIndicator indicator) {
    myIndicator = indicator;
    myIndicator.setIndeterminate(false);

    final ProjectManager projectManager = ProjectManager.getInstance();
    projectManager.addProjectManagerListener(myProject, myCloseListener = new CloseListener());

    final Semaphore semaphore = ((CompilerManagerImpl)CompilerManager.getInstance(myProject)).getCompilationSemaphore();
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
        projectManager.removeProjectManagerListener(myProject, myCloseListener);
      }
      finally {
        if (acquired) {
          semaphore.release();
        }
      }
    }
  }

  private void prepareMessageView() {
    if (!myIndicator.isRunning()) {
      return;
    }
    if (myMessageViewWasPrepared.getAndSet(true)) {
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      final Project project = myProject;
      if (project == null || project.isDisposed()) {
        return;
      }
      synchronized (myMessageViewLock) {
        // clear messages from the previous compilation
        if (myErrorTreeView == null) {
          // if message view != null, the contents has already been cleared
          removeAllContents(project, null);
        }
      }
    });
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
        selectFirstMessage();
        stopAppIconProgress();
      }

      @Override
      public void stop() {
        super.stop();
        if (!isCanceled()) {
          selectFirstMessage();
        }
        stopAppIconProgress();
      }

      private void selectFirstMessage() {
        if (!isHeadlessMode()) {
          SwingUtilities.invokeLater(() -> {
            if (myProject != null && myProject.isDisposed()) {
              return;
            }
            synchronized (myMessageViewLock) {
              if (myErrorTreeView != null) {
                myErrorTreeView.selectFirstMessage();
              }
            }
          });
        }
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
      public void setText(final String text) {
        super.setText(text);
        updateProgressText();
      }

      @Override
      public void setText2(final String text) {
        super.setText2(text);
        updateProgressText();
      }

      @Override
      public void setFraction(final double fraction) {
        super.setFraction(fraction);
        updateProgressText();
        GuiUtils.invokeLaterIfNeeded(
          () -> AppIcon.getInstance().setProgress(myProject, APP_ICON_ID, AppIconScheme.Progress.BUILD, fraction, true),
          ModalityState.any()
        );
      }

      @Override
      protected void onProgressChange() {
        prepareMessageView();
      }
    });
  }

  public void cancel() {
    if (!myIndicator.isCanceled()) {
      myIndicator.cancel();
    }
  }

  public void addMessage(final CompilerMessage message) {
    prepareMessageView();

    final CompilerMessageCategory messageCategory = message.getCategory();
    if (CompilerMessageCategory.WARNING.equals(messageCategory)) {
      myWarningCount += 1;
    }
    else if (CompilerMessageCategory.ERROR.equals(messageCategory)) {
      myErrorCount += 1;
      ReadAction.run(() -> informWolf(message));
    }

    if (ApplicationManager.getApplication().isDispatchThread()) {
      openMessageView();
      doAddMessage(message);
    }
    else {
      final Window window = getWindow();
      final ModalityState modalityState = window != null ? ModalityState.stateForComponent(window) : ModalityState.NON_MODAL;
      ApplicationManager.getApplication().invokeLater(() -> {
        if (myProject != null && !myProject.isDisposed()) {
          openMessageView();
          doAddMessage(message);
        }
      }, modalityState);
    }
  }

  private void informWolf(final CompilerMessage message) {
    WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(myProject);
    VirtualFile file = getVirtualFile(message);
    wolf.queue(file);
  }

  private void doAddMessage(final CompilerMessage message) {
    synchronized (myMessageViewLock) {
      if (myErrorTreeView != null) {
        final Navigatable navigatable = message.getNavigatable();
        final VirtualFile file = message.getVirtualFile();
        final CompilerMessageCategory category = message.getCategory();
        final int type = translateCategory(category);
        final String[] text = convertMessage(message);
        if (navigatable != null) {
          final String groupName = file != null? file.getPresentableUrl() : category.getPresentableText();
          myErrorTreeView.addMessage(type, text, groupName, navigatable, message.getExportTextPrefix(), message.getRenderTextPrefix(), message.getVirtualFile());
        }
        else {
          myErrorTreeView.addMessage(type, text, file, -1, -1, message.getVirtualFile());
        }

        final boolean shouldAutoActivate =
          !myMessagesAutoActivated &&
          (
            CompilerMessageCategory.ERROR.equals(category) ||
            (CompilerMessageCategory.WARNING.equals(category) && !ErrorTreeViewConfiguration.getInstance(myProject).isHideWarnings())
          );
        if (shouldAutoActivate) {
          myMessagesAutoActivated = true;
          activateMessageView();
        }
      }
    }
  }

  private static String[] convertMessage(final CompilerMessage message) {
    String text = message.getMessage();
    if (!text.contains("\n")) {
      return new String[]{text};
    }
    ArrayList<String> lines = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(text, "\n", false);
    while (tokenizer.hasMoreTokens()) {
      lines.add(tokenizer.nextToken());
    }
    return ArrayUtilRt.toStringArray(lines);
  }

  public static int translateCategory(CompilerMessageCategory category) {
    if (CompilerMessageCategory.ERROR.equals(category)) {
      return MessageCategory.ERROR;
    }
    if (CompilerMessageCategory.WARNING.equals(category)) {
      return MessageCategory.WARNING;
    }
    if (CompilerMessageCategory.STATISTICS.equals(category)) {
      return MessageCategory.STATISTICS;
    }
    if (CompilerMessageCategory.INFORMATION.equals(category)) {
      return MessageCategory.INFORMATION;
    }
    LOG.error("Unknown message category: " + category);
    return 0;
  }

  public void start(Runnable compileWork, Runnable restartWork) {
    myCompileWork = compileWork;
    myRestartWork = restartWork;
    queue();
  }

  private void updateProgressText() {
  }

  // error tree view initialization must be invoked from event dispatch thread
  private void openMessageView() {
    if (isHeadlessMode()) {
      return;
    }
    if (myIndicator.isCanceled()) {
      return;
    }

    final JComponent component;
    synchronized (myMessageViewLock) {
      if (myErrorTreeView != null) {
        return;
      }
      myErrorTreeView = new CompilerErrorTreeView(
          myProject,
          myRestartWork
      );

      myErrorTreeView.setProcessController(new NewErrorTreeViewPanel.ProcessController() {
        @Override
        public void stopProcess() {
          cancel();
        }

        @Override
        public boolean isProcessStopped() {
          return !myIndicator.isRunning();
        }
      });
      component = myErrorTreeView.getComponent();
    }

    final MessageView messageView = MessageView.SERVICE.getInstance(myProject);
    final Content content = ContentFactory.SERVICE.getInstance().createContent(component, myContentName, true);
    content.setHelpId(HelpID.COMPILER);
    CONTENT_ID_KEY.set(content, myContentId);
    SESSION_ID_KEY.set(content, mySessionId);
    messageView.getContentManager().addContent(content);
    myCloseListener.setContent(content, messageView.getContentManager());
    removeAllContents(myProject, content);
    messageView.getContentManager().setSelectedContent(content);
  }

  public void showCompilerContent() {
    synchronized (myMessageViewLock) {
      if (myErrorTreeView != null) {
        showCompilerContent(myProject, myContentId);
      }
    }
  }

  public static boolean showCompilerContent(final Project project, final Object contentId) {
    final MessageView messageView = MessageView.SERVICE.getInstance(project);
    for (Content content : messageView.getContentManager().getContents()) {
      if (CONTENT_ID_KEY.get(content) == contentId) {
        messageView.getContentManager().setSelectedContent(content);
        return true;
      }
    }
    return false;
  }

  private void removeAllContents(Project project, Content notRemove) {
    if (project.isDisposed()) {
      return;
    }
    final MessageView messageView = MessageView.SERVICE.getInstance(project);
    Content[] contents = messageView.getContentManager().getContents();
    for (Content content : contents) {
      if (content.isPinned()) {
        continue;
      }
      if (content == notRemove) {
        continue;
      }
      boolean toRemove = CONTENT_ID_KEY.get(content) == myContentId;
      if (!toRemove) {
        final Object contentSessionId = SESSION_ID_KEY.get(content);
        toRemove = contentSessionId != null && contentSessionId != mySessionId; // the content was added by previous compilation
      }
      if (toRemove) {
        messageView.getContentManager().removeContent(content, true);
      }
    }
  }

  private void activateMessageView() {
    synchronized (myMessageViewLock) {
      if (myErrorTreeView != null && myProject != null) {
        final ToolWindow tw = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (tw != null) {
          tw.activate(null, false);
        }
      }
    }
  }

  public Window getWindow(){
    return null;
  }

  @Override
  public boolean isHeadless() {
    return myHeadlessMode && !myForceAsyncExecution;
  }

  private boolean isHeadlessMode() {
    return myHeadlessMode;
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

  private class CloseListener extends ContentManagerAdapter implements ProjectManagerListener {
    private Content myContent;
    private ContentManager myContentManager;
    private boolean myIsApplicationExitingOrProjectClosing = false;
    private boolean myUserAcceptedCancel = false;

    @Override
    public void projectClosingBeforeSave(@NotNull Project project) {
      if (myProject == project) {
        cancel();
      }
    }

    public void setContent(Content content, ContentManager contentManager) {
      myContent = content;
      myContentManager = contentManager;
      contentManager.addContentManagerListener(this);
    }

    @Override
    public void contentRemoved(@NotNull ContentManagerEvent event) {
      if (event.getContent() == myContent) {
        synchronized (myMessageViewLock) {
          if (myErrorTreeView != null) {
            Disposer.dispose(myErrorTreeView);
            myErrorTreeView = null;
            if (myIndicator.isRunning()) {
              cancel();
            }
            if (AppIcon.getInstance().hideProgress(myProject, "compiler")) {
              AppIcon.getInstance().setErrorBadge(myProject, null);
            }
          }
        }
        myContentManager.removeContentManagerListener(this);
        myContent.release();
        myContent = null;
      }
    }

    @Override
    public void contentRemoveQuery(@NotNull ContentManagerEvent event) {
      if (event.getContent() == myContent) {
        if (!myIndicator.isCanceled() && shouldAskUser()) {
          int result = Messages.showOkCancelDialog(
            myProject,
            CompilerBundle.message("warning.compiler.running.on.toolwindow.close"),
            CompilerBundle.message("compiler.running.dialog.title"),
            Messages.getQuestionIcon()
          );
          if (result != Messages.OK) {
            event.consume(); // veto closing
          }
          myUserAcceptedCancel = true;
        }
      }
    }

    private boolean shouldAskUser() {
      // do not ask second time if user already accepted closing
      return !myUserAcceptedCancel && !myIsApplicationExitingOrProjectClosing && myIndicator.isRunning();
    }

    @Override
    public void projectClosed(@NotNull Project project) {
      if (project.equals(myProject) && myContent != null) {
        myContentManager.removeContent(myContent, true);
      }
    }

    @Override
    public void projectClosing(@NotNull Project project) {
      if (project.equals(myProject)) {
        myIsApplicationExitingOrProjectClosing = true;
      }
    }
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
