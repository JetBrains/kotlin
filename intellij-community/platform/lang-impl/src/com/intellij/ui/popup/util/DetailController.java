/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ui.popup.util;

import com.intellij.ui.components.JBList;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

public class DetailController {
  private final MasterController myMasterController;
  private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private DetailView myDetailView;
  private ItemWrapper mySelectedItem;

  public DetailController(MasterController masterController) {
    myMasterController = masterController;
  }

  public void setDetailView(@NotNull DetailView detailView) {
    myDetailView = detailView;
  }

  protected void doUpdateDetailViewWithItem(ItemWrapper wrapper) {
    if (wrapper != null) {
      wrapper.updateDetailView(myDetailView);
    }
    else {
      myDetailView.clearEditor();
      myDetailView.setPropertiesPanel(null);
      myDetailView.setCurrentItem(null);
    }
  }

  private String getTitle2Text(String fullText) {
    int labelWidth = getLabel().getWidth();
    if (fullText == null || fullText.length() == 0) return " ";
    while (getLabel().getFontMetrics(getLabel().getFont()).stringWidth(fullText) > labelWidth) {
      int sep = fullText.indexOf(File.separatorChar, 4);
      if (sep < 0) return fullText;
      fullText = "..." + fullText.substring(sep);
    }

    return fullText;
  }

  private JLabel getLabel() {
    return myMasterController.getPathLabel();
  }

  public ItemWrapper getSelectedItem() {
    return mySelectedItem;
  }

  public void doUpdateDetailView(boolean now) {
    final Object[] values = myMasterController.getSelectedItems();
    ItemWrapper wrapper = null;
    if (values != null && values.length == 1) {
      wrapper = (ItemWrapper)values[0];
      getLabel().setText(getTitle2Text(wrapper.footerText()));
    }
    else {
      getLabel().setText(" ");
    }
    mySelectedItem = wrapper;
    myUpdateAlarm.cancelAllRequests();
    if (now) {
      doUpdateDetailViewWithItem(mySelectedItem);
    }
    else {
      myUpdateAlarm.addRequest(() -> {
        doUpdateDetailViewWithItem(mySelectedItem);
        myUpdateAlarm.cancelAllRequests();
      }, 100);
    }
  }

  public void updateDetailView() {
    doUpdateDetailView(false);
  }

  public void setList(final JBList list) {
    final ListSelectionModel listSelectionModel = list.getSelectionModel();
    listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    if (list.getModel().getSize() == 0) {
      list.clearSelection();
    }
  }
}