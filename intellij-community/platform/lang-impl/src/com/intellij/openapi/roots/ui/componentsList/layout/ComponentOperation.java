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

package com.intellij.openapi.roots.ui.componentsList.layout;


import java.awt.*;

public abstract class ComponentOperation {
  public abstract void applyTo(Component component);

  public static class SizeCalculator extends ComponentOperation {
    private final int myDefaultExtent;
    private final SizeProperty mySizeProperty;
    private final OrientedDimensionSum myDimensionSum;

    public SizeCalculator(int defaultExtent, SizeProperty sizeProperty, Orientation orientation) {
      myDefaultExtent = defaultExtent;
      mySizeProperty = sizeProperty;
      myDimensionSum = new OrientedDimensionSum(orientation);
    }

    protected SizeCalculator(SizeProperty sizeProperty) {
      this(0, sizeProperty, Orientation.VERTICAL);
    }

    @Override
    public void applyTo(Component component) {
      Dimension size = mySizeProperty.getSize(component);
      if (size != null) {
        myDimensionSum.add(size);
      } else
        myDimensionSum.grow(myDefaultExtent);
    }

    public OrientedDimensionSum getSum() {
      return myDimensionSum;
    }
  }

  public static class InlineLayout extends ComponentOperation {
    private final Point myPosition;
    private final int myParentExtent;
    private final int myDefaultExtent;
    private final SizeProperty mySizeProperty;
    private final Orientation myOrientation;

    public InlineLayout(Container parent, int defaultExtent, SizeProperty sizeProperty, Orientation orientation) {
      final Insets insets = parent.getInsets();

      myOrientation = orientation;
      mySizeProperty = sizeProperty;
      myDefaultExtent = defaultExtent;
      myParentExtent = myOrientation.getContrary().getInnerExtent(parent);
      myPosition = new Point(insets.left, insets.top);
    }

    @Override
    public void applyTo(Component component) {
      component.setSize(myParentExtent, myDefaultExtent);
      Dimension preferredSize = mySizeProperty.getSize(component);
      int height = getHeight(preferredSize);
      int width = getWidth(preferredSize);
      component.setBounds(myPosition.x, myPosition.y, width, height);
      myOrientation.advance(myPosition, width, height);
    }

    private int getHeight(Dimension preferredSize) {
      if (myOrientation.isVertical())
        return preferredSize != null ? preferredSize.height : myDefaultExtent;
      else
        return myParentExtent;
    }

    private int getWidth(Dimension preferredSize) {
      if (!myOrientation.isVertical())
        return preferredSize != null ? preferredSize.width : myDefaultExtent;
      else
        return myParentExtent;
    }
  }

}
