/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.util;

import com.intellij.util.ui.GridBag;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Panel which occupies horizontal and vertical space even if it's content is invisible.
 * 
 * @author Denis Zhdanov
 */
public class InsetsPanel extends JPanel {

  @NotNull private final JComponent myContent;

  public InsetsPanel(@NotNull JComponent content) {
    super(new GridBagLayout());
    setOpaque(false);
    myContent = content;
    add(myContent, new GridBag().fillCell().weightx(1).weighty(1));
  }

  @Override
  public Dimension getPreferredSize() {
    return myContent.getPreferredSize();
  }

  @Override
  public Dimension getMinimumSize() {
    return myContent.getMinimumSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return myContent.getMaximumSize();
  }
}
