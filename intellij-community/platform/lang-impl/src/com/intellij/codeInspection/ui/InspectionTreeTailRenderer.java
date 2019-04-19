// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.containers.ContainerUtil;

import java.awt.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * @author Dmitry Batkovich
 */
public abstract class InspectionTreeTailRenderer {
  private final static int MAX_LEVEL_TYPES = 5;

  private final static JBColor TREE_RED = new JBColor(new Color(184, 66, 55), new Color(204, 102, 102));
  private final static JBColor TREE_GRAY = new JBColor(Gray._153, Gray._117);

  private final Map<HighlightSeverity, String> myPluralizedSeverityNames = ContainerUtil.createSoftMap();
  private final Map<HighlightSeverity, String> myUnpluralizedSeverityNames = ContainerUtil.createSoftMap();

  private final GlobalInspectionContextImpl myContext;

  public InspectionTreeTailRenderer(GlobalInspectionContextImpl context) {
    myContext = context;
  }

  public void appendTailText(InspectionTreeNode node) {
    final String customizedTailText = node.getTailText();
    if (customizedTailText != null) {
      if (!customizedTailText.isEmpty()) {
        appendText("    ");
        appendText(customizedTailText, SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
    }
    else {
      appendText("  ");
      LevelAndCount[] problemLevels = node.getProblemLevels();
      if (problemLevels.length > MAX_LEVEL_TYPES) {
        int sum = Arrays.stream(problemLevels).mapToInt(LevelAndCount::getCount).sum();
        appendText(InspectionsBundle.message("inspection.problem.descriptor.count", sum, SimpleTextAttributes.GRAYED_ATTRIBUTES));
      }
      else {
        for (LevelAndCount levelAndCount : problemLevels) {
          SimpleTextAttributes attrs = SimpleTextAttributes.GRAY_ATTRIBUTES;
          attrs = attrs.derive(-1, levelAndCount.getLevel() == HighlightDisplayLevel.ERROR && !myContext.getUIOptions().GROUP_BY_SEVERITY
                                   ? TREE_RED
                                   : TREE_GRAY, null, null);
          appendText(levelAndCount.getCount() + " " + getPresentableName(levelAndCount.getLevel(), levelAndCount.getCount() > 1) + " ", attrs);
        }
      }
    }
  }

  protected abstract void appendText(String text, SimpleTextAttributes attributes);

  protected abstract void appendText(String text);

  private String getPresentableName(HighlightDisplayLevel level, boolean pluralize) {
    final HighlightSeverity severity = level.getSeverity();
    if (pluralize) {
      String name = myPluralizedSeverityNames.get(severity);
      if (name == null) {
        final String lowerCaseName = level.getName().toLowerCase(Locale.ENGLISH);
        name = SeverityRegistrar.isDefaultSeverity(severity) ? StringUtil.pluralize(lowerCaseName) : lowerCaseName;
        myPluralizedSeverityNames.put(severity, name);
      }
      return name;
    }
    else {
      String name = myUnpluralizedSeverityNames.get(severity);
      if (name == null) {
        name = level.getName().toLowerCase(Locale.ENGLISH);
        myUnpluralizedSeverityNames.put(severity, name);
      }
      return name;
    }
  }
}
