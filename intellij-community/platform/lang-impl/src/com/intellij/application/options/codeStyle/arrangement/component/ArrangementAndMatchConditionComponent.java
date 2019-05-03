// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.component;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementMatchNodeComponentFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchConditionVisitor;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementUiComponent;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * {@link ArrangementUiComponent Component} for showing {@link ArrangementCompositeMatchCondition composite nodes}.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 */
public class ArrangementAndMatchConditionComponent extends JPanel implements ArrangementUiComponent {

  @NotNull private final List<ArrangementUiComponent>  myComponents      = new ArrayList<>();
  @NotNull private final Set<ArrangementSettingsToken> myAvailableTokens = new HashSet<>();

  @NotNull private final ArrangementCompositeMatchCondition mySetting;
  @Nullable private      Rectangle                          myScreenBounds;
  @Nullable private      ArrangementUiComponent             myComponentUnderMouse;

  public ArrangementAndMatchConditionComponent(@NotNull StdArrangementMatchRule rule,
                                               @NotNull ArrangementCompositeMatchCondition setting,
                                               @NotNull ArrangementMatchNodeComponentFactory factory,
                                               @NotNull ArrangementStandardSettingsManager manager,
                                               boolean allowModification)
  {
    mySetting = setting;
    setOpaque(false);
    setLayout(new GridBagLayout());
    final Map<ArrangementSettingsToken, ArrangementMatchCondition> operands =
      new HashMap<>();
    ArrangementMatchConditionVisitor visitor = new ArrangementMatchConditionVisitor() {
      @Override
      public void visit(@NotNull ArrangementAtomMatchCondition condition) {
        operands.put(condition.getType(), condition);
      }

      @Override
      public void visit(@NotNull ArrangementCompositeMatchCondition condition) {
        assert false;
      }
    };
    for (ArrangementMatchCondition operand : setting.getOperands()) {
      operand.invite(visitor);
    }

    List<ArrangementSettingsToken> ordered = manager.sort(operands.keySet());
    GridBagConstraints constraints = new GridBag().anchor(GridBagConstraints.EAST).insets(0, 0, 0, ArrangementConstants.HORIZONTAL_GAP);
    for (ArrangementSettingsToken key : ordered) {
      ArrangementMatchCondition operand = operands.get(key);
      assert operand != null;
      ArrangementUiComponent component = factory.getComponent(operand, rule, allowModification);
      myComponents.add(component);
      myAvailableTokens.addAll(component.getAvailableTokens());
      JComponent uiComponent = component.getUiComponent();
      add(uiComponent, constraints);
    }
  }

  @NotNull
  @Override
  public ArrangementMatchCondition getMatchCondition() {
    return mySetting;
  }

  @Override
  public void setData(@NotNull Object data) {
    // Do nothing
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
    for (ArrangementUiComponent component : myComponents) {
      component.setSelected(selected);
    }
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public void paint(Graphics g) {
    Point point = UIUtil.getLocationOnScreen(this);
    if (point != null) {
      Rectangle bounds = getBounds();
      myScreenBounds = new Rectangle(point.x, point.y, bounds.width, bounds.height);
    }
    super.paint(g);
  }

  @Override
  public Rectangle onMouseMove(@NotNull MouseEvent event) {
    Point location = event.getLocationOnScreen();
    for (ArrangementUiComponent component : myComponents) {
      Rectangle bounds = component.getScreenBounds();
      if (bounds == null || !bounds.contains(location)) {
        continue;
      }
      if (myComponentUnderMouse == null) {
        myComponentUnderMouse = component;
        Rectangle rectangleOnEnter = myComponentUnderMouse.onMouseEntered(event);
        Rectangle rectangleOnMove = myComponentUnderMouse.onMouseMove(event);
        if (rectangleOnEnter != null && rectangleOnMove != null) {
          return myScreenBounds; // Repaint row
        }
        else if (rectangleOnEnter != null) {
          return rectangleOnEnter;
        }
        else {
          return rectangleOnMove;
        }
      }
      else {
        if (myComponentUnderMouse != component) {
          myComponentUnderMouse.onMouseExited();
          myComponentUnderMouse = component;
          component.onMouseEntered(event);
          return myScreenBounds; // Repaint row.
        }
        else {
          return component.onMouseMove(event);
        }
      }
    }
    if (myComponentUnderMouse == null) {
      return null;
    }
    else {
      Rectangle result = myComponentUnderMouse.onMouseExited();
      myComponentUnderMouse = null;
      return result;
    }
  }

  @Override
  public void onMouseRelease(@NotNull MouseEvent event) {
    Point location = event.getLocationOnScreen();
    for (ArrangementUiComponent component : myComponents) {
      Rectangle bounds = component.getScreenBounds();
      if (bounds != null && bounds.contains(location)) {
        component.onMouseRelease(event);
        return;
      }
    }
  }

  @Override
  public Rectangle onMouseEntered(@NotNull MouseEvent event) {
    Point location = event.getLocationOnScreen();
    for (ArrangementUiComponent component : myComponents) {
      Rectangle bounds = component.getScreenBounds();
      if (bounds != null && bounds.contains(location)) {
        myComponentUnderMouse = component;
        return component.onMouseEntered(event);
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Rectangle onMouseExited() {
    if (myComponentUnderMouse != null) {
      Rectangle result = myComponentUnderMouse.onMouseExited();
      myComponentUnderMouse = null;
      return result;
    }
    return null;
  }

  @Nullable
  @Override
  public ArrangementSettingsToken getToken() {
    return myComponentUnderMouse == null ? null : myComponentUnderMouse.getToken();
  }

  @NotNull
  @Override
  public Set<ArrangementSettingsToken> getAvailableTokens() {
    return myAvailableTokens;
  }

  @Override
  public void chooseToken(@NotNull ArrangementSettingsToken data) throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSelected() {
    return myComponentUnderMouse != null && myComponentUnderMouse.isSelected();
  }

  @Override
  public void reset() {
    for (ArrangementUiComponent component : myComponents) {
      component.reset();
    }
  }

  @Override
  public int getBaselineToUse(int width, int height) {
    return -1;
  }

  @Override
  public void setListener(@NotNull Listener listener) {
    for (ArrangementUiComponent component : myComponents) {
      component.setListener(listener);
    }
  }

  @Override
  public void handleMouseClickOnSelected() {
    for (ArrangementUiComponent component : myComponents) {
      component.handleMouseClickOnSelected();
    }
  }

  @Override
  public boolean alwaysCanBeActive() {
    return false;
  }

  @Override
  public String toString() {
    return String.format("(%s)", StringUtil.join(myComponents, " and "));
  }
}
