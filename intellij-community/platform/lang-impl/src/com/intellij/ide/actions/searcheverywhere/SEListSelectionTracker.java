// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.components.JBList;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

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

  private int myLockCounter;
  private final List<Object> mySelectedItems = new ArrayList<>();
  private boolean myMoreSelected;

  SEListSelectionTracker(@NotNull JBList<?> list, @NotNull SearchEverywhereUI.SearchListModel model) {
    myList = list;
    myListModel = model;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (isLocked()) return;

    saveSelection();
  }

  void saveSelection() {
    mySelectedItems.clear();

    int[] indices = myList.getSelectedIndices();
    List<?> selectedItemsList;
    if (indices.length == 1 && myListModel.isMoreElement(indices[0])) {
      myMoreSelected = true;
      selectedItemsList = Collections.singletonList(myListModel.getElementAt(indices[0] - 1));
    }
    else {
      myMoreSelected = false;
      selectedItemsList = Arrays.stream(indices)
        .filter(i -> !myListModel.isMoreElement(i))
        .mapToObj(i -> myListModel.getElementAt(i))
        .collect(Collectors.toList());
    }

    mySelectedItems.addAll(selectedItemsList);
  }

  void restoreSelection() {
    if (isLocked()) return;

    lock();
    try {
      int[] indicesToSelect = calcIndicesToSelect();
      if (myMoreSelected && indicesToSelect.length == 1) {
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
      mySelectedItems.clear();
    }
  }

  void lock() {
    myLockCounter++;
  }

  void unlock() {
    if (myLockCounter > 0) myLockCounter--;
  }

  private boolean isLocked() {
    return myLockCounter > 0;
  }

  private int[] calcIndicesToSelect() {
    List<Object> items = myListModel.getItems();
    if (items.isEmpty()) return ArrayUtil.EMPTY_INT_ARRAY;

    return IntStream.range(0, items.size())
      .filter(i -> mySelectedItems.contains(items.get(i)))
      .toArray();
  }
}
