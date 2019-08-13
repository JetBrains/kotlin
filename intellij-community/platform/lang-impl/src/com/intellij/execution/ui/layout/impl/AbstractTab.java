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

package com.intellij.execution.ui.layout.impl;

import javax.swing.*;

abstract class AbstractTab {

  int myIndex;
  int myDefaultIndex = -1;
  String myDisplayName;
  Icon myIcon;

  float myLeftProportion = .2f;
  float myRightProportion = .2f;
  float myBottomProportion = .5f;

  boolean myLeftDetached = false;

  boolean myCenterDetached = false;

  boolean myRightDetached = false;

  boolean myBottomDetached = false;

  void copyFrom(final AbstractTab from) {
    myIndex = from.myIndex;
    myDefaultIndex = from.myDefaultIndex;
    myDisplayName = from.myDisplayName;
    myIcon = from.myIcon;

    myLeftProportion = from.myLeftProportion;
    myRightProportion = from.myRightProportion;
    myBottomProportion = from.myBottomProportion;

    myLeftDetached = from.myLeftDetached;
    myCenterDetached = from.myCenterDetached;
    myRightDetached = from.myRightDetached;
    myBottomDetached = from.myBottomDetached;
  }

}