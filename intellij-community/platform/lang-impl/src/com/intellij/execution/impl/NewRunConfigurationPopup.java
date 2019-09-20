// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.Consumer;
import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author Irina.Chernushina on 10/8/2015.
 */
public class NewRunConfigurationPopup {
  static final ConfigurationType HIDDEN_ITEMS_STUB = new ConfigurationType() {
    @NotNull
    @Override
    public String getDisplayName() {
      return "";
    }

    @Nls
    @Override
    public String getConfigurationTypeDescription() {
      return "";
    }

    @Override
    public Icon getIcon() {
      return EmptyIcon.ICON_16;
    }

    @NotNull
    @Override
    public String getId() {
      return "";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
      return new ConfigurationFactory[0];
    }
  };

  @NotNull
  public static ListPopup createAddPopup(@NotNull Project project,
                                         @NotNull final List<? extends ConfigurationType> typesToShow,
                                         @NotNull final String defaultText,
                                         @NotNull final Consumer<? super ConfigurationFactory> creator,
                                         @Nullable final ConfigurationType selectedConfigurationType,
                                         @Nullable final Runnable finalStep, boolean showTitle) {
    BaseListPopupStep<ConfigurationType> step = new BaseListPopupStep<ConfigurationType>(
      showTitle ? ExecutionBundle.message("add.new.run.configuration.action2.name") : null, typesToShow) {

      @Override
      @NotNull
      public String getTextFor(final ConfigurationType type) {
        return type != HIDDEN_ITEMS_STUB ? type.getDisplayName() : defaultText;
      }

      @Override
      public boolean isSpeedSearchEnabled() {
        return true;
      }

      @Override
      public Icon getIconFor(final ConfigurationType type) {
        return type.getIcon();
      }

      @Override
      public PopupStep onChosen(final ConfigurationType type, final boolean finalChoice) {
        if (hasSubstep(type)) {
          return getSupStep(type);
        }
        if (type == HIDDEN_ITEMS_STUB) {
          return doFinalStep(finalStep);
        }

        final ConfigurationFactory[] factories = type.getConfigurationFactories();
        if (factories.length > 0) {
          creator.consume(factories[0]);
        }
        return FINAL_CHOICE;
      }

      @Override
      public int getDefaultOptionIndex() {
        return selectedConfigurationType != HIDDEN_ITEMS_STUB ? typesToShow.indexOf(selectedConfigurationType) : super.getDefaultOptionIndex();
      }

      private ListPopupStep getSupStep(final ConfigurationType type) {
        final ConfigurationFactory[] factories = type.getConfigurationFactories();
        Arrays.sort(factories, (factory1, factory2) -> factory1.getName().compareToIgnoreCase(factory2.getName()));
        return new BaseListPopupStep<ConfigurationFactory>(
          ExecutionBundle.message("add.new.run.configuration.action.name", type.getDisplayName()), factories) {

          @Override
          @NotNull
          public String getTextFor(final ConfigurationFactory value) {
            return value.getName();
          }

          @Override
          public Icon getIconFor(final ConfigurationFactory factory) {
            return factory.getIcon();
          }

          @Override
          public PopupStep onChosen(final ConfigurationFactory factory, final boolean finalChoice) {
            creator.consume(factory);
            return FINAL_CHOICE;
          }
        };
      }

      @Override
      public boolean hasSubstep(final ConfigurationType type) {
        return type.getConfigurationFactories().length > 1;
      }
    };

    return new ListPopupImpl(project, step) {
      @Override
      protected void onSpeedSearchPatternChanged() {
        List<ConfigurationType> values = step.getValues();
        values.clear();
        values.addAll(RunConfigurable.Companion.configurationTypeSorted(project,
                                                                        false,
                                                                        ConfigurationType.CONFIGURATION_TYPE_EP.getExtensionList()));

        getListModel().updateOriginalList();
        super.onSpeedSearchPatternChanged();
      }
    };
  }
}
