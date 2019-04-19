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
package com.intellij.codeInsight.navigation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ListComponentUpdater;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageView;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.*;

public abstract class BackgroundUpdaterTask extends Task.Backgroundable {
  protected JBPopup myPopup;
  private ListComponentUpdater myUpdater;
  private Ref<? extends UsageView> myUsageView;
  private final Collection<PsiElement> myData;

  private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private final Object lock = new Object();

  private volatile boolean myCanceled;
  private volatile boolean myFinished;
  private volatile ProgressIndicator myIndicator;

  public BackgroundUpdaterTask(@Nullable Project project, @NotNull String title, @Nullable Comparator<PsiElement> comparator) {
    super(project, title);
    myData = comparator == null ? ContainerUtil.newSmartList() : new TreeSet<>(comparator);
  }

  @TestOnly
  public ListComponentUpdater getUpdater() {
    return myUpdater;
  }

  public void init(@NotNull JBPopup popup, @NotNull ListComponentUpdater updater, @NotNull Ref<? extends UsageView> usageView) {
    myPopup = popup;
    myUpdater = updater;
    myUsageView = usageView;
  }

  public abstract String getCaption(int size);

  protected void replaceModel(@NotNull List<? extends PsiElement> data) {
    myUpdater.replaceModel(data);
  }

  protected void paintBusy(boolean paintBusy) {
    myUpdater.paintBusy(paintBusy);
  }

  protected static Comparator<PsiElement> createComparatorWrapper(@NotNull Comparator<? super PsiElement> comparator) {
    return (o1, o2) -> {
      int diff = comparator.compare(o1, o2);
      if (diff == 0) {
        return ReadAction.compute(() -> PsiUtilCore.compareElementsByPosition(o1, o2));
      }
      return diff;
    };
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
   * @deprecated Use {@link #BackgroundUpdaterTask(Project, String, Comparator)} and {@link #updateComponent(PsiElement)} instead
   */
  @Deprecated
  public boolean updateComponent(@NotNull PsiElement element, @Nullable Comparator comparator) {
    final UsageView view = myUsageView.get();
    if (view != null && !((UsageViewImpl)view).isDisposed()) {
      ApplicationManager.getApplication().runReadAction(() -> view.appendUsage(new UsageInfo2UsageAdapter(new UsageInfo(element))));
      return true;
    }

    if (myCanceled) return false;

    final JComponent content = myPopup.getContent();
    if ((myPopup instanceof AbstractPopup && content == null) || myPopup.isDisposed()) return false;
    ModalityState modalityState = content == null ? null : ModalityState.stateForComponent(content);

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
  
  public boolean updateComponent(@NotNull PsiElement element) {
    final UsageView view = myUsageView.get();
    if (view != null && !((UsageViewImpl)view).isDisposed()) {
      ApplicationManager.getApplication().runReadAction(() -> view.appendUsage(new UsageInfo2UsageAdapter(new UsageInfo(element))));
      return true;
    }

    if (myCanceled) return false;
    final JComponent content = myPopup.getContent();
    if (content == null || myPopup.isDisposed()) return false;

    synchronized (lock) {
      if (!myData.add(element)) return true;
    }

    myAlarm.addRequest(() -> {
      myAlarm.cancelAllRequests();
      refreshModelImmediately();
    }, 200, ModalityState.stateForComponent(content));
    return true;
  }

  private void refreshModelImmediately() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myCanceled) return;
    if (myPopup.isDisposed()) return;
    List<PsiElement> data;
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
  protected PsiElement getTheOnlyOneElement() {
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
