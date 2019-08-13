// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.compound;

import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;

public class ConfigurationSelectionUtil {
  @NotNull
  public static String getDisplayText(@NotNull RunConfiguration configuration, @Nullable ExecutionTarget target) {
    return configuration.getType().getDisplayName() + " '" + configuration.getName() +
           "'" + (target != null && target != DefaultExecutionTarget.INSTANCE ? " | " + target.getDisplayName() : "");
  }

  // todo merge with ChooseRunConfigurationPopup
  public static ListPopup createPopup(@NotNull Project project,
                                      @NotNull RunManagerImpl runManager,
                                      @NotNull List<? extends RunConfiguration> configurations,
                                      @NotNull BiConsumer<? super List<RunConfiguration>, ? super ExecutionTarget> onSelected) {
    return JBPopupFactory.getInstance().createListPopup(new MultiSelectionListPopupStep<RunConfiguration>(null, configurations) {
      @Nullable
      @Override
      public ListSeparator getSeparatorAbove(RunConfiguration value) {
        int i = configurations.indexOf(value);
        if (i < 1) return null;
        RunConfiguration previous = configurations.get(i - 1);
        return value.getType() != previous.getType() ? new ListSeparator() : null;
      }

      @Override
      public Icon getIconFor(RunConfiguration value) {
        return value.getType().getIcon();
      }

      @Override
      public boolean isSpeedSearchEnabled() {
        return true;
      }

      @NotNull
      @Override
      public String getTextFor(RunConfiguration value) {
        return value.getName();
      }

      @Override
      public PopupStep<?> onChosen(List<RunConfiguration> selectedConfigs, boolean finalChoice) {
        if (finalChoice) {
          onSelected.accept(selectedConfigs, null);
          return FINAL_CHOICE;
        }
        else {
          return new BaseListPopupStep<ExecutionTarget>(null, getTargets(selectedConfigs)) {
            @Override
            public boolean isSpeedSearchEnabled() {
              return true;
            }

            @Override
            public Icon getIconFor(ExecutionTarget value) {
              return value.getIcon();
            }

            @NotNull
            @Override
            public String getTextFor(ExecutionTarget value) {
              return value.getDisplayName();
            }

            @Override
            public PopupStep onChosen(ExecutionTarget selectedTarget, boolean finalChoice) {
              onSelected.accept(selectedConfigs, selectedTarget);
              return FINAL_CHOICE;
            }
          };
        }
      }

      @Override
      public boolean hasSubstep(List<? extends RunConfiguration> selectedValues) {
        return !getTargets(selectedValues).isEmpty();
      }

      @NotNull
      public List<ExecutionTarget> getTargets(List<? extends RunConfiguration> selectedValues) {
        LinkedHashSet<ExecutionTarget> intersection = new LinkedHashSet<>();
        for (int i = 0; i < selectedValues.size(); i++) {
          RunConfiguration config = selectedValues.get(i);
          RunnerAndConfigurationSettingsImpl settings = runManager.getSettings(config);
          List<ExecutionTarget> targets = settings == null ? Collections.emptyList() : ExecutionTargetManager.getTargetsToChooseFor(project, settings.getConfiguration());
          if (i == 0) {
            intersection.addAll(targets);
          }
          else {
            intersection.retainAll(targets);
          }
        }
        return new ArrayList<>(intersection);
      }
    });
  }
}
