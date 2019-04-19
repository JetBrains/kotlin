/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ide.navigationToolbar;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author Konstantin Bulenkov
 */
public enum NavBarKeyboardCommand {
  LEFT(KeyEvent.VK_LEFT),
  RIGHT(KeyEvent.VK_RIGHT),
  HOME(KeyEvent.VK_HOME),
  END(KeyEvent.VK_END),
  DOWN(KeyEvent.VK_DOWN),
  UP(KeyEvent.VK_UP),
  ENTER(KeyEvent.VK_ENTER),
  NAVIGATE(KeyEvent.VK_F4),
  ESCAPE(KeyEvent.VK_ESCAPE);

  private final KeyStroke myKeyStroke;

  NavBarKeyboardCommand(int keyCode) {
    myKeyStroke = KeyStroke.getKeyStroke(keyCode, 0);
  }

  public KeyStroke getKeyStroke() {
    return myKeyStroke;
  }

  @Nullable
  public static NavBarKeyboardCommand fromString(String name) {
    if (name != null) {
      for (NavBarKeyboardCommand command : values()) {
        if (command.name().equals(name)) {
          return command;
        }
      }
    }
    return null;
  }
}
