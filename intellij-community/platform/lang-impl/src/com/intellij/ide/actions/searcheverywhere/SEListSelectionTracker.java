// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.components.JBList;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class SEListSelectionTracker implements ListSelectionListener {

  private final JBList<?> myList;
  private final SearchEverywhereUI.SearchListModel myListModel;

  private int lockCounter;
  private final List<Object> selectedItems = new ArrayList<>();
  private boolean moreSelected = false;

  SEListSelectionTracker(JBList<?> list, SearchEverywhereUI.SearchListModel model) {
    myList = list;
    myListModel = model;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (isLocked()) return;

    saveSelection();
  }

  void saveSelection() {
    selectedItems.clear();

    int[] indices = myList.getSelectedIndices();
    List<?> selectedItemsList;
    if (indices.length == 1 && myListModel.isMoreElement(indices[0])) {
      moreSelected = true;
      selectedItemsList = Collections.singletonList(myListModel.getElementAt(indices[0] - 1));
    }
    else {
      moreSelected = false;
      selectedItemsList = Arrays.stream(indices)
        .filter(i -> !myListModel.isMoreElement(i))
        .mapToObj(i -> myListModel.getElementAt(i))
        .collect(Collectors.toList());
    }

    selectedItems.addAll(selectedItemsList);
  }

  void restoreSelection() {
    if (isLocked()) return;

    lock();
    try {
      int[] indicesToSelect = calcIndicesToSelect();
      if (moreSelected && indicesToSelect.length == 1) {
        indicesToSelect[0] += 1;
      }

      if (indicesToSelect.length == 0) {
        indicesToSelect = new int[]{0};
      }

      myList.setSelectedIndices(indicesToSelect);
      ScrollingUtil.ensureRangeIsVisible(myList, indicesToSelect[0], indicesToSelect[indicesToSelect.length - 1]);
    }
    finally {
      unlock();
    }
  }

  void resetSelectionIfNeeded() {
    int[] indices = calcIndicesToSelect();
    if (indices.length == 0) {
      selectedItems.clear();
    }
  }

  void lock() {
    lockCounter++;
  }

  void unlock() {
    if (lockCounter > 0) lockCounter--;
  }

  private boolean isLocked() {
    return lockCounter > 0;
  }

  private int[] calcIndicesToSelect() {
    List<Object> items = myListModel.getItems();
    if (items.isEmpty()) return new int[0];

    return IntStream.range(0, items.size())
      .filter(i -> selectedItems.contains(items.get(i)))
      .toArray();
  }

}
