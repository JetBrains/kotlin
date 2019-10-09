/*
/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.application.options.codeStyle;

import com.intellij.application.options.schemes.AbstractSchemeActions;
import com.intellij.application.options.schemes.SchemesModel;
import com.intellij.application.options.schemes.SimpleSchemesPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.modifier.CodeStyleSettingsModifier;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemeImpl;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.util.ui.JBDimension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CodeStyleSchemesPanel extends SimpleSchemesPanel<CodeStyleScheme> {
  
  private final CodeStyleSchemesModel myModel;
  
  private boolean myIsReset;

  private JLabel myBottomLabel;
  private JPanel myBottomPanel;

  public CodeStyleSchemesPanel(CodeStyleSchemesModel model, int vGap) {
    super(vGap);
    myModel = model;
  }

  CodeStyleSchemesPanel(CodeStyleSchemesModel model) {
    super(DEFAULT_VGAP);
    myModel = model;
    showOverridingMessage(myModel.getOverridingStatus());
  }

  private void onCombo() {
    CodeStyleScheme selected = getSelectedScheme();
    if (selected != null) {
      myModel.selectScheme(selected, this);
    }
  }

  public void resetSchemesCombo() {
    myIsReset = true;
    try {
      List<CodeStyleScheme> schemes = new ArrayList<>(myModel.getAllSortedSchemes());
      resetSchemes(schemes);
      selectScheme(myModel.getSelectedScheme());
    }
    finally {
      myIsReset = false;
    }
  }

  public void onSelectedSchemeChanged() {
    myIsReset = true;
    try {
      selectScheme(myModel.getSelectedScheme());
    }
    finally {
      myIsReset = false;
    }
  }

  @NotNull
  @Override
  protected AbstractSchemeActions<CodeStyleScheme> createSchemeActions() {
    return
      new CodeStyleSchemesActions(this) {

        @Override
        protected void onSchemeChanged(@Nullable CodeStyleScheme scheme) {
          if (!myIsReset) {
            ApplicationManager.getApplication().invokeLater(() -> onCombo());
          }
        }

        @Override
        protected void renameScheme(@NotNull CodeStyleScheme scheme, @NotNull String newName) {
          CodeStyleSchemeImpl newScheme = new CodeStyleSchemeImpl(newName, false, scheme);
          myModel.addScheme(newScheme, false);
          myModel.removeScheme(scheme);
          myModel.selectScheme(newScheme, null);
        }
      };
  }

  @NotNull
  @Override
  public SchemesModel<CodeStyleScheme> getModel() {
    return myModel;
  }

  @Override
  protected boolean supportsProjectSchemes() {
    return true;
  }

  @Override
  protected boolean highlightNonDefaultSchemes() {
    return true;
  }

  @Override
  public boolean useBoldForNonRemovableSchemes() {
    return true;
  }

  @Nullable
  @Override
  protected JComponent createBottomComponent() {
    myBottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JLabel iconLabel = new JLabel();
    iconLabel.setIcon(AllIcons.General.Warning);
    myBottomPanel.add(iconLabel);
    myBottomPanel.add(Box.createRigidArea(new JBDimension(5,0)));
    myBottomLabel = new JLabel();
    myBottomPanel.add(myBottomLabel);
    LinkLabel<Object> disableHyperLink = new LinkLabel<>("Disable", null, new LinkListener<Object>() {
      @Override
      public void linkSelected(LinkLabel aSource, Object aLinkData) {
        disableOverriding();
      }
    });
    myBottomPanel.add(disableHyperLink);
    myBottomPanel.setVisible(false);
    return myBottomPanel;
  }

  private void disableOverriding() {
    CodeStyleSchemesModel.OverridingStatus status = myModel.getOverridingStatus();
    if (status != null) {
      final CodeStyleScheme currScheme = getSelectedScheme();
      final CodeStyleSettings currSettings = currScheme.getCodeStyleSettings();
      final CodeStyleSettings modelSettings = myModel.getCloneSettings(currScheme);
      for (CodeStyleSettingsModifier modifier : status.getModifiers()) {
        Consumer<CodeStyleSettings> disablingFunction = modifier.getDisablingFunction();
        if (disablingFunction != null) {
          disablingFunction.accept(currSettings);
          CodeStyleSettingsManager.getInstance(myModel.getProject()).notifyCodeStyleSettingsChanged();
          disablingFunction.accept(modelSettings);
        }
      }
      myModel.updateOverridingStatus();
      myModel.fireModelSettingsChanged(modelSettings);
    }
    else {
      myBottomPanel.setVisible(false);
    }
  }

  public final void updateOverridingMessage() {
    showOverridingMessage(myModel.getOverridingStatus());
  }

  private void showOverridingMessage(@Nullable CodeStyleSchemesModel.OverridingStatus overridingStatus) {
    if (overridingStatus != null) {
      CodeStyleSettingsModifier[] modifiers = overridingStatus.getModifiers();
      myBottomLabel.setText(getMessage(modifiers));
      myBottomPanel.setVisible(true);
      return;
    }
    myBottomPanel.setVisible(false);
  }

  private static String getMessage(@NotNull CodeStyleSettingsModifier[] modifiers) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Settings may be overridden by ");
    boolean isList = false;
    for (CodeStyleSettingsModifier modifier : modifiers) {
      if (isList) messageBuilder.append(", ");
      messageBuilder.append(modifier.getName());
      isList = true;
    }
    messageBuilder.append('.');
    return messageBuilder.toString();
  }
}
