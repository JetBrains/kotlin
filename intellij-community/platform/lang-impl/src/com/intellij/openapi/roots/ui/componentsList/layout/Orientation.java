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

public abstract class Orientation {
  public static Orientation VERTICAL = new Orientation() {
    @Override
    public int getExtent(Insets insets) {
      return insets.top + insets.bottom;
    }

    @Override
    public Orientation getContrary() {
      return HORIZONTAL;
    }

    @Override
    public int getExtent(Container container) {
      return container.getHeight();
    }

    @Override
    public void advance(Point point, int width, int height) {
      point.y += height;
    }

    @Override
    public boolean isVertical() {
      return true;
    }

    @Override
    public void extend(Dimension dimension, int extend) {
      dimension.height += extend;
    }

    @Override
    public void expandInline(Dimension dimension, Dimension extend) {
      dimension.height += extend.height;
      dimension.width = Math.max(dimension.width, extend.width);
    }
  };

  public static Orientation HORIZONTAL = new Orientation() {
    @Override
    public int getExtent(Insets insets) {
      return insets.left + insets.right;
    }

    @Override
    public Orientation getContrary() {
      return VERTICAL;
    }

    @Override
    public int getExtent(Container container) {
      return container.getWidth();
    }

    @Override
    public void advance(Point point, int width, int height) {
      point.x += width;
    }

    @Override
    public boolean isVertical() {
      return false;
    }

    @Override
    public void extend(Dimension dimension, int extend) {
      dimension.width += extend;
    }

    @Override
    public void expandInline(Dimension dimension, Dimension extend) {
      dimension.width += extend.width;
      dimension.height = Math.max(dimension.height, extend.height);
    }
  };

  public int getInnerExtent(Container container) {
    return getExtent(container) - getExtent(container.getInsets());
  }

  public abstract int getExtent(Insets insets);

  public abstract Orientation getContrary();

  public abstract int getExtent(Container container);

  public abstract void advance(Point point, int width, int height);

  public abstract boolean isVertical();

  public abstract void extend(Dimension dimension, int extend);

  public abstract void expandInline(Dimension dimension, Dimension extend);
}
