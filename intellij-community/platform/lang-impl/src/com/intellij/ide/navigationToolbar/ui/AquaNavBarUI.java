/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package com.intellij.ide.navigationToolbar.ui;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.ApiStatus;

import java.awt.*;

/**
 * @deprecated will be removed 2020.1
 * @author Konstantin Bulenkov
 */
@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
@Deprecated
public class AquaNavBarUI extends AbstractNavBarUI {
  @Override
  public void doPaintWrapperPanel(Graphics2D g, Rectangle bounds, boolean mainToolbarVisible) {
    UIUtil.drawGradientHToolbarBackground(g, bounds.width, bounds.height);
  }

  @Override
  protected Color getBackgroundColor() {
    return ColorUtil.darker(Gray._200, 1);
  }
}
