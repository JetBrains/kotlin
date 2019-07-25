/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.util;

import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 */
public class PaintAwarePanel extends JPanel {

  @Nullable private Consumer<? super Graphics> myPaintCallback;

  public PaintAwarePanel() {
    this(new GridBagLayout());
  }

  public PaintAwarePanel(LayoutManager layout) {
    super(layout);
  }

  @Override
  public void paint(Graphics g) {
    if (myPaintCallback != null) {
      myPaintCallback.consume(g);
    }
    super.paint(g);
  }

  public void setPaintCallback(@Nullable Consumer<? super Graphics> paintCallback) {
    myPaintCallback = paintCallback;
  }
}
