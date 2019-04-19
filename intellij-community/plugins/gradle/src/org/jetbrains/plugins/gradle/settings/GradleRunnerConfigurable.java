// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleBundle;

import javax.swing.*;
import java.util.Objects;

/**
 * @author Vladislav.Soroka
 */
public class GradleRunnerConfigurable implements Configurable {
  private JPanel myMainPanel;
  private JBCheckBox myGradleAwareMakeCheckBox;
  private ComboBox myPreferredTestRunner;
  private static final TestRunnerItem[] TEST_RUNNER_ITEMS = new TestRunnerItem[]{
    new TestRunnerItem(TestRunner.PLATFORM),
    new TestRunnerItem(TestRunner.GRADLE),
    new TestRunnerItem(TestRunner.CHOOSE_PER_TEST)};
  private final DefaultGradleProjectSettings mySettings;

  public GradleRunnerConfigurable(@NotNull DefaultGradleProjectSettings settings) {
    mySettings = settings;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return GradleBundle.message("gradle.runner");
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.project.gradle";
  }

  @Override
  public void apply() throws ConfigurationException {
    boolean gradleMakeEnabled = myGradleAwareMakeCheckBox.isSelected();
    mySettings.setDelegatedBuild(gradleMakeEnabled);
    TestRunner preferredTestRunner = getSelectedRunner();
    mySettings.setTestRunner(preferredTestRunner);
  }

  @Override
  public void reset() {
    TestRunnerItem item = getItem(mySettings.getTestRunner());
    myPreferredTestRunner.setSelectedItem(item);
    boolean gradleMakeEnabled = mySettings.isDelegatedBuild();
    enableGradleMake(gradleMakeEnabled);
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myMainPanel;
  }

  @Override
  public boolean isModified() {
    return mySettings.isDelegatedBuild() != myGradleAwareMakeCheckBox.isSelected() ||
           mySettings.getTestRunner() != getSelectedRunner();
  }

  private void createUIComponents() {
    myGradleAwareMakeCheckBox = new JBCheckBox(GradleBundle.message("gradle.settings.text.use.gradle.aware.make"));
    myGradleAwareMakeCheckBox.addActionListener(e -> enableGradleMake(myGradleAwareMakeCheckBox.isSelected()));
    myPreferredTestRunner = new ComboBox<>(getItems());
  }

  private void enableGradleMake(boolean enable) {
    myGradleAwareMakeCheckBox.setSelected(enable);
  }

  private TestRunner getSelectedRunner() {
    final TestRunnerItem selectedItem = (TestRunnerItem)myPreferredTestRunner.getSelectedItem();
    return selectedItem == null ? TestRunner.CHOOSE_PER_TEST : selectedItem.value;
  }

  private static TestRunnerItem getItem(TestRunner preferredTestRunner) {
    for (TestRunnerItem item : getItems()) {
      if (item.value == preferredTestRunner) return item;
    }
    return null;
  }

  private static TestRunnerItem[] getItems() {
    return TEST_RUNNER_ITEMS;
  }

  static class TestRunnerItem {
    TestRunner value;

    TestRunnerItem(TestRunner value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof TestRunnerItem)) return false;
      TestRunnerItem item = (TestRunnerItem)o;
      return value == item.value;
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return GradleBundle.message("gradle.preferred_test_runner." + (value == null ? "CHOOSE_PER_TEST" : value.name()));
    }
  }
}
