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
package com.intellij.codeInspection.actions;

import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * @author Dmitry Avdeev
 */
class FileFilterPanel {
  private JCheckBox myUseFileMask;
  private JComboBox myFileMask;
  private JPanel myPanel;

  void init(AnalysisUIOptions options) {
    FindInProjectUtil.initFileFilter(myFileMask, myUseFileMask);
    myUseFileMask.setSelected(StringUtil.isNotEmpty(options.FILE_MASK));
    myFileMask.setEnabled(StringUtil.isNotEmpty(options.FILE_MASK));
    myFileMask.setSelectedItem(options.FILE_MASK);
    ActionListener listener = __ -> options.FILE_MASK = myUseFileMask.isSelected() ? (String)myFileMask.getSelectedItem() : null;
    myUseFileMask.addActionListener(listener);
    myFileMask.addActionListener(listener);
  }

  @Nullable
  GlobalSearchScope getSearchScope() {
    if (!myUseFileMask.isSelected()) return null;
    String text = (String)myFileMask.getSelectedItem();
    if (text == null) return null;

    final Condition<CharSequence> patternCondition = FindInProjectUtil.createFileMaskCondition(text);
    return new GlobalSearchScope() {
      @Override
      public boolean contains(@NotNull VirtualFile file) {
        return patternCondition.value(file.getNameSequence());
      }

      @Override
      public boolean isSearchInModuleContent(@NotNull Module aModule) {
        return true;
      }

      @Override
      public boolean isSearchInLibraries() {
        return true;
      }
    };
  }
  
  JPanel getPanel() {
    return myPanel;
  }
}
