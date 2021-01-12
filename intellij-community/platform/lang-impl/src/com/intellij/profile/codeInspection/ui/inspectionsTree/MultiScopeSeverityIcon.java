// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.profile.codeInspection.ui.inspectionsTree;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeHighlighting.HighlightDisplayLevel.ColoredIcon;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.profile.codeInspection.ui.ScopeOrderComparator;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Dmitry Batkovich
 */
public class MultiScopeSeverityIcon implements Icon {
  private final LinkedHashMap<String, HighlightDisplayLevel> myScopeToAverageSeverityMap;
  private final String myDefaultScopeName;

  MultiScopeSeverityIcon(@NotNull Map<String, HighlightSeverity> scopeToAverageSeverityMap,
                         final String defaultScopeName,
                         @NotNull InspectionProfileImpl inspectionProfile) {
    myDefaultScopeName = defaultScopeName;
    final List<String> sortedScopeNames = new ArrayList<>(scopeToAverageSeverityMap.keySet());
    myScopeToAverageSeverityMap = new LinkedHashMap<>();
    sortedScopeNames.sort(new ScopeOrderComparator(inspectionProfile));
    sortedScopeNames.remove(defaultScopeName);
    sortedScopeNames.add(defaultScopeName);
    for (final String scopeName : sortedScopeNames) {
      final HighlightSeverity severity = scopeToAverageSeverityMap.get(scopeName);
      if (severity == null) {
        continue;
      }
      final HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
      if (level == null) {
        continue;
      }
      myScopeToAverageSeverityMap.put(scopeName, level);
    }
  }

  private static JBColor getMixedSeverityColor() {
    return JBColor.DARK_GRAY;
  }

  public String getDefaultScopeName() {
    return myDefaultScopeName;
  }

  public LinkedHashMap<String, HighlightDisplayLevel> getScopeToAverageSeverityMap() {
    return myScopeToAverageSeverityMap;
  }

  @Override
  public void paintIcon(final Component c, final Graphics g, final int i, final int j) {
    final int partWidth = getIconWidth() / myScopeToAverageSeverityMap.size();

    final Collection<HighlightDisplayLevel> values = myScopeToAverageSeverityMap.values();
    int idx = 0;
    for (final HighlightDisplayLevel level : values) {
      final Icon icon = level.getIcon();
      g.setColor(icon instanceof ColoredIcon ? ((ColoredIcon)icon).getColor() : getMixedSeverityColor());
      final int x = i + partWidth * idx;
      g.fillRect(x, j, partWidth, getIconHeight());
      idx++;
    }
  }

  @Override
  public int getIconWidth() {
    return HighlightDisplayLevel.getEmptyIconDim();
  }

  @Override
  public int getIconHeight() {
    return HighlightDisplayLevel.getEmptyIconDim();
  }
}
