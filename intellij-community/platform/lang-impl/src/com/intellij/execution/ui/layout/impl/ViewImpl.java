// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.execution.ui.layout.Tab;
import com.intellij.execution.ui.layout.View;
import com.intellij.openapi.util.Key;

public class ViewImpl implements View {

  public static final Key<String> ID = Key.create("ViewID");

  private String myID;

  private Tab myTab;
  private int myTabIndex;

  private int myWindow;

  private PlaceInGrid myPlaceInGrid;

  private boolean myMinimizedInGrid;

  public ViewImpl(String id, TabImpl tab, final PlaceInGrid placeInGrid, boolean minimizedInGrid, int window) {
    myID = id;
    myTab = tab;
    myPlaceInGrid = placeInGrid;
    myMinimizedInGrid = minimizedInGrid;
    myWindow = window;
  }

  public ViewImpl() {
  }

  @Override
  public Tab getTab() {
    return myTab;
  }

  @Override
  public PlaceInGrid getPlaceInGrid() {
    return myPlaceInGrid;
  }


  @Override
  public boolean isMinimizedInGrid() {
    return myMinimizedInGrid;
  }

  public void setID(final String ID) {
    myID = ID;
  }

  public String getID() {
    return myID;
  }


  @Override
  public void setMinimizedInGrid(final boolean minimizedInGrid) {
    myMinimizedInGrid = minimizedInGrid;
  }

  @Override
  public void setPlaceInGrid(PlaceInGrid placeInGrid) {
    myPlaceInGrid = placeInGrid;
  }

  @Override
  public void assignTab(final Tab tab) {
    myTab = tab;
  }

  @Override
  public int getTabIndex() {
    return myTab != null ? myTab.getIndex() : myTabIndex;
  }

  @Override
  public void setTabIndex(final int tabIndex) {
    myTabIndex = tabIndex;
  }

  @Override
  public int getWindow() {
    return myWindow;
  }

  @Override
  public void setWindow(int windowNumber) {
    myWindow = windowNumber;
  }

  public static class Default {

    private final String myID;
    private final int myTabID;
    private final PlaceInGrid myPlaceInGrid;
    private final boolean myMinimizedInGrid;

    public Default(final String ID, final int tabID, final PlaceInGrid placeInGrid, final boolean minimizedInGrid) {
      myID = ID;
      myTabID = tabID;
      myPlaceInGrid = placeInGrid;
      myMinimizedInGrid = minimizedInGrid;
    }

    public ViewImpl createView(RunnerLayout settings) {
      final TabImpl tab = myTabID == Integer.MAX_VALUE ? settings.createNewTab() : settings.getOrCreateTab(myTabID);
      return new ViewImpl(myID, tab, myPlaceInGrid, myMinimizedInGrid, 0);
    }

    public PlaceInGrid getPlaceInGrid() {
      return myPlaceInGrid;
    }
  }

}
