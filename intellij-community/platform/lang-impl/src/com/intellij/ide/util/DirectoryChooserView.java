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

package com.intellij.ide.util;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author dsl
 */
public interface DirectoryChooserView {
  JComponent getComponent();

  void onSelectionChange(Runnable runnable);

  DirectoryChooser.ItemWrapper getItemByIndex(int i);

  void clearSelection();

  void selectItemByIndex(int selectionIndex);

  void addItem(DirectoryChooser.ItemWrapper itemWrapper);

  void listFilled();

  void clearItems();

  int getItemsSize();

  @Nullable
  DirectoryChooser.ItemWrapper getSelectedItem();
}
