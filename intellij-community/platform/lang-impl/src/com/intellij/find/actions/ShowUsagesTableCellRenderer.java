// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.psi.search.SearchScope;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.usages.TextChunk;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsagePresentation;
import com.intellij.usages.impl.GroupNode;
import com.intellij.usages.impl.UsageNode;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.usages.impl.UsageViewManagerImpl;
import com.intellij.usages.rules.UsageInFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

class ShowUsagesTableCellRenderer implements TableCellRenderer {
  private final UsageViewImpl myUsageView;
  @NotNull private final AtomicInteger myOutOfScopeUsages;
  @NotNull private final SearchScope mySearchScope;

  ShowUsagesTableCellRenderer(@NotNull UsageViewImpl usageView, @NotNull AtomicInteger outOfScopeUsages, @NotNull SearchScope searchScope) {
    myUsageView = usageView;
    myOutOfScopeUsages = outOfScopeUsages;
    mySearchScope = searchScope;
  }

  private static final int CURRENT_ASTERISK_COL = 0;
  private static final int FILE_GROUP_COL = 1;
  private static final int LINE_NUMBER_COL = 2;
  private static final int USAGE_TEXT_COL = 3;
  @Override
  public Component getTableCellRendererComponent(JTable list, Object value, boolean isSelected, boolean hasFocus, int row,
                                                 @MagicConstant(intValues = {CURRENT_ASTERISK_COL, FILE_GROUP_COL, LINE_NUMBER_COL, USAGE_TEXT_COL}) int column) {
    UsageNode usageNode = value instanceof UsageNode ? (UsageNode)value : null;
    Usage usage = usageNode == null ? null : usageNode.getUsage();

    Color fileBgColor = getBackgroundColor(isSelected, usage);
    Color selectionBg = UIUtil.getListSelectionBackground();
    Color selectionFg = UIUtil.getListSelectionForeground();
    Color rowBackground = isSelected ? selectionBg : fileBgColor == null ? list.getBackground() : fileBgColor;
    Color rowForeground = isSelected ? selectionFg : list.getForeground();

    if (usageNode == null || usageNode instanceof ShowUsagesAction.StringNode) {
      SimpleColoredComponent textChunks = new SimpleColoredComponent();
      textChunks.append(ObjectUtils.notNull(value, "").toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      return textComponentSpanningWholeRow(textChunks, rowBackground, rowForeground, column, list);
    }
    if (usage == ShowUsagesTable.MORE_USAGES_SEPARATOR) {
      SimpleColoredComponent textChunks = new SimpleColoredComponent();
      textChunks.append("...<");
      textChunks.append("more usages", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      textChunks.append(">...");
      return textComponentSpanningWholeRow(textChunks, rowBackground, rowForeground, column, list);
    }
    if (usage == ShowUsagesTable.USAGES_OUTSIDE_SCOPE_SEPARATOR) {
      SimpleColoredComponent textChunks = new SimpleColoredComponent();
      textChunks.append("...<");
      textChunks.append(UsageViewManagerImpl.outOfScopeMessage(myOutOfScopeUsages.get(), mySearchScope), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      textChunks.append(">...");
      return textComponentSpanningWholeRow(textChunks, rowBackground, rowForeground, column, list);
    }

    // want to be able to right-align the "current" word
    LayoutManager layout = column == USAGE_TEXT_COL
                           ? new BorderLayout() : new FlowLayout(column == LINE_NUMBER_COL ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0) {
      @Override
      public void layoutContainer(Container container) {
        super.layoutContainer(container);
        for (Component component : container.getComponents()) { // align inner components
          Rectangle b = component.getBounds();
          Insets insets = container.getInsets();
          component.setBounds(b.x, b.y, b.width, container.getSize().height - insets.top - insets.bottom);
        }
      }
    };
    JPanel panel = new JPanel(layout);
    panel.setFont(null);

    // greying the current usage the "find usages" was originated from
    boolean isOriginUsage = myUsageView.isOriginUsage(usage);
    if (isOriginUsage) {
      rowBackground = slightlyDifferentColor(rowBackground);
      if (fileBgColor != null) {
        fileBgColor = slightlyDifferentColor(fileBgColor);
      }
      selectionBg = slightlyDifferentColor(selectionBg);
    }
    panel.setBackground(rowBackground);
    panel.setForeground(rowForeground);

    SimpleColoredComponent textChunks = new SimpleColoredComponent();
    UsagePresentation presentation = usage.getPresentation();
    TextChunk[] text = presentation.getText();

    switch(column) {
      case CURRENT_ASTERISK_COL:
        if (isOriginUsage) {
          panel.add(new JLabel(isSelected ? AllIcons.General.ModifiedSelected : AllIcons.General.Modified));
        }
        break;
      case FILE_GROUP_COL:
        appendGroupText(list, (GroupNode)usageNode.getParent(), panel, fileBgColor, isSelected);
        break;
      case LINE_NUMBER_COL:
        if (text.length != 0) {
          TextChunk chunk = text[0];
          textChunks.append(chunk.getText(), getAttributes(isSelected, fileBgColor, selectionBg, selectionFg, chunk));
        }
        SpeedSearchUtil.applySpeedSearchHighlighting(list, textChunks, false, isSelected);

        panel.add(textChunks);
        break;

      case USAGE_TEXT_COL:
        Icon icon = presentation.getIcon();
        textChunks.setIcon(icon == null ? EmptyIcon.ICON_16 : icon);
        textChunks.append("").appendTextPadding(JBUIScale.scale(16 + 5));
        for (int i = 1; i < text.length; i++) {
          TextChunk chunk = text[i];
          textChunks.append(chunk.getText(), getAttributes(isSelected, fileBgColor, selectionBg, selectionFg, chunk));
        }
        SpeedSearchUtil.applySpeedSearchHighlighting(list, textChunks, false, isSelected);

        panel.add(textChunks);

        if (isOriginUsage) {
          SimpleColoredComponent origin = new SimpleColoredComponent();
          origin.setIconTextGap(JBUIScale.scale(5)); // for this particular icon it looks better

          // use attributes of "line number" to show "Current" word
          SimpleTextAttributes attributes =
            text.length == 0 ? SimpleTextAttributes.REGULAR_ATTRIBUTES.derive(-1, new Color(0x808080), null, null) :
            getAttributes(isSelected, fileBgColor, selectionBg, selectionFg, text[0]);
          origin.append("| Current", attributes);
          origin.appendTextPadding(JBUIScale.scale(45));
          panel.add(origin, BorderLayout.EAST);
        }
        break;

      default:
        throw new IllegalStateException("unknown column: " + column);
    }

    return panel;
  }

  @NotNull
  private static Color slightlyDifferentColor(@NotNull Color back) {
    return EditorColorsManager.getInstance().isDarkEditor() ?
           ColorUtil.brighter(back, 3) : // dunno, under the dark theme the "brighter,1" doesn't look bright enough so we use 3
           ColorUtil.hackBrightness(back, 1, 1/1.05f); // Olga insisted on very-pale almost invisible gray. oh well
  }

  @NotNull
  private static SimpleTextAttributes getAttributes(boolean isSelected, Color fileBgColor, Color selectionBg, Color selectionFg, @NotNull TextChunk chunk) {
    SimpleTextAttributes background = chunk.getSimpleAttributesIgnoreBackground();
    return isSelected
           ? new SimpleTextAttributes(selectionBg, selectionFg, null, background.getStyle())
           : deriveBgColor(background, fileBgColor);
  }

  @NotNull
  private static Component textComponentSpanningWholeRow(@NotNull SimpleColoredComponent chunks,
                                                         Color rowBackground,
                                                         Color rowForeground,
                                                         final int column,
                                                         @NotNull final JTable table) {
    final SimpleColoredComponent component = new SimpleColoredComponent() {
      @Override
      protected void doPaint(Graphics2D g) {
        int offset = 0;
        int i = 0;
        final TableColumnModel columnModel = table.getColumnModel();
        while (i < column) {
          offset += columnModel.getColumn(i).getWidth();
          i++;
        }
        g.translate(-offset, 0);

        // should increase the column width so that selection background will be visible even after offset translation
        setSize(getWidth()+offset, getHeight());

        super.doPaint(g);

        g.translate(+offset, 0);
      }

      @NotNull
      @Override
      public Dimension getPreferredSize() {
        //return super.getPreferredSize();
        return column == table.getColumnModel().getColumnCount()-1 ? super.getPreferredSize() : new Dimension(0,0);
        // it should span the whole row, so we can't return any specific value here,
        // because otherwise it would be used in the "max width" calculation in com.intellij.find.actions.ShowUsagesAction.calcMaxWidth
      }
    };

    component.setBackground(rowBackground);
    component.setForeground(rowForeground);

    for (SimpleColoredComponent.ColoredIterator iterator = chunks.iterator(); iterator.hasNext(); ) {
      iterator.next();
      String fragment = iterator.getFragment();
      SimpleTextAttributes attributes = iterator.getTextAttributes();
      attributes = attributes.derive(attributes.getStyle(), rowForeground, rowBackground, attributes.getWaveColor());
      component.append(fragment, attributes);
    }

    return component;
  }

  @NotNull
  private static SimpleTextAttributes deriveBgColor(@NotNull SimpleTextAttributes attributes, @Nullable Color fileBgColor) {
    if (fileBgColor != null) {
      attributes = attributes.derive(-1,null, fileBgColor,null);
    }
    return attributes;
  }

  private Color getBackgroundColor(boolean isSelected, Usage usage) {
    Color fileBgColor = null;
    if (isSelected) {
      fileBgColor = UIUtil.getListSelectionBackground();
    }
    else {
      VirtualFile virtualFile = usage instanceof UsageInFile ? ((UsageInFile)usage).getFile() : null;
      if (virtualFile != null) {
        Project project = myUsageView.getProject();
        Color color = VfsPresentationUtil.getFileBackgroundColor(project, virtualFile);
        if (color != null) fileBgColor = color;
      }
    }
    return fileBgColor;
  }

  private void appendGroupText(@NotNull JTable table,
                               final GroupNode node,
                               @NotNull JPanel panel,
                               Color fileBgColor,
                               boolean isSelected) {
    UsageGroup group = node == null ? null : node.getGroup();
    if (group == null) return;
    GroupNode parentGroup = (GroupNode)node.getParent();
    appendGroupText(table, parentGroup, panel, fileBgColor, isSelected);
    if (node.canNavigateToSource()) {
      SimpleColoredComponent renderer = new SimpleColoredComponent();
      renderer.setIcon(group.getIcon(false));
      SimpleTextAttributes attributes = deriveBgColor(SimpleTextAttributes.REGULAR_ATTRIBUTES, fileBgColor);
      renderer.append(group.getText(myUsageView), attributes);
      SpeedSearchUtil.applySpeedSearchHighlighting(table, renderer, false, isSelected);
      panel.add(renderer);
    }
  }
}
