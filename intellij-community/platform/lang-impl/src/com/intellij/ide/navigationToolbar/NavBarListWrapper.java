/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;

/**
* @author Konstantin Bulenkov
*/
class NavBarListWrapper extends JBScrollPane implements DataProvider {
  private static final int MAX_SIZE = 20;
  private final JList myList;

  NavBarListWrapper(final JList list) {
    super(list);
    list.addMouseMotionListener(new MouseMotionAdapter() {
      boolean myIsEngaged = false;
      @Override
      public void mouseMoved(MouseEvent e) {
        if (myIsEngaged && !UIUtil.isSelectionButtonDown(e)) {
          final Point point = e.getPoint();
          final int index = list.locationToIndex(point);
          list.setSelectedIndex(index);
        } else {
          myIsEngaged = true;
        }
      }
    });

    ScrollingUtil.installActions(list);

    final int modelSize = list.getModel().getSize();
    setBorder(BorderFactory.createEmptyBorder());
    if (modelSize > 0 && modelSize <= MAX_SIZE) {
      list.setVisibleRowCount(0);
      getViewport().setPreferredSize(list.getPreferredSize());
    } else {
      list.setVisibleRowCount(MAX_SIZE);
    }
    myList = list;
  }


  @Override
  @Nullable
  public Object getData(@NotNull @NonNls String dataId) {
    if (PlatformDataKeys.SELECTED_ITEM.is(dataId)){
      return myList.getSelectedValue();
    }
    if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)){
      return myList.getSelectedValues();
    }
    return null;
  }

  @Override
  public void setBorder(Border border) {
    if (myList != null){
      myList.setBorder(border);
    }
  }

  @Override
  public void requestFocus() {
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myList, true));
  }

  @Override
  public synchronized void addMouseListener(MouseListener l) {
    myList.addMouseListener(l);
  }
}
