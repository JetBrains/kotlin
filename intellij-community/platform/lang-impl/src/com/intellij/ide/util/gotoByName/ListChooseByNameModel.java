// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Model allowing to show a fixed list of items, each having a name and description, in Choose by Name popup.
 */
public class ListChooseByNameModel<T extends ChooseByNameItem> extends SimpleChooseByNameModel {

  private static final int MAX_DESC_LENGTH = 80;
  private static final String ELLIPSIS_SUFFIX = "...";

  private Pattern myCompiledPattern;
  private String myPattern;
  private final List<? extends T> myItems;
  private final String myNotInMessage;

  public ListChooseByNameModel(@NotNull final Project project,
                               @NotNull String prompt,
                               @NotNull String notInMessage,
                               @NotNull List<? extends T> items) {
    super(project, prompt, null);

    myItems = items;
    myNotInMessage = notInMessage;
  }

  @Override
  public String[] getNames() {
    final ArrayList<String> taskFullCmds = new ArrayList<>();
    for (T item : myItems) {
      taskFullCmds.add(item.getName());
    }

    return ArrayUtilRt.toStringArray(taskFullCmds);
  }

  @Override
  protected Object[] getElementsByName(String name, String pattern) {
    for (T item : myItems) {
      if (item.getName().equals(name)) {
        return new Object[] { item };
      }
    }
    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }

  @NotNull
  @Override
  public String getNotInMessage() {
    return myNotInMessage;
  }

  @NotNull
  @Override
  public String getNotFoundMessage() {
    return myNotInMessage;
  }

  // from ruby plugin
  @NotNull
  @Override
  public ListCellRenderer getListCellRenderer() {
    return new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index, final boolean isSelected, final boolean cellHasFocus) {

        final JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBorder(JBUI.Borders.emptyRight(5));

        final Color bg = isSelected ? UIUtil.getListSelectionBackground(true) : UIUtil.getListBackground();
        panel.setBackground(bg);

        if (value instanceof ChooseByNameItem) {
          final ChooseByNameItem item = (ChooseByNameItem) value;

          final Color fg = isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();

          final JLabel actionLabel = new JLabel(item.getName(), null, LEFT);
          actionLabel.setBackground(bg);
          actionLabel.setForeground(fg);
          actionLabel.setFont(actionLabel.getFont().deriveFont(Font.BOLD));
          actionLabel.setBorder(JBUI.Borders.emptyLeft(4));

          panel.add(actionLabel, BorderLayout.WEST);

          String description = item.getDescription();
          if (description != null) {
            // truncate long descriptions
            final String normalizedDesc;
            if (description.length() > MAX_DESC_LENGTH) {
              normalizedDesc = description.substring(0, MAX_DESC_LENGTH) + ELLIPSIS_SUFFIX;
            }
            else {
              normalizedDesc = description;
            }
            final JLabel descriptionLabel = new JLabel(normalizedDesc);
            descriptionLabel.setBackground(bg);
            descriptionLabel.setForeground(fg);
            descriptionLabel.setBorder(JBUI.Borders.emptyLeft(15));

            panel.add(descriptionLabel, BorderLayout.EAST);
          }
        }
        else {
          // E.g. "..." item
          final JLabel actionLabel = new JLabel(value.toString(), null, LEFT);
          actionLabel.setBackground(bg);
          actionLabel.setForeground(UIUtil.getListForeground());
          actionLabel.setFont(actionLabel.getFont().deriveFont(Font.PLAIN));
          actionLabel.setBorder(JBUI.Borders.emptyLeft(4));
          panel.add(actionLabel, BorderLayout.WEST);
        }
        return panel;
      }
    };
  }

  @Override
  public String getElementName(@NotNull final Object element) {
    if (!(element instanceof ChooseByNameItem)) return null;
    return ((ChooseByNameItem)element).getName();
  }

  public boolean matches(@NotNull final String name, @NotNull final String pattern) {
    final Pattern compiledPattern = getTaskPattern(pattern);
    if (compiledPattern == null) {
      return false;
    }

    return new Perl5Matcher().matches(name, compiledPattern);
  }

  @Nullable
  private Pattern getTaskPattern(String pattern) {
    if (!Comparing.strEqual(pattern, myPattern)) {
      myCompiledPattern = null;
      myPattern = pattern;
    }
    if (myCompiledPattern == null) {
      final String regex = "^.*" + NameUtil.buildRegexp(pattern, 0, true, true);

      final Perl5Compiler compiler = new Perl5Compiler();
      try {
        myCompiledPattern = compiler.compile(regex);
      }
      catch (MalformedPatternException ignored) {
      }
    }
    return myCompiledPattern;
  }
}
