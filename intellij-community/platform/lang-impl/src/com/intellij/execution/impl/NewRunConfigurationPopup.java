// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.ui.popup.util.BaseTreePopupStep;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.ActiveComponent;
import com.intellij.ui.UIBundle;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.ui.popup.tree.TreePopupImpl;
import com.intellij.util.Consumer;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
      return ConfigurationFactory.EMPTY_ARRAY;
    }
  };

  @NotNull
  public static JBPopup createAddPopup(@NotNull Project project,
                                       @NotNull final List<? extends ConfigurationType> typesToShow,
                                       @NotNull final String defaultText,
                                       @NotNull final Consumer<? super ConfigurationFactory> creator,
                                       @Nullable final ConfigurationType selectedConfigurationType,
                                       @Nullable final Runnable finalStep, boolean showTitle) {
    if (Registry.is("run.configuration.use.tree.popup.to.add.new", false)) {
      return createAddTreePopup(project, creator, selectedConfigurationType, showTitle);
    }

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
      public PopupStep<?> onChosen(final ConfigurationType type, final boolean finalChoice) {
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
        return selectedConfigurationType != HIDDEN_ITEMS_STUB
               ? typesToShow.indexOf(selectedConfigurationType)
               : super.getDefaultOptionIndex();
      }

      private ListPopupStep<?> getSupStep(final ConfigurationType type) {
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
          public PopupStep<?> onChosen(final ConfigurationFactory factory, final boolean finalChoice) {
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

  private static JBPopup createAddTreePopup(@NotNull Project project,
                                            @NotNull final Consumer<? super ConfigurationFactory> creator,
                                            @Nullable final ConfigurationType selectedConfigurationType,
                                            boolean showTitle) {
    NewRunConfigurationTreePopupFactory treePopupFactory = ApplicationManager.getApplication().getService(NewRunConfigurationTreePopupFactory.class);
    treePopupFactory.initStructure(project);

    AbstractTreeStructure structure = new AbstractTreeStructure() {
      private final Map<NodeDescriptor<?>, NodeDescriptor<?>[]> myCache = new HashMap<>();

      @NotNull
      @Override
      public Object getRootElement() {
        return treePopupFactory.getRootElement();
      }

      @Override
      public NodeDescriptor<?> @NotNull [] getChildElements(@NotNull Object element) {
        NodeDescriptor<?> nodeDescriptor = (NodeDescriptor<?>)element;
        if (!myCache.containsKey(nodeDescriptor)) {
          myCache.put(nodeDescriptor, treePopupFactory.createChildElements(project, nodeDescriptor));
        }
        return myCache.get(nodeDescriptor);
      }

      @Nullable
      @Override
      public Object getParentElement(@NotNull Object element) {
        return ((NodeDescriptor<?>)element).getParentDescriptor();
      }

      @NotNull
      @Override
      public NodeDescriptor<?> createDescriptor(@NotNull Object element, @Nullable NodeDescriptor parentDescriptor) {
        return treePopupFactory.createDescriptor(project, element, parentDescriptor, NodeDescriptor.DEFAULT_WEIGHT);
      }

      @Override
      public void commit() {
      }

      @Override
      public boolean hasSomethingToCommit() {
        return false;
      }
    };

    final AtomicBoolean isAutoSelectionPassed = new AtomicBoolean(selectedConfigurationType == null);

    BaseTreePopupStep<Object> treePopupStep = new BaseTreePopupStep<Object>(
      project,
      showTitle ? ExecutionBundle.message("add.new.run.configuration.action2.name") : null, structure
    ) {
      @Override
      public boolean isRootVisible() {
        return false;
      }

      @Override
      public boolean isSelectable(Object node, Object userData) {
        if (!(userData instanceof NodeDescriptor)) return false;
        if (getStructure().getChildElements(userData).length > 0) return false;
        userData = ((NodeDescriptor<?>)userData).getElement();
        return isAutoSelectionPassed.get() || userData == selectedConfigurationType;
      }

      @Override
      public PopupStep<?> onChosen(Object selectedValue, boolean finalChoice) {
        Object element = ((NodeDescriptor<?>)selectedValue).getElement();
        if (element instanceof ConfigurationType) {
          ConfigurationFactory[] factories = ((ConfigurationType)element).getConfigurationFactories();
          if (factories.length == 1) {
            creator.consume(factories[0]);
            return FINAL_CHOICE;
          }
        }
        if (element instanceof ConfigurationFactory) {
          creator.consume((ConfigurationFactory)element);
          return FINAL_CHOICE;
        }
        return super.onChosen(selectedValue, finalChoice);
      }
    };
    TreePopupImpl treePopup = new TreePopupImpl(project, null, treePopupStep, null) {
      @Override
      protected void afterShow() {
        super.afterShow();
        isAutoSelectionPassed.set(true);
        scrollToSelection();
      }

      @Override
      public boolean shouldBeShowing(Object value) {
        NodeDescriptor<?> parent = (value instanceof NodeDescriptor) ? ((NodeDescriptor)value).getParentDescriptor() : null;
        return super.shouldBeShowing(value) || (parent != null && super.shouldBeShowing(parent));
      }
    };
    DumbAwareAction collapseAllAction =
      new DumbAwareAction(UIBundle.message("tree.view.collapse.all.action.name"), null, AllIcons.Actions.Collapseall) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          treePopup.collapseAll();
        }
      };
    ActionToolbarImpl toolbar = (ActionToolbarImpl)ActionManager.getInstance()
      .createActionToolbar(ActionPlaces.POPUP, new DefaultActionGroup(collapseAllAction), true);
    toolbar.setMiniMode(true);
    treePopup.getTitle().setButtonComponent(new ActiveComponent() {
      @Override
      public void setActive(boolean active) {
      }

      @NotNull
      @Override
      public JComponent getComponent() {
        return toolbar.getComponent();
      }
    }, JBUI.Borders.empty(2, 1, 0, 1));
    return treePopup;
  }
}
