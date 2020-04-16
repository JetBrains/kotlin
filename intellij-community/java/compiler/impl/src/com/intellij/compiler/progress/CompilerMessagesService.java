// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.progress;

import com.intellij.compiler.HelpID;
import com.intellij.compiler.impl.CompilerErrorTreeView;
import com.intellij.compiler.impl.ExitStatus;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.ide.errorTreeView.impl.ErrorTreeViewConfiguration;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.pom.Navigatable;
import com.intellij.ui.AppIcon;
import com.intellij.ui.content.*;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ui.MessageCategory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
public class CompilerMessagesService implements BuildViewService {
  private static final Logger LOG = Logger.getInstance(CompilerTask.class);
  private static final Key<Object> CONTENT_ID_KEY = Key.create("CONTENT_ID");
  private static final Key<Object> SESSION_ID_KEY = Key.create("SESSION_ID");

  @NotNull private final Project myProject;
  @NotNull private final Object myContentId;
  private String myContentName;
  private Runnable myRestartWork;
  private final boolean myHeadlessMode;
  private boolean myMessagesAutoActivated = false;
  private final Object myMessageViewLock = new Object();
  private final AtomicBoolean myMessageViewWasPrepared = new AtomicBoolean(false);
  private volatile ProgressIndicator myIndicator = new EmptyProgressIndicator();
  private NewErrorTreeViewPanel myErrorTreeView;
  private CloseListener myCloseListener;

  public CompilerMessagesService(@NotNull Project project, @NotNull Object contentId, String contentName, boolean headlessMode) {
    myProject = project;
    myContentId = contentId;
    myContentName = contentName;
    myHeadlessMode = headlessMode;
  }

