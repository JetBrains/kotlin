/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.util;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementMatchingRulesControl;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementUiComponent;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * @author Denis Zhdanov
 */
public class ArrangementListRowDecorator extends JPanel implements ArrangementUiComponent {

  @NotNull private final JLabel mySortLabel = new JLabel(AllIcons.ObjectBrowser.Sorted);

  @NotNull private final ArrangementRuleIndexControl     myRowIndexControl;
  @NotNull private final ArrangementUiComponent          myDelegate;
  @NotNull private final ArrangementMatchingRulesControl myControl;
  @NotNull private final MyActionButton                  myEditButton;

  @Nullable private Rectangle myScreenBounds;

  private boolean myBeingEdited;
  private boolean myUnderMouse;

  public ArrangementListRowDecorator(@NotNull ArrangementUiComponent delegate,
                                     @NotNull ArrangementMatchingRulesControl control)
  {
    myDelegate = delegate;
    myControl = control;

    mySortLabel.setVisible(false);

    AnAction action = ActionManager.getInstance().getAction("Arrangement.Rule.Edit");
    Presentation presentation = action.getTemplatePresentation().clone();
    Icon editIcon = presentation.getIcon();
    Dimension buttonSize = new Dimension(editIcon.getIconWidth(), editIcon.getIconHeight());
    myEditButton = new MyActionButton(action, presentation, ArrangementConstants.MATCHING_RULES_CONTROL_PLACE, buttonSize);
    myEditButton.setVisible(false);

    FontMetrics metrics = getFontMetrics(getFont());
    int maxWidth = 0;
    for (int i = 0; i <= 99; i++) {
      maxWidth = Math.max(metrics.stringWidth(String.valueOf(i)), maxWidth);
    }
    int height = metrics.getHeight() - metrics.getDescent() - metrics.getLeading();
    int diameter = Math.max(maxWidth, height) * 5 / 3;
    myRowIndexControl = new ArrangementRuleIndexControl(diameter, height);

    setOpaque(true);
    init();
  }

  public void setError(@Nullable String message) {
    myRowIndexControl.setError(StringUtil.isNotEmpty(message));
    setToolTipText(message);
  }

  private void init() {
    setLayout(new GridBagLayout());
    GridBag constraints = new GridBag().anchor(GridBagConstraints.CENTER)
      .insets(0, ArrangementConstants.HORIZONTAL_PADDING, 0, ArrangementConstants.HORIZONTAL_GAP * 2);
    add(myRowIndexControl, constraints);
    add(new InsetsPanel(mySortLabel), new GridBag().anchor(GridBagConstraints.CENTER).insets(0, 0, 0, ArrangementConstants.HORIZONTAL_GAP));
    add(myDelegate.getUiComponent(), new GridBag().weightx(1).anchor(GridBagConstraints.WEST));
    add(myEditButton, new GridBag().anchor(GridBagConstraints.EAST));
    setBorder(JBUI.Borders.empty(ArrangementConstants.VERTICAL_GAP));
  }

  @Override
  protected void paintComponent(Graphics g) {
    Point point = UIUtil.getLocationOnScreen(this);
    if (point != null) {
      Rectangle bounds = getBounds();
      myScreenBounds = new Rectangle(point.x, point.y, bounds.width, bounds.height);
    }
    
    FontMetrics metrics = g.getFontMetrics();
    int baseLine = SimpleColoredComponent.getTextBaseLine(metrics, metrics.getHeight());
    myRowIndexControl.setBaseLine(
      baseLine + ArrangementConstants.VERTICAL_GAP + myDelegate.getUiComponent().getBounds().y - myRowIndexControl.getBounds().y
    );
    super.paintComponent(g);
  }

  public void setRowIndex(int row) {
    myRowIndexControl.setIndex(row);
  }

  public void setUnderMouse(boolean underMouse) {
    myUnderMouse = underMouse;
    if (myUnderMouse) {
      setBackground(UIUtil.getDecoratedRowColor());
    }
    else {
      setBackground(UIUtil.getListBackground());
    }
  }

  public void setBeingEdited(boolean beingEdited) {
    if (myBeingEdited && !beingEdited) {
      Toggleable.setSelected(myEditButton.getPresentation(), false);
    }
    if (!beingEdited && !myUnderMouse) {
      myEditButton.setVisible(false);
    }
    if (beingEdited && !myBeingEdited) {
      myEditButton.setVisible(true);
      Toggleable.setSelected(myEditButton.getPresentation(), true);
    }
    myBeingEdited = beingEdited;
  }

