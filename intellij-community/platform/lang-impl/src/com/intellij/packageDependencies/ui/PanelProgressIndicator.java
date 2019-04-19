/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.packageDependencies.ui;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.EdtExecutorService;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PanelProgressIndicator extends ProgressIndicatorBase {
  private final MyProgressPanel myProgressPanel;
  private boolean myPaintInQueue;
  private final Consumer<? super JComponent> myComponentUpdater;
  private Future<?> myAlarm = CompletableFuture.completedFuture(null);

  public PanelProgressIndicator(Consumer<? super JComponent> componentUpdater) {
    myProgressPanel = new MyProgressPanel();
    myProgressPanel.myFractionProgress.setMaximum(100);
    myComponentUpdater = componentUpdater;
    setIndeterminate(false);
  }

  @Override
  public void start() {
    super.start();
    myComponentUpdater.consume(myProgressPanel.myPanel);
  }

  @Override
  public void stop() {
    super.stop();
    if (isCanceled()) {
      JLabel label = new JLabel(AnalysisScopeBundle.message("usage.view.canceled"));
      label.setHorizontalAlignment(SwingConstants.CENTER);
      myComponentUpdater.consume(label);
    }
  }

  @Override
  public void setText(String text) {
    if (!text.equals(getText())) {
      super.setText(text);
    }
  }

  @Override
  public void setFraction(double fraction) {
    if (fraction != getFraction()) {
      super.setFraction(fraction);
    }
  }


  @Override
  public void setIndeterminate(final boolean indeterminate) {
    if (isIndeterminate() == indeterminate) return;
    super.setIndeterminate(indeterminate);
  }

  public void update(final String scanningPackagesMessage, final boolean indeterminate, final double ffraction) {
    if (myPaintInQueue) return;
    checkCanceled();
    myPaintInQueue = true;
    myAlarm.cancel(false);
    myAlarm = EdtExecutorService.getScheduledExecutorInstance().schedule(() -> {
      myPaintInQueue = false;
      myProgressPanel.myTextLabel.setText(scanningPackagesMessage);
      int fraction = (int)(ffraction * 99 + 0.5);
      myProgressPanel.myFractionLabel.setText(fraction + "%");
      if (fraction != -1) {
        myProgressPanel.myFractionProgress.setValue(fraction);
      }
      myProgressPanel.myFractionProgress.setIndeterminate(indeterminate);
    }, 10, TimeUnit.MILLISECONDS);
  }

  public void setBordersVisible(final boolean visible) {
    myProgressPanel.myLeftPanel.setVisible(visible);
    myProgressPanel.myRightPanel.setVisible(visible);
  }

  private static class MyProgressPanel {
    public JLabel myFractionLabel;
    public JLabel myTextLabel;
    public JPanel myPanel;
    private JProgressBar myFractionProgress;
    private JPanel myLeftPanel;
    private JPanel myRightPanel;
  }
}
