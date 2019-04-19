/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.util.projectWizard;

import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class WebProjectSettingsStepWrapper implements SettingsStep {
  private static final Function<Pair<String, JComponent>, LabeledComponent> PAIR_LABELED_COMPONENT_FUNCTION =
    pair -> LabeledComponent.create(pair.getSecond(), pair.getFirst());

  private final List<Pair<String, JComponent>> myFields = ContainerUtil.newArrayList();
  private final List<JComponent> myComponents = ContainerUtil.newArrayList();

  public List<JComponent> getComponents() {
    return myComponents;
  }

  @Override
  @Nullable
  public WizardContext getContext() {
    return null;
  }

  public List<LabeledComponent> getFields() {
    return ContainerUtil.map(myFields, PAIR_LABELED_COMPONENT_FUNCTION);
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
