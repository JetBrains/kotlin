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

public class VerticalStackLayout implements LayoutManager2 {
  private static final int myDefaultHeight = 200;

  /**
   * Calculates the minimum size dimensions for the specified
   * container, given the components it contains.
   * @param parent the component to be laid out
   * @see #preferredLayoutSize
   */
  @Override
  public Dimension minimumLayoutSize(Container parent) {
    ComponentOperation.SizeCalculator calculator = new ComponentOperation.SizeCalculator(SizeProperty.MINIMUM_SIZE);
    withAllVisibleDo(parent, calculator);
    OrientedDimensionSum result = calculator.getSum();
    result.addInsets(parent.getInsets());
    return result.getSum();
  }

  public static void withAllVisibleDo(Container container, ComponentOperation operation) {
    Component[] components = container.getComponents();
    for (Component component : components) {
      if (!component.isVisible()) continue;
      operation.applyTo(component);
    }
  }

  /**
   * Lays out the specified container.
   * @param parent the container to be laid out
   */
  @Override
  public void layoutContainer(final Container parent) {
    withAllVisibleDo(parent,
                     new ComponentOperation.InlineLayout(parent, myDefaultHeight, SizeProperty.PREFERED_SIZE,
                                                         Orientation.VERTICAL));
  }

  /**
   * Calculates the preferred size dimensions for the specified
   * container, given the components it contains.
   * @param parent the container to be laid out
   *
   * @see #minimumLayoutSize
   */
  @Override
  public Dimension preferredLayoutSize(Container parent) {
    ComponentOperation.SizeCalculator calculator =
        new ComponentOperation.SizeCalculator(myDefaultHeight, SizeProperty.PREFERED_SIZE, Orientation.VERTICAL);
    withAllVisibleDo(parent, calculator);
    OrientedDimensionSum result = calculator.getSum();
    result.addInsets(parent.getInsets());
    return result.getSum();
  }

  @Override
  public void removeLayoutComponent(Component comp) {}
  @Override
  public Dimension maximumLayoutSize(Container target) { return null; }
  @Override
  public float getLayoutAlignmentY(Container target) { return 0; }
  @Override
  public void addLayoutComponent(Component comp, Object constraints) {}
  @Override
  public void invalidateLayout(Container target) {}
  @Override
  public void addLayoutComponent(String name, Component comp) {}
  @Override
  public float getLayoutAlignmentX(Container target) { return 0; }
}
