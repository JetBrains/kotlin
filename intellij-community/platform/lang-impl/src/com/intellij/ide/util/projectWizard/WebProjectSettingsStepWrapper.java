// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.projectWizard;

import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class WebProjectSettingsStepWrapper implements SettingsStep {
  private final List<Pair<String, JComponent>> myFields = new ArrayList<>();
  private final List<JComponent> myComponents = new ArrayList<>();

  public List<JComponent> getComponents() {
    return myComponents;
  }

  @Override
  @Nullable
  public WizardContext getContext() {
    return null;
  }

  public List<LabeledComponent<? extends JComponent>> getFields() {
    return ContainerUtil.map(myFields, pair -> LabeledComponent.create(pair.second, pair.first));
  }

  @Override
  public void addSettingsField(@NotNull String label, @NotNull JComponent field) {
    myFields.add(Pair.create(label, field));
  }

  @Override
  public void addSettingsComponent(@NotNull JComponent component) {
    myComponents.add(component);
  }

  @Override
  public void addExpertPanel(@NotNull JComponent panel) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addExpertField(@NotNull String label, @NotNull JComponent field) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public JTextField getModuleNameField() {
    return null;
  }

  public boolean isEmpty() {
    return myFields.isEmpty() && myComponents.isEmpty();
  }
}