// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
class BuildProgressStripe extends JBPanel {
  @NotNull
  private final JBPanel myPanel;
  private final NotNullLazyValue<MyLoadingDecorator> myCreateLoadingDecorator;
  private MyLoadingDecorator myDecorator;

  BuildProgressStripe(@NotNull JComponent targetComponent, @NotNull Disposable parent, int startDelayMs) {
    super(new BorderLayout());
    myPanel = new JBPanel(new BorderLayout());
    myPanel.setOpaque(false);
    myPanel.add(targetComponent);
    myCreateLoadingDecorator = NotNullLazyValue.createValue(() -> {
      return new MyLoadingDecorator(myPanel, parent, startDelayMs);
    });
    createLoadingDecorator();
  }

  public void updateProgress(long total, long progress) {
    if (total == progress) {
      stopLoading();
      return;
    }
    boolean isDeterminate = total > 0 && progress > 0;
    JProgressBar progressBar = getProgressBar();
    boolean isProgressBarIndeterminate = progressBar.isIndeterminate();
    if (isDeterminate) {
      startLoading();
      progressBar.setValue(Math.toIntExact(progress * 100 / total));
      if (isProgressBarIndeterminate) {
        progressBar.setIndeterminate(false);
      }
    }
    else if (!isProgressBarIndeterminate) {
      progressBar.setIndeterminate(true);
    }
  }

  void startLoading() {
    myDecorator.startLoading();
  }

  void stopLoading() {
    JProgressBar progressBar = getProgressBar();
    if (!progressBar.isIndeterminate()) {
      progressBar.setValue(100);
    }
    myDecorator.stopLoading();
  }

  private JProgressBar getProgressBar() {
    return myCreateLoadingDecorator.getValue().myProgressBar;
  }

  private void createLoadingDecorator() {
    myDecorator = myCreateLoadingDecorator.getValue();
    add(myDecorator.getComponent(), BorderLayout.CENTER);
    myDecorator.setLoadingText("");
  }

  private static class MyLoadingDecorator extends LoadingDecorator {
    private final AtomicBoolean loadingStarted = new AtomicBoolean(false);
    private @Nullable JProgressBar myProgressBar;


    private MyLoadingDecorator(@NotNull JPanel contentPanel, @NotNull Disposable disposable, int startDelayMs) {
      super(contentPanel, disposable, startDelayMs, true);
    }

    @Override
    protected NonOpaquePanel customizeLoadingLayer(JPanel parent, JLabel text, AsyncProcessIcon icon) {
      parent.setLayout(new BorderLayout());
      NonOpaquePanel result = new NonOpaquePanel();
      result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
      myProgressBar = new JProgressBar();
      myProgressBar.setIndeterminate(true);
      myProgressBar.putClientProperty("ProgressBar.stripeWidth", 2);
      myProgressBar.putClientProperty("ProgressBar.flatEnds", Boolean.TRUE);
      result.add(myProgressBar);
      parent.add(result, BorderLayout.NORTH);
      return result;
    }

    public void startLoading() {
      if (loadingStarted.compareAndSet(false, true)) {
        super.startLoading(false);
      }
    }

    @Override
    public void stopLoading() {
      if (loadingStarted.compareAndSet(true, false)) {
        super.stopLoading();
      }
    }
  }
}
