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
package com.intellij.uiDesigner.shared;

import com.intellij.uiDesigner.core.AbstractLayout;

import java.awt.*;

public class XYLayoutManager extends AbstractLayout {
  public XYLayoutManager(){
  }

  @Override
  public Dimension maximumLayoutSize(final Container target){
    throw new UnsupportedOperationException();
  }

  @Override
  public Dimension preferredLayoutSize(final Container parent){
    throw new UnsupportedOperationException();
  }

  @Override
  public Dimension minimumLayoutSize(final Container parent){
    throw new UnsupportedOperationException();
  }

  @Override
  public void layoutContainer(final Container parent){
    throw new UnsupportedOperationException();
  }

  public void setPreferredSize(final Dimension size){
    throw new UnsupportedOperationException();
  }

  @Override
  public final void invalidateLayout(final Container target){
  }
}
