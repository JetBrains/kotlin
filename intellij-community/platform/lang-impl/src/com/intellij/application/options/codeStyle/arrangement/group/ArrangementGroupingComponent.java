// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.group;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementEditorAware;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementRepresentationAware;
import com.intellij.application.options.codeStyle.arrangement.util.ArrangementRuleIndexControl;
import com.intellij.psi.codeStyle.arrangement.ArrangementUtil;
import com.intellij.psi.codeStyle.arrangement.std.*;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Denis Zhdanov
 */
public class ArrangementGroupingComponent extends JPanel implements ArrangementRepresentationAware, ArrangementEditorAware {

  @NotNull private final ArrangementUiComponent      myGroupingTypeToken;
  @NotNull private final ArrangementRuleIndexControl myRowIndexControl;

  @Nullable private final ArrangementUiComponent myOrderTypeToken;

  /**
   * Assumes that given token {@link CompositeArrangementSettingsToken#getChildren() has no children} or all its children have
   * the {@link CompositeArrangementSettingsToken#getRole() same UI role}.
   * <p/>
   * Lays out given token (and its children if any) in a single row.
   *
   * @param token                       base token which serves as a grouping rule model
   * @param colorsProvider              colors provider
   *
   * @throws IllegalArgumentException   if invariant described above is not satisfied
   */
  public ArrangementGroupingComponent(@NotNull CompositeArrangementSettingsToken token,
                                      @NotNull ArrangementColorsProvider colorsProvider,
                                      @NotNull ArrangementStandardSettingsManager settingsManager)
    throws IllegalArgumentException
  {
    List<ArrangementSettingsToken> children = new ArrayList<>();
    StdArrangementTokenUiRole childRole = null;
    for (CompositeArrangementSettingsToken child : token.getChildren()) {
      if (childRole == null) {
        childRole = child.getRole();
        children.add(child.getToken());
      }
      else if (!childRole.equals(child.getRole())) {
        throw new IllegalArgumentException(String.format(
          "Can't build a grouping component for token '%s'. Reason: its children has different UI roles (%s and %s)",
          token, childRole, child.getRole()
        ));
      }
      else {
        children.add(child.getToken());
      }
    }

    FontMetrics metrics = getFontMetrics(getFont());
    int maxWidth = 0;
    for (int i = 0; i <= 99; i++) {
      maxWidth = Math.max(metrics.stringWidth(String.valueOf(i)), maxWidth);
    }
    int height = metrics.getHeight() - metrics.getDescent() - metrics.getLeading();
    int diameter = Math.max(maxWidth, height) * 5 / 3;
    myRowIndexControl = new ArrangementRuleIndexControl(diameter, height);

    myGroupingTypeToken = ArrangementUtil.buildUiComponent(
      token.getRole(), Collections.singletonList(token.getToken()), colorsProvider, settingsManager
    );

    if (children.size() <= 0) {
      myOrderTypeToken = null;
    }
    else {
      assert childRole != null;
      myOrderTypeToken = ArrangementUtil.buildUiComponent(childRole, children, colorsProvider, settingsManager);
      myGroupingTypeToken.setListener(new ArrangementUiComponent.Listener() {
        @Override
        public void stateChanged() {
          myOrderTypeToken.setEnabled(myGroupingTypeToken.isSelected());
        }
      });
    }

    init();
  }

  private void init() {
    setLayout(new GridBagLayout());
    add(myRowIndexControl,
        new GridBag().anchor(GridBagConstraints.CENTER)
          .insets(0, ArrangementConstants.HORIZONTAL_PADDING, 0, ArrangementConstants.HORIZONTAL_GAP * 2)
    );
    add(myGroupingTypeToken.getUiComponent(), new GridBag().anchor(GridBagConstraints.WEST).insets(0, 0, 0, 2));
    if (myOrderTypeToken != null) {
      add(myOrderTypeToken.getUiComponent(), new GridBag().anchor(GridBagConstraints.WEST));
    }
    add(new JLabel(" "), new GridBag().weightx(1).fillCellHorizontally());

    setBackground(UIUtil.getListBackground());
    setBorder(JBUI.Borders.empty(ArrangementConstants.VERTICAL_GAP));
    setOpaque(!UIUtil.isUnderIntelliJLaF() && !UIUtil.isUnderNativeMacLookAndFeel() && !StartupUiUtil.isUnderDarcula());
  }

  @Override
  protected void paintComponent(Graphics g) {
    Dimension size = getSize();
    if (size != null) {
      int baseline = myGroupingTypeToken.getBaselineToUse(size.width, size.height);
      if (baseline > 0) {
        baseline -= myRowIndexControl.getBounds().y;
        myRowIndexControl.setBaseLine(baseline);
      }
    }
    if (UIUtil.isUnderIntelliJLaF() || StartupUiUtil.isUnderDarcula() || UIUtil.isUnderNativeMacLookAndFeel()) {
      g.setColor(getBackground());
      g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
    }
    super.paintComponent(g);
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return this;
  }

  public boolean isSelected() {
    return myGroupingTypeToken.isSelected();
  }

  public void setSelected(boolean selected) {
    myGroupingTypeToken.setSelected(selected);
    refreshControl();
  }

  private void refreshControl() {
    boolean checked = isSelected();
    if (myOrderTypeToken != null) {
      myOrderTypeToken.setEnabled(checked);
    }
  }

  @NotNull
  public ArrangementSettingsToken getGroupingType() {
    ArrangementSettingsToken token = myGroupingTypeToken.getToken();
    assert token != null;
    return token;
  }

  public void setOrderType(@NotNull ArrangementSettingsToken type) {
    if (myOrderTypeToken != null) {
      myOrderTypeToken.chooseToken(type);
    }
  }

  public void setRowIndex(int row) {
    myRowIndexControl.setIndex(row);
  }

  public void setHighlight(boolean highlight) {
    setBackground(highlight ? UIUtil.getDecoratedRowColor() : UIUtil.getListBackground());
  }

  @Nullable
  public ArrangementSettingsToken getOrderType() {
    return myOrderTypeToken == null ? null : myOrderTypeToken.getToken();
  }

  @Override
  public String toString() {
    return myGroupingTypeToken.toString();
  }
}
