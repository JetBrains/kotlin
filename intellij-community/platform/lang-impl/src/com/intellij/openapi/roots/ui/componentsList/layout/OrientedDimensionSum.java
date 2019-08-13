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

public class OrientedDimensionSum {
  private final Orientation myOrientation;
  private final Dimension mySum = new Dimension();

  public OrientedDimensionSum(Orientation orientation) {
    myOrientation = orientation;
  }

  public void add(Dimension size) {
    myOrientation.expandInline(mySum, size);
  }

  public void addInsets(Insets insets) {
    mySum.width += insets.left + insets.right;
    mySum.height += insets.top + insets.bottom;
  }

  public Dimension getSum() { return mySum; }

  public void grow(int length) {
    myOrientation.extend(mySum, length);
  }

}
