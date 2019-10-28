package com.intellij.openapi.externalSystem.service.internal;

import com.intellij.build.events.ProgressBuildEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.*;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemStatusEvent;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent;
import com.intellij.openapi.externalSystem.service.ExternalSystemFacadeManager;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemFacade;
import com.intellij.openapi.externalSystem.service.execution.NotSupportedException;
import com.intellij.openapi.externalSystem.service.notification.*;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Encapsulates particular task performed by external system integration.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public abstract class AbstractExternalSystemTask extends UserDataHolderBase implements ExternalSystemTask {

  private static final Logger LOG = Logger.getInstance(AbstractExternalSystemTask.class);

  private final AtomicReference<ExternalSystemTaskState> myState =
    new AtomicReference<>(ExternalSystemTaskState.NOT_STARTED);
  private final AtomicReference<Throwable> myError = new AtomicReference<>();

  @NotNull private final transient Project myIdeProject;

  @NotNull private final ExternalSystemTaskId myId;
  @NotNull private final ProjectSystemId myExternalSystemId;
  @NotNull private final String myExternalProjectPath;

  protected AbstractExternalSystemTask(@NotNull ProjectSystemId id,
                                       @NotNull ExternalSystemTaskType type,
                                       @NotNull Project project,
                                       @NotNull String externalProjectPath) {
    myExternalSystemId = id;
    myIdeProject = project;
    myId = ExternalSystemTaskId.create(id, type, myIdeProject);
    myExternalProjectPath = externalProjectPath;
  }

  @NotNull
  public ProjectSystemId getExternalSystemId() {
    return myExternalSystemId;
  }

  @Override
  @NotNull
  public ExternalSystemTaskId getId() {
    return myId;
  }

  @Override
  @NotNull
  public ExternalSystemTaskState getState() {
    return myState.get();
  }

  protected void setState(@NotNull ExternalSystemTaskState state) {
    myState.set(state);
  }

  protected boolean compareAndSetState(@NotNull ExternalSystemTaskState expect, @NotNull ExternalSystemTaskState update) {
    return myState.compareAndSet(expect, update);
  }

  @Override
  public Throwable getError() {
    return myError.get();
  }

  @NotNull
  public Project getIdeProject() {
    return myIdeProject;
  }

  @NotNull
  public String getExternalProjectPath() {
    return myExternalProjectPath;
  }

  @Override
  public void refreshState() {
    if (getState() != ExternalSystemTaskState.IN_PROGRESS) {
      return;
    }
    final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
    try {
      final RemoteExternalSystemFacade facade = manager.getFacade(myIdeProject, myExternalProjectPath, myExternalSystemId);
      setState(facade.isTaskInProgress(getId()) ? ExternalSystemTaskState.IN_PROGRESS : ExternalSystemTaskState.FAILED);
    }
    catch (Throwable e) {
      setState(ExternalSystemTaskState.FAILED);
      myError.set(e);
      if (!myIdeProject.isDisposed()) {
        LOG.warn(e);
      }
    }
  }

  @Override
  public void execute(@NotNull final ProgressIndicator indicator, @NotNull ExternalSystemTaskNotificationListener... listeners) {
    indicator.setIndeterminate(true);
    ExternalSystemTaskNotificationListenerAdapter adapter = new ExternalSystemTaskNotificationListenerAdapter() {
      @Override
      public void onStatusChange(@NotNull ExternalSystemTaskNotificationEvent event) {
        updateProgressIndicator(event, indicator);
      }
    };
    final ExternalSystemTaskNotificationListener[] ls;
    if (listeners.length > 0) {
      ls = ArrayUtil.append(listeners, adapter);
    }
    else {
      ls = new ExternalSystemTaskNotificationListener[]{adapter};
    }

    execute(ls);
  }

  @Override
  public void execute(@NotNull ExternalSystemTaskNotificationListener... listeners) {
    if (!compareAndSetState(ExternalSystemTaskState.NOT_STARTED, ExternalSystemTaskState.IN_PROGRESS)) return;

    ExternalSystemProgressNotificationManager progressManager = ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
    for (ExternalSystemTaskNotificationListener listener : listeners) {
      progressManager.addNotificationListener(getId(), listener);
    }
    ExternalSystemProcessingManager processingManager = ServiceManager.getService(ExternalSystemProcessingManager.class);
    try {
      processingManager.add(this);
      doExecute();
      setState(ExternalSystemTaskState.FINISHED);
    }
    catch (Exception e) {
      LOG.debug(e);
      myError.set(e);
      setState(ExternalSystemTaskState.FAILED);
    }
    catch (Throwable e) {
      LOG.error(e);
      myError.set(e);
      setState(ExternalSystemTaskState.FAILED);
    }
    finally {
      for (ExternalSystemTaskNotificationListener listener : listeners) {
        progressManager.removeNotificationListener(listener);
      }
      processingManager.release(getId());
    }
  }

  protected abstract void doExecute() throws Exception;

  @Override
  public boolean cancel(@NotNull final ProgressIndicator indicator, @NotNull ExternalSystemTaskNotificationListener... listeners) {
    indicator.setIndeterminate(true);
    ExternalSystemTaskNotificationListenerAdapter adapter = new ExternalSystemTaskNotificationListenerAdapter() {
      @Override
      public void onStatusChange(@NotNull ExternalSystemTaskNotificationEvent event) {
        indicator.setText(wrapProgressText(event.getDescription()));
      }
    };
    final ExternalSystemTaskNotificationListener[] ls;
    if (listeners.length > 0) {
      ls = ArrayUtil.append(listeners, adapter);
    }
    else {
      ls = new ExternalSystemTaskNotificationListener[]{adapter};
    }

    return cancel(ls);
  }

  @Override
  public boolean cancel(@NotNull ExternalSystemTaskNotificationListener... listeners) {
    ExternalSystemTaskState currentTaskState = getState();
    if (currentTaskState.isStopped()) return true;

    ExternalSystemProgressNotificationManager progressManager = ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
    for (ExternalSystemTaskNotificationListener listener : listeners) {
      progressManager.addNotificationListener(getId(), listener);
    }

    if (!compareAndSetState(currentTaskState, ExternalSystemTaskState.CANCELING)) return false;

    boolean result = false;
    try {
      result = doCancel();
      return result;
    }
    catch (NotSupportedException e) {
      NotificationData notification =
        new NotificationData("Cancellation failed", e.getMessage(), NotificationCategory.WARNING, NotificationSource.PROJECT_SYNC);
      notification.setBalloonNotification(true);
      ExternalSystemNotificationManager.getInstance(getIdeProject()).showNotification(getExternalSystemId(), notification);
    }
    catch (Throwable e) {
      setState(ExternalSystemTaskState.CANCELLATION_FAILED);
      myError.set(e);
      LOG.warn(e);
    }
    finally {
      for (ExternalSystemTaskNotificationListener listener : listeners) {
        progressManager.removeNotificationListener(listener);
      }
    }
    return result;
  }

  protected abstract boolean doCancel() throws Exception;


  @NotNull
  protected String wrapProgressText(@NotNull String text) {
    return ExternalSystemBundle.message("progress.update.text", getExternalSystemId(), text);
  }

  @Override
  public int hashCode() {
    return myId.hashCode() + myExternalSystemId.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AbstractExternalSystemTask task = (AbstractExternalSystemTask)o;
    return myId.equals(task.myId) && myExternalSystemId.equals(task.myExternalSystemId);
  }

  @Override
  public String toString() {
    return String.format("%s task %s: %s", myExternalSystemId.getReadableName(), myId, myState);
  }

  private void updateProgressIndicator(@NotNull ExternalSystemTaskNotificationEvent event, @NotNull ProgressIndicator indicator) {
    long total;
    long progress;
    String unit;
    if (event instanceof ExternalSystemBuildEvent &&
        ((ExternalSystemBuildEvent)event).getBuildEvent() instanceof ProgressBuildEvent) {
      ProgressBuildEvent progressEvent = (ProgressBuildEvent)((ExternalSystemBuildEvent)event).getBuildEvent();
      total = progressEvent.getTotal();
      progress = progressEvent.getProgress();
      unit = progressEvent.getUnit();
    }
    else if (event instanceof ExternalSystemTaskExecutionEvent &&
             ((ExternalSystemTaskExecutionEvent)event).getProgressEvent() instanceof ExternalSystemStatusEvent) {
      ExternalSystemStatusEvent progressEvent = (ExternalSystemStatusEvent)((ExternalSystemTaskExecutionEvent)event).getProgressEvent();
      total = progressEvent.getTotal();
      progress = progressEvent.getProgress();
      unit = progressEvent.getUnit();
    } else {
      return;
    }

    String sizeInfo;
    if (total <= 0) {
      indicator.setIndeterminate(true);
      sizeInfo = "bytes".equals(unit) ? (StringUtil.formatFileSize(progress) + " / ?") : "";
    }
    else {
      indicator.setIndeterminate(false);
      indicator.setFraction((double)progress / total);
      sizeInfo = "bytes".equals(unit) ? (StringUtil.formatFileSize(progress) +
                                         " / " +
                                         StringUtil.formatFileSize(total)) : "";
    }
    String description = event.getDescription();
    indicator.setText(wrapProgressText(description) + (sizeInfo.isEmpty() ? "" : "  (" + sizeInfo + ')'));
  }
}
