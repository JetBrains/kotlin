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
package com.intellij.ide.favoritesTreeView;

import com.intellij.ui.JBColor;

import java.awt.*;

public enum Flag {
  orange(JBColor.orange),
  blue(JBColor.blue),
  green(JBColor.green),
  red(JBColor.red),
  brown(new JBColor(new Color(0x804000), new Color(0x9C5700))),
  magenta(JBColor.magenta),
  violet(new JBColor(new Color(0x8000FF), new Color(0x9C57FF))),
  yellow(JBColor.yellow),
  grey(JBColor.lightGray);

  private final Color myColor;

  Flag(Color color) {
    myColor = color;
  }

  public Color getColor() {
    return myColor;
  }
}