  @Override
  public void registerCloseAction(Runnable onClose) {
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
  public void onProgressChange(Object sessionId, ProgressIndicator indicator) {
    prepareMessageView(sessionId, indicator);
  }

  @Override
  public void addMessage(Object sessionId, CompilerMessage message) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      openMessageView(sessionId);
      doAddMessage(message);
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (!myProject.isDisposed()) {
          openMessageView(sessionId);
          doAddMessage(message);
        }
      }, ModalityState.NON_MODAL);
    }
  }

  @Override
  public void onStart(Object sessionId, long startCompilationStamp, Runnable restartWork, ProgressIndicator indicator) {
    myCloseListener = new CloseListener();
    synchronized (myMessageViewLock) {
      myRestartWork = restartWork;
    }
    myIndicator = indicator;
    if (!isHeadlessMode()) {
      addIndicatorDelegate(myIndicator);
    }
    ProjectManager.getInstance().addProjectManagerListener(myProject, myCloseListener);
  }

  private void addIndicatorDelegate(ProgressIndicator indicator) {
    if (!(indicator instanceof ProgressIndicatorEx)) {
      return;
    }
    ((ProgressIndicatorEx)indicator).addStateDelegate(new DummyProgressIndicator() {
      @Override
      public void cancel() {
        selectFirstMessage();
      }

      @Override
      public void stop() {
        if (!isCanceled()) {
          selectFirstMessage();
        }
      }
    });
  }

  @Override
  public void onEnd(Object sessionId, ExitStatus exitStatus, long endCompilationStamp) {
    ProjectManager.getInstance().removeProjectManagerListener(myProject, myCloseListener);
    cleanupMessageViewIfEmpty(sessionId);
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

  private void selectFirstMessage() {
    if (!isHeadlessMode()) {
      SwingUtilities.invokeLater(() -> {
        if (myProject.isDisposed()) {
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

  private boolean isHeadlessMode() {
    return myHeadlessMode;
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

  private void prepareMessageView(Object sessionId, ProgressIndicator indicator) {
    if (indicator.isRunning() && !myMessageViewWasPrepared.getAndSet(true)) {
      ApplicationManager.getApplication().invokeLater(() -> {
        final Project project = myProject;
        if (!project.isDisposed()) {
          synchronized (myMessageViewLock) {
            if (myErrorTreeView == null) {
              MessageView messageView = project.getServiceIfCreated(MessageView.class);
              if (messageView != null && messageView.getContentManager().getContentCount() > 0) {
                // only do something if there are already contains from previous compilations present
                // this will add the new content for this task and clear messages from previous compilations
                openMessageView(sessionId);
              }
            }
          }
        }
      });
    }
  }

  private void cleanupMessageViewIfEmpty(Object sessionId) {
    ApplicationManager.getApplication().invokeLater(() -> {
      final Project project = myProject;
      if (!project.isDisposed()) {
        synchronized (myMessageViewLock) {
          final NewErrorTreeViewPanel errorTreeView = myErrorTreeView;
          if (errorTreeView != null && errorTreeView.getErrorViewStructure().isEmpty()) {
            final ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
            if (tw == null || !tw.isVisible()) {
              // if message view is not visible and current content is empty, remove the content
              removeTaskContents(project, sessionId);
            }
          }
        }
      }
    });
  }

  private void removeTaskContents(Project project, Object sessionId) {
    if (project.isDisposed()) {
      return;
    }
    final ContentManager contentManager = MessageView.SERVICE.getInstance(project).getContentManager();
    for (Content content : contentManager.getContents()) {
      if (!content.isPinned()) {
        if (CONTENT_ID_KEY.get(content) == myContentId || SESSION_ID_KEY.get(content) == sessionId) {
          contentManager.removeContent(content, true);
        }
      }
    }
  }

  // error tree view initialization must be invoked from event dispatch thread
  private void openMessageView(Object sessionId) {
    if (isHeadlessMode() || myIndicator.isCanceled()) {
      return;
    }
    final JComponent component;
    synchronized (myMessageViewLock) {
      if (myErrorTreeView != null) {
        return;
      }
      myErrorTreeView = new CompilerErrorTreeView(myProject, myRestartWork);
      myErrorTreeView.setProcessController(new NewErrorTreeViewPanel.ProcessController() {
        @Override
        public void stopProcess() {
          if (!myIndicator.isCanceled()) {
            myIndicator.cancel();
          }
        }

        @Override
        public boolean isProcessStopped() {
          return !myIndicator.isRunning();
        }
      });
      component = myErrorTreeView.getComponent();
    }

    MessageView messageView = MessageView.SERVICE.getInstance(myProject);
    messageView.runWhenInitialized(() -> {
      Content content = ContentFactory.SERVICE.getInstance().createContent(component, myContentName, true);
      content.setHelpId(HelpID.COMPILER);
      CONTENT_ID_KEY.set(content, myContentId);
      SESSION_ID_KEY.set(content, sessionId);
      messageView.getContentManager().addContent(content);
      myCloseListener.setContent(content, messageView.getContentManager());
      removeOldContents(myProject, sessionId, content);
      messageView.getContentManager().setSelectedContent(content);
    });
  }

  private void removeOldContents(final Project project, Object sessionId, final Content notRemove) {
    if (project.isDisposed()) {
      return;
    }
    final ContentManager contentManager = MessageView.SERVICE.getInstance(project).getContentManager();
    for (Content content : contentManager.getContents()) {
      if (content.isPinned() || content == notRemove) {
        continue;
      }
      boolean toRemove = CONTENT_ID_KEY.get(content) == myContentId;
      if (!toRemove) {
        final Object contentSessionId = SESSION_ID_KEY.get(content);
        toRemove = contentSessionId != null && contentSessionId != sessionId; // the content was added by previous compilation
      }
      if (toRemove) {
        contentManager.removeContent(content, true);
      }
    }
  }

  private void activateMessageView() {
    synchronized (myMessageViewLock) {
      if (myErrorTreeView != null) {
        final ToolWindow tw = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (tw != null) {
          tw.activate(null, false);
        }
      }
    }
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
          final String groupName = file != null ? file.getPresentableUrl() : category.getPresentableText();
          myErrorTreeView.addMessage(type, text, groupName, navigatable, message.getExportTextPrefix(), message.getRenderTextPrefix(),
                                     message.getVirtualFile());
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

  private final class CloseListener implements ProjectManagerListener, ContentManagerListener {
    private Content myContent;
    private ContentManager myContentManager;
    private boolean myIsApplicationExitingOrProjectClosing = false;
    private boolean myUserAcceptedCancel = false;

    @Override
    public void projectClosingBeforeSave(@NotNull Project project) {
      if (myProject == project && !myIndicator.isCanceled()) {
        myIndicator.cancel();
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
            if (myIndicator.isRunning() && !myIndicator.isCanceled()) {
              myIndicator.cancel();
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
            JavaCompilerBundle.message("warning.compiler.running.on.toolwindow.close"),
            JavaCompilerBundle.message("compiler.running.dialog.title"),
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

  static class DummyProgressIndicator implements ProgressIndicatorEx {
    @Override
    public void addStateDelegate(@NotNull ProgressIndicatorEx delegate) { }

    @Override
    public void finish(@NotNull TaskInfo task) { }

    @Override
    public boolean isFinished(@NotNull TaskInfo task) {
      return false;
    }

    @Override
    public boolean wasStarted() {
      return false;
    }

    @Override
    public void processFinish() { }

    @Override
    public void initStateFrom(@NotNull ProgressIndicator indicator) { }

    @Override
    public void start() { }

    @Override
    public void stop() { }

    @Override
    public boolean isRunning() {
      return false;
    }

    @Override
    public void cancel() { }

    @Override
    public boolean isCanceled() {
      return false;
    }

    @Override
    public void setText(@Nls(capitalization = Nls.Capitalization.Sentence) String text) { }

    @Override
    public String getText() {
      return null;
    }

    @Override
    public void setText2(@Nls(capitalization = Nls.Capitalization.Sentence) String text) { }

    @Override
    public String getText2() {
      return null;
    }

    @Override
    public double getFraction() {
      return 0;
    }

    @Override
    public void setFraction(double fraction) { }

    @Override
    public void pushState() { }

    @Override
    public void popState() { }

    @Override
    public boolean isModal() {
      return false;
    }

    @Override
    public @NotNull ModalityState getModalityState() {
      return ModalityState.defaultModalityState();
    }

    @Override
    public void setModalityProgress(@Nullable ProgressIndicator modalityProgress) { }

    @Override
    public boolean isIndeterminate() {
      return false;
    }

    @Override
    public void setIndeterminate(boolean indeterminate) { }

    @Override
    public void checkCanceled() throws ProcessCanceledException { }

    @Override
    public boolean isPopupWasShown() {
      return false;
    }

    @Override
    public boolean isShowing() {
      return false;
    }
  }
}