  @NotNull
  @Override
  public ArrangementMatchCondition getMatchCondition() {
    return myDelegate.getMatchCondition();
  }

  @NotNull
  @Override
  public JComponent getUiComponent() {
    return this;
  }

  @Nullable
  @Override
  public Rectangle getScreenBounds() {
    return myScreenBounds;
  }

  @Override
  public void setSelected(boolean selected) {
    myDelegate.setSelected(selected); 
  }

  @Override
  public void setData(@NotNull Object data) {
    myDelegate.setData(data);
  }

  public void setShowSortIcon(boolean show) {
    mySortLabel.setVisible(show);
  }
  
  @Override
  public Rectangle onMouseEntered(@NotNull MouseEvent e) {
    setBackground(UIUtil.getDecoratedRowColor());
    myEditButton.setVisible(myControl.getSelectedModelRows().size() <= 1);
    return myDelegate.onMouseEntered(e);
  }

  @Nullable
  @Override
  public Rectangle onMouseMove(@NotNull MouseEvent event) {
    myEditButton.setVisible(myControl.getSelectedModelRows().size() <= 1);
    Rectangle bounds = getButtonScreenBounds();
    if (!myBeingEdited && bounds != null) {
      boolean selected = bounds.contains(event.getLocationOnScreen());
      boolean wasSelected = Toggleable.isSelected(myEditButton.getPresentation());
      Toggleable.setSelected(myEditButton.getPresentation(), selected);
      if (selected ^ wasSelected) {
        return myScreenBounds;
      }
    }

    return myDelegate.onMouseMove(event);
  }

  @Override
  public void onMouseRelease(@NotNull MouseEvent event) {
    myEditButton.setVisible(myControl.getSelectedModelRows().size() <= 1);
    Rectangle bounds = getButtonScreenBounds();
    if (bounds != null && bounds.contains(event.getLocationOnScreen())) {
      if (myBeingEdited) {
        myControl.hideEditor();
        myBeingEdited = false;
      }
      else {
        int row = myControl.getRowByRenderer(this);
        if (row >= 0) {
          myControl.showEditor(row);
          myControl.scrollRowToVisible(row);
          myBeingEdited = true;
        }
      }
      event.consume();
      return;
    }
    myDelegate.onMouseRelease(event); 
  }

  @Nullable
  @Override
  public Rectangle onMouseExited() {
    setBackground(UIUtil.getListBackground());
    if (!myBeingEdited) {
      myEditButton.setVisible(false);
    }
    return myDelegate.onMouseExited(); 
  }
  
  @Nullable
  private Rectangle getButtonScreenBounds() {
    if (myScreenBounds == null) {
      return null;
    }
    Rectangle bounds = myEditButton.getBounds();
    return new Rectangle(bounds.x + myScreenBounds.x, bounds.y + myScreenBounds.y, bounds.width, bounds.height); 
  }

  @Nullable
  @Override
  public ArrangementSettingsToken getToken() {
    return myDelegate.getToken();
  }

  @NotNull
  @Override
  public Set<ArrangementSettingsToken> getAvailableTokens() {
    return myDelegate.getAvailableTokens();
  }

  @Override
  public void chooseToken(@NotNull ArrangementSettingsToken data) throws IllegalArgumentException, UnsupportedOperationException {
    myDelegate.chooseToken(data); 
  }

  @Override
  public boolean isSelected() {
    return myDelegate.isSelected();
  }

  @Override
  public void reset() {
    myDelegate.reset(); 
  }

  @Override
  public int getBaselineToUse(int width, int height) {
    return myDelegate.getBaselineToUse(width, height);
  }

  @Override
  public void setListener(@NotNull Listener listener) {
    myDelegate.setListener(listener); 
  }

  @Override
  public void handleMouseClickOnSelected() {
    myDelegate.handleMouseClickOnSelected();
  }

  @Override
  public boolean alwaysCanBeActive() {
    return false;
  }

  @Override
  public String toString() {
    return "list row decorator for " + myDelegate.toString();
  }
  
  private static class MyActionButton extends ActionButton {
    MyActionButton(AnAction action, Presentation presentation, String place, @NotNull Dimension minimumSize) {
      super(action, presentation, place, minimumSize);
    }

    @NotNull
    public Presentation getPresentation() {
      return myPresentation;
    }
  }
}
