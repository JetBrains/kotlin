// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.actions;

import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.text.Matcher;
import com.intellij.util.text.MatcherHolder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
* @author Konstantin Bulenkov
*/
public class InspectionListCellRenderer extends DefaultListCellRenderer {
  private final SimpleTextAttributes mySelected;
  private final SimpleTextAttributes myPlain;
  private final SimpleTextAttributes myHighlighted;

  public InspectionListCellRenderer() {
    mySelected = new SimpleTextAttributes(UIUtil.getListSelectionBackground(true),
                                          UIUtil.getListSelectionForeground(),
                                          JBColor.RED,
                                          SimpleTextAttributes.STYLE_PLAIN);
    myPlain = new SimpleTextAttributes(UIUtil.getListBackground(),
                                       UIUtil.getListForeground(),
                                       JBColor.RED,
                                       SimpleTextAttributes.STYLE_PLAIN);
    myHighlighted = new SimpleTextAttributes(UIUtil.getListBackground(),
                                             UIUtil.getListForeground(),
                                             null,
                                             SimpleTextAttributes.STYLE_SEARCH_MATCH);
  }


  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean sel, boolean focus) {
    final BorderLayout layout = new BorderLayout();
    layout.setHgap(5);
    final JPanel panel = new JPanel(layout);
    panel.setOpaque(true);

    final Color bg = sel ? UIUtil.getListSelectionBackground(true) : UIUtil.getListBackground();
    final Color fg = sel ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    panel.setBackground(bg);
    panel.setForeground(fg);
    if (value instanceof InspectionElement) {
      final InspectionToolWrapper toolWrapper = ((InspectionElement)value).getToolWrapper();
      final String inspectionName = "  " + toolWrapper.getDisplayName();
      final String groupName = StringUtil.join(toolWrapper.getGroupPath(), " | ");
      final String matchingText = inspectionName + "|" + groupName;
      Matcher matcher = MatcherHolder.getAssociatedMatcher(list);
      List<TextRange> fragments = matcher == null ? null : ((MinusculeMatcher)matcher).matchingFragments(matchingText);
      List<TextRange> adjustedFragments = new ArrayList<>();
      if (fragments != null) {
        adjustedFragments.addAll(fragments);
      }
      final int splitPoint = adjustRanges(adjustedFragments, inspectionName.length() + 1);
      final SimpleColoredComponent c = new SimpleColoredComponent();
      final boolean matchHighlighting = Registry.is("ide.highlight.match.in.selected.only") && !sel;
      if (matchHighlighting) {
        c.append(inspectionName, myPlain);
      }
      else {
        final List<TextRange> ranges = adjustedFragments.subList(0, splitPoint);
        SpeedSearchUtil.appendColoredFragments(c, inspectionName, ranges, sel ? mySelected : myPlain, myHighlighted);
      }
      panel.add(c, BorderLayout.WEST);

      final SimpleColoredComponent group = new SimpleColoredComponent();
      if (matchHighlighting) {
        group.append(groupName, SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
      else {
        final SimpleTextAttributes attributes = sel ? mySelected : SimpleTextAttributes.GRAYED_ATTRIBUTES;
        final List<TextRange> ranges = adjustedFragments.subList(splitPoint, adjustedFragments.size());
        SpeedSearchUtil.appendColoredFragments(group, groupName, ranges, attributes, myHighlighted);
      }
      final JPanel right = new JPanel(new BorderLayout());
      right.setBackground(bg);
      right.setForeground(fg);
      right.add(group, BorderLayout.CENTER);
      final JLabel icon = new JLabel(getIcon(toolWrapper));
      icon.setBackground(bg);
      icon.setForeground(fg);
      right.add(icon, BorderLayout.EAST);
      panel.add(right, BorderLayout.EAST);
    }
    else {
      // E.g. "..." item
      return super.getListCellRendererComponent(list, value, index, sel, focus);
    }

    return panel;
  }

  private static int adjustRanges(List<TextRange> ranges, int offset) {
    int result = 0;
    for (int i = 0; i < ranges.size(); i++) {
      final TextRange range = ranges.get(i);
      final int startOffset = range.getStartOffset();
      if (startOffset < offset) {
        result = i + 1;
      }
      else {
        ranges.set(i, new TextRange(startOffset - offset, range.getEndOffset() - offset));
      }
    }
    return result;
  }

  @NotNull
  private static Icon getIcon(@NotNull InspectionToolWrapper tool) {
    Icon icon = null;
    final Language language = Language.findLanguageByID(tool.getLanguage());
    if (language != null) {
      final LanguageFileType fileType = language.getAssociatedFileType();
      if (fileType != null) {
        icon = fileType.getIcon();
      }
    }
    if (icon == null) {
      icon = UnknownFileType.INSTANCE.getIcon();
    }
    assert icon != null;
    return icon;
  }

}
