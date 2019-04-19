/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.animation;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Denis Zhdanov
 */
public class ArrangementAnimationManager implements ArrangementAnimationPanel.Listener, ActionListener {

  @NotNull private final Timer myTimer = UIUtil.createNamedTimer("ArrangementAnimation", ArrangementConstants.ANIMATION_STEPS_TIME_GAP_MILLIS, this);

  @NotNull private final ArrangementAnimationPanel myAnimationPanel;
  @NotNull private final Callback                  myCallback;
  
  private boolean myFinished;

  public ArrangementAnimationManager(@NotNull ArrangementAnimationPanel panel, @NotNull Callback callback) {
    myAnimationPanel = panel;
    myCallback = callback;
    myAnimationPanel.setListener(this);
  }

  public void startAnimation() {
    myAnimationPanel.startAnimation();
    myCallback.onAnimationIteration(false);
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    myTimer.stop();
    myFinished = !myAnimationPanel.nextIteration();
    myCallback.onAnimationIteration(myFinished);
  }

  @Override
  public void onPaint() {
    if (!myFinished && !myTimer.isRunning()) {
      myTimer.start();
    }
  }

  public interface Callback {
    void onAnimationIteration(boolean finished);
  }
}
