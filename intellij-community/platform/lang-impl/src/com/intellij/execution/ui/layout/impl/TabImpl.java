// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.execution.ui.layout.Tab;

import javax.swing.*;

public class TabImpl extends AbstractTab implements Tab {
  TabImpl() {
  }

  @Override
  public int getIndex() {
    return myIndex;
  }

  @Override
  public int getDefaultIndex() {
    return myDefaultIndex >= 0 ? myDefaultIndex : myIndex;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  public Icon getIcon() {
    return myIcon;
  }

  public void setIndex(final int index) {
    myIndex = index;
  }

  public void setDefaultIndex(final int index) {
    myDefaultIndex = index;
  }

  public void setDisplayName(final String displayName) {
    myDisplayName = displayName;
  }

  public float getLeftProportion() {
    return myLeftProportion;
  }

  public void setLeftProportion(final float leftProportion) {
    if (leftProportion < 0 || leftProportion > 1.0) return;
    myLeftProportion = leftProportion;
  }

  public float getRightProportion() {
    return myRightProportion;
  }

  public void setRightProportion(final float rightProportion) {
    if (rightProportion < 0 || rightProportion > 1.0) return;
    myRightProportion = rightProportion;
  }

  public float getBottomProportion() {
    return myBottomProportion;
  }

  public void setBottomProportion(final float bottomProportion) {
    if (bottomProportion < 0 || bottomProportion > 1.0) return;
    myBottomProportion = bottomProportion;
  }

  public boolean isLeftDetached() {
    return myLeftDetached;
  }

  public void setLeftDetached(final boolean leftDetached) {
    myLeftDetached = leftDetached;
  }

  public boolean isCenterDetached() {
    return myCenterDetached;
  }

  public void setCenterDetached(final boolean centerDetached) {
    myCenterDetached = centerDetached;
  }

  public boolean isRightDetached() {
    return myRightDetached;
  }

  public void setRightDetached(final boolean rightDetached) {
    myRightDetached = rightDetached;
  }

  public boolean isBottomDetached() {
    return myBottomDetached;
  }

  public void setBottomDetached(final boolean bottomDetached) {
    myBottomDetached = bottomDetached;
  }

  @Override
  public boolean isDefault() {
    return myIndex == 0;
  }

  @Override
  public boolean isDetached(PlaceInGrid place) {
    switch (place) {
      case bottom:
        return isBottomDetached();
      case center:
        return isCenterDetached();
      case left:
        return isLeftDetached();
      case right:
        return isRightDetached();
    }

    return false;
  }

  @Override
  public void setDetached(PlaceInGrid place, boolean detached) {
    switch (place) {
      case bottom:
        setBottomDetached(detached);
        break;
      case center:
        setCenterDetached(detached);
        break;
      case left:
        setLeftDetached(detached);
        break;
      case right:
        setRightDetached(detached);
        break;
    }
  }

  public static final class Default extends AbstractTab {
    public Default(final int index, final String displayName, final Icon icon) {
      myIndex = index;
      myDefaultIndex = index;
      myDisplayName = displayName;
      myIcon = icon;
    }

    public TabImpl createTab() {
      final TabImpl tab = new TabImpl();
      tab.copyFrom(this);
      return tab;
    }
  }
}
