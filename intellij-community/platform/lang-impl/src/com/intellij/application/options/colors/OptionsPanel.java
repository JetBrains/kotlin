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

package com.intellij.application.options.colors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Set;


public interface OptionsPanel {
  void addListener(ColorAndFontSettingsListener listener);

  JPanel getPanel();

  void updateOptionsList();

  Runnable showOption(String option);

  void applyChangesToScheme();

  void selectOption(String typeToSelect);

  Set<String> processListOptions();
  
  default void setEmptyText(@NotNull String text, @Nullable ActionListener linkListener) {}
}
