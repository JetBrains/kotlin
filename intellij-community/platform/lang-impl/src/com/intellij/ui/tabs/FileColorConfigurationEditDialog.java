// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ui.tabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.FileColorManager;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
public class FileColorConfigurationEditDialog extends DialogWrapper {
  private FileColorConfiguration myConfiguration;
  private JComboBox myScopeComboBox;
  private final FileColorManager myManager;
  private final ColorSelectionComponent myColorSelectionComponent;

  private final Map<String, NamedScope> myScopeNames = new HashMap<>();

  public FileColorConfigurationEditDialog(@NotNull final FileColorManager manager, @Nullable final FileColorConfiguration configuration) {
    super(true);

    setTitle(configuration == null ? "Add Color Label" : "Edit Color Label");
    setResizable(false);

    myManager = manager;
    myConfiguration = configuration;
    myColorSelectionComponent = new ColorSelectionComponent();
    myColorSelectionComponent.initDefault(manager, configuration == null ? null : configuration.getColorName());
    myColorSelectionComponent.setChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        updateOKButton();
      }
    });

    init();
    updateCustomButton();
    if (myConfiguration != null && !StringUtil.isEmpty(myConfiguration.getScopeName())) {
      myScopeComboBox.setSelectedItem(myConfiguration.getScopeName());
    }
    updateOKButton();
  }

  public JComboBox getScopeComboBox() {
    return myScopeComboBox;
  }

  @Override
  protected JComponent createNorthPanel() {
    final List<NamedScope> scopeList = new ArrayList<>();
    final Project project = myManager.getProject();
    final NamedScopesHolder[] scopeHolders = NamedScopesHolder.getAllNamedScopeHolders(project);
    for (final NamedScopesHolder scopeHolder : scopeHolders) {
      final NamedScope[] scopes = scopeHolder.getScopes();
      Collections.addAll(scopeList, scopes);
    }
    CustomScopesProviderEx.filterNoSettingsScopes(project, scopeList);
    for (final NamedScope scope : scopeList) {
      myScopeNames.put(scope.getName(), scope);
    }

    myScopeComboBox = new ComboBox<>(ArrayUtilRt.toStringArray(myScopeNames.keySet()));
    myScopeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateCustomButton();
        updateOKButton();
      }
    });
    new ComboboxSpeedSearch(myScopeComboBox);

    final JLabel pathLabel = new JLabel("Scope:");
    pathLabel.setDisplayedMnemonic('S');
    pathLabel.setLabelFor(myScopeComboBox);
    final JLabel colorLabel = new JLabel("Color:");

    JPanel result = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = JBUI.insets(5);
    gbc.gridx = 0;
    result.add(pathLabel, gbc);
    result.add(colorLabel, gbc);
    gbc.gridx = 1;
    gbc.weightx = 1;
    result.add(myScopeComboBox, gbc);
    result.add(myColorSelectionComponent, gbc);
    return result;
  }

  private void updateCustomButton() {
    Object item = myScopeComboBox.getSelectedItem();
    if (item instanceof String) {
      Color color = myConfiguration == null ? null : ColorUtil.fromHex(myConfiguration.getColorName(), null);
      NamedScope scope = myScopeNames.get(item);
      String colorName = scope.getDefaultColorName();

      if (color == null && StringUtil.isNotEmpty(colorName)) {
        color = myManager.getColor(colorName);
      }

      if (color != null) {
        if (StringUtil.isNotEmpty(colorName) && color.equals(myManager.getColor(colorName))) {
          myColorSelectionComponent.setSelectedColor(colorName);
        }
        else {
          myColorSelectionComponent.setCustomButtonColor(color);
        }
      }
    }
  }

  @Override
  protected void doOKAction() {
    close(OK_EXIT_CODE);

    if (myConfiguration != null) {
      myConfiguration.setScopeName((String) myScopeComboBox.getSelectedItem());
      myConfiguration.setColorName(getColorName());
    } else {
      myConfiguration = new FileColorConfiguration((String) myScopeComboBox.getSelectedItem(), getColorName());
    }
  }

  public FileColorConfiguration getConfiguration() {
    return myConfiguration;
  }

  @Nullable
  private String getColorName() {
    return myColorSelectionComponent.getSelectedColorName();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myScopeComboBox.isEnabled() ? myScopeComboBox : myColorSelectionComponent;
  }

  private void updateOKButton() {
    getOKAction().setEnabled(isOKActionEnabled());
  }

  @Override
  public boolean isOKActionEnabled() {
    final String scopeName = (String) myScopeComboBox.getSelectedItem();
    return scopeName != null && scopeName.length() > 0 && getColorName() != null;
  }

  @Override
  protected JComponent createCenterPanel() {
    return null;
  }
}
