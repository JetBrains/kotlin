// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.GenericListComponentUpdater;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Ref;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.util.Alarm;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public abstract class BackgroundUpdaterTaskBase<T> extends Task.Backgroundable {
  protected JBPopup myPopup;
  private GenericListComponentUpdater<T> myUpdater;
  private Ref<? extends UsageView> myUsageView;
  private final Collection<T> myData;

  private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private final Object lock = new Object();

  private volatile boolean myCanceled;
  private volatile boolean myFinished;
  private volatile ProgressIndicator myIndicator;

  public BackgroundUpdaterTaskBase(@Nullable Project project, @NotNull String title, @Nullable Comparator<T> comparator) {
    super(project, title);
    myData = comparator == null ? new SmartList<>() : new TreeSet<>(comparator);
  }

  @TestOnly
  public GenericListComponentUpdater<T> getUpdater() {
    return myUpdater;
  }

  public void init(@NotNull JBPopup popup, @NotNull GenericListComponentUpdater<T> updater, @NotNull Ref<? extends UsageView> usageView) {
    myPopup = popup;
    myUpdater = updater;
    myUsageView = usageView;
  }

  public abstract String getCaption(int size);

  @Nullable
  protected abstract Usage createUsage(T element);

  protected void replaceModel(@NotNull List<? extends T> data) {
    myUpdater.replaceModel(data);
  }

  protected void paintBusy(boolean paintBusy) {
    myUpdater.paintBusy(paintBusy);
  }

  private boolean setCanceled() {
    boolean canceled = myCanceled;
    myCanceled = true;
    return canceled;
  }

  public boolean isCanceled() {
    return myCanceled;
  }

  /**
   * @deprecated Use {@link #BackgroundUpdaterTaskBase(Project, String, Comparator)} and {@link #updateComponent(T)} instead
   */
  @Deprecated
  public boolean updateComponent(@NotNull T element, @Nullable Comparator comparator) {
    if (tryAppendUsage(element)) return true;
    if (myCanceled) return false;

    if (myPopup.isDisposed()) return false;
    ModalityState modalityState = ModalityState.stateForComponent(myPopup.getContent());

    synchronized (lock) {
      if (myData.contains(element)) return true;
      myData.add(element);
      if (comparator != null && myData instanceof List) {
        Collections.sort((List)myData, comparator);
      }
    }

    myAlarm.addRequest(() -> {
      myAlarm.cancelAllRequests();
      refreshModelImmediately();
    }, 200, modalityState);
    return true;
  }

  private boolean tryAppendUsage(@NotNull T element) {
    final UsageView view = myUsageView.get();
    if (view != null && !((UsageViewImpl)view).isDisposed()) {
      Usage usage = createUsage(element);
      if (usage == null)
        return false;
      ApplicationManager.getApplication().runReadAction(() -> view.appendUsage(usage));
      return true;
    }
    return false;
  }

  public boolean updateComponent(@NotNull T element) {
    if (tryAppendUsage(element)) return true;

    if (myCanceled) return false;
    if (myPopup.isDisposed()) return false;

    synchronized (lock) {
      if (!myData.add(element)) return true;
    }

    myAlarm.addRequest(() -> {
      myAlarm.cancelAllRequests();
      refreshModelImmediately();
    }, 200, ModalityState.stateForComponent(myPopup.getContent()));
    return true;
  }

  private void refreshModelImmediately() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myCanceled) return;
    if (myPopup.isDisposed()) return;
    List<T> data;
    synchronized (lock) {
      data = new ArrayList<>(myData);
    }
    replaceModel(data);
    myPopup.setCaption(getCaption(getCurrentSize()));
    myPopup.pack(true, true);
  }

  public int getCurrentSize() {
    synchronized (lock) {
      return myData.size();
    }
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    paintBusy(true);
    myIndicator = indicator;
  }

  @Override
  public void onSuccess() {
    myFinished = true;
    refreshModelImmediately();
    paintBusy(false);
  }

  @Override
  public void onFinished() {
    myAlarm.cancelAllRequests();
    myFinished = true;
  }

  @Nullable
  protected T getTheOnlyOneElement() {
    synchronized (lock) {
      if (myData.size() == 1) {
        return myData.iterator().next();
      }
    }
    return null;
  }

  public boolean isFinished() {
    return myFinished;
  }

  public boolean cancelTask() {
    ProgressIndicator indicator = myIndicator;
    if (indicator != null) {
      indicator.cancel();
    }
    return setCanceled();
  }
}
