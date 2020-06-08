// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.profile.codeInspection.ui.inspectionsTree;

import com.intellij.openapi.util.text.NaturalComparator;

import java.util.Comparator;

public final class InspectionsConfigTreeComparator {
  public static final Comparator<InspectionConfigTreeNode> INSTANCE =
    Comparator.<InspectionConfigTreeNode>comparingInt(n -> n instanceof InspectionConfigTreeNode.Group ? 0 : 1)
      .thenComparing(n -> getDisplayTextToSort(n.getText()), NaturalComparator.INSTANCE);

  public static String getDisplayTextToSort(String s) {
    if (s.isEmpty()) {
      return s;
    }
    while (!Character.isLetterOrDigit(s.charAt(0))) {
      s = s.substring(1);
      if (s.isEmpty()) {
        return s;
      }
    }
    return s;
  }
}