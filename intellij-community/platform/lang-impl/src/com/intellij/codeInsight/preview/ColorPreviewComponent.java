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

package com.intellij.codeInsight.preview;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author spleaner
 */
public class ColorPreviewComponent extends JComponent implements PreviewHintComponent {
  @NotNull
  private final Color myColor;

  public ColorPreviewComponent(@NotNull final Color color) {
    myColor = color;
    setOpaque(true);
  }

  @Override
  public void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D)g;

    final Rectangle r = getBounds();

    g2.setPaint(myColor);
    g2.fillRect(1, 1, r.width - 2, r.height - 2);

    g2.setPaint(Color.BLACK);
    g2.drawRect(0, 0, r.width - 1, r.height - 1);
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(70, 25);
  }

  @Override
  public boolean isEqualTo(@Nullable PreviewHintComponent other) {
    return other instanceof ColorPreviewComponent && myColor.equals(((ColorPreviewComponent)other).myColor);
  }
}
