/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.component;

import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Denis Zhdanov
 */
public class ArrangementLabelUiComponent extends AbstractArrangementUiComponent {

  @NotNull private final ArrangementAtomMatchCondition myCondition;
  @NotNull private final JLabel                        myLabel;

  public ArrangementLabelUiComponent(@NotNull ArrangementSettingsToken token) {
    super(token);
    myCondition = new ArrangementAtomMatchCondition(token);
    myLabel = new JLabel(token.getRepresentationValue());
  }

  @NotNull
  @Override
  public ArrangementSettingsToken getToken() {
    return myCondition.getType();
  }

  @Override
  public void chooseToken(@NotNull ArrangementSettingsToken data) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public ArrangementMatchCondition getMatchCondition() {
    return myCondition;
  }

  @Override
  protected JComponent doGetUiComponent() {
    return myLabel;
  }

  @Override
  public boolean isSelected() {
    return true;
  }

  @Override
  public void setSelected(boolean selected) {
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void setEnabled(boolean enabled) {
  }

  @Override
  protected void doReset() {
  }
  
  @Override
  public int getBaselineToUse(int width, int height) {
    return myLabel.getBaseline(width, height);
  }

  @Override
  public void handleMouseClickOnSelected() {
    setSelected(false);
  }
}
