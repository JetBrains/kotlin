// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ui;

import org.jetbrains.annotations.NotNull;

public class InspectionGroupNode extends InspectionTreeNode {
  private final String myGroup;

  InspectionGroupNode(@NotNull String group, @NotNull InspectionTreeNode parent) {
    super(parent);
    myGroup = group;
  }

  String getSubGroup() {
    return myGroup;
  }

  @Override
  public boolean appearsBold() {
    return true;
  }

  @Override
  public String getPresentableText() {
    return myGroup;
  }
}
