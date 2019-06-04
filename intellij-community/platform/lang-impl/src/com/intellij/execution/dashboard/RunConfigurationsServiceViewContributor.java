// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.StopAction;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.tree.ConfigurationTypeDashboardGroupingRule;
import com.intellij.execution.dashboard.tree.RunConfigurationNode;
import com.intellij.execution.dashboard.tree.RunDashboardGroupImpl;
import com.intellij.execution.runners.FakeRerunAction;
import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.execution.services.ServiceViewGroupingContributor;
import com.intellij.execution.services.ServiceViewProvidingContributor;
import com.intellij.execution.services.SimpleServiceViewDescriptor;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.layout.impl.RunnerLayoutUiImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intellij.execution.dashboard.RunDashboardContent.RUN_DASHBOARD_CONTENT_TOOLBAR;
import static com.intellij.execution.dashboard.RunDashboardContent.RUN_DASHBOARD_TREE_TOOLBAR;
import static com.intellij.execution.dashboard.RunDashboardCustomizer.NODE_LINKS;
import static com.intellij.execution.dashboard.RunDashboardManagerImpl.getRunnerLayoutUi;
import static com.intellij.openapi.actionSystem.ActionPlaces.RUN_DASHBOARD_POPUP;

public class RunConfigurationsServiceViewContributor
  implements ServiceViewGroupingContributor<RunConfigurationsServiceViewContributor.RunConfigurationContributor, RunDashboardGroup> {
  private static final ServiceViewDescriptor CONTRIBUTOR_DESCRIPTOR =
    new SimpleServiceViewDescriptor("Run Dashboard", AllIcons.Actions.Execute) {
      @Override
      public ActionGroup getToolbarActions() {
        return RunConfigurationsServiceViewContributor.getToolbarActions(null);
      }

      @Override
      public ActionGroup getPopupActions() {
        return RunConfigurationsServiceViewContributor.getPopupActions();
      }
    };
  private static final RunDashboardGroupingRule TYPE_GROUPING_RULE = new ConfigurationTypeDashboardGroupingRule();

  @NotNull
  @Override
  public ServiceViewDescriptor getViewDescriptor() {
    return CONTRIBUTOR_DESCRIPTOR;
  }

  @NotNull
  @Override
  public List<RunConfigurationContributor> getServices(@NotNull Project project) {
    RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(project);
    return ContainerUtil.map(runDashboardManager.getRunConfigurations(),
                             value -> new RunConfigurationContributor(
                               new RunConfigurationNode(project, value,
                                                        runDashboardManager.getCustomizers(value.getSettings(), value.getDescriptor()))));
  }

  @NotNull
  @Override
  public ServiceViewDescriptor getServiceDescriptor(@NotNull RunConfigurationContributor contributor) {
    return contributor.getViewDescriptor();
  }

  @Nullable
  @Override
  public RunDashboardGroup groupBy(@NotNull RunConfigurationContributor contributor) {
    return TYPE_GROUPING_RULE.getGroup(contributor.asService());
  }

  @NotNull
  @Override
  public ServiceViewDescriptor getGroupDescriptor(@NotNull RunDashboardGroup group) {
    PresentationData presentationData = new PresentationData();
    presentationData.setPresentableText(group.getName());
    presentationData.setIcon(group.getIcon());
    return new ServiceViewDescriptor() {
      @Nullable
      @Override
      public String getId() {
        if (group instanceof RunDashboardGroupImpl) {
          Object value = ((RunDashboardGroupImpl)group).getValue();
          if (value instanceof ConfigurationType) {
            return ((ConfigurationType)value).getId();
          }
        }
        return group.getName();
      }

      @Override
      public JComponent getContentComponent() {
        return null;
      }

      @Override
      public ActionGroup getToolbarActions() {
        return RunConfigurationsServiceViewContributor.getToolbarActions(null);
      }

      @Override
      public ActionGroup getPopupActions() {
        return RunConfigurationsServiceViewContributor.getPopupActions();
      }

      @NotNull
      @Override
      public ItemPresentation getPresentation() {
        return presentationData;
      }

      @Override
      public DataProvider getDataProvider() {
        return null;
      }
    };
  }

  @NotNull
  private static JComponent createEmptyContent() {
    return new JBPanelWithEmptyText().withEmptyText(ExecutionBundle.message("run.dashboard.not.started.configuration.message"));
  }

  private static ActionGroup getToolbarActions(@Nullable RunContentDescriptor descriptor) {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(ActionManager.getInstance().getAction(RUN_DASHBOARD_CONTENT_TOOLBAR));

    RunnerLayoutUiImpl ui = getRunnerLayoutUi(descriptor);
    if (ui == null) return actionGroup;

    List<AnAction> leftToolbarActions = ui.getActions();
    for (AnAction action : leftToolbarActions) {
      if (!(action instanceof StopAction) && !(action instanceof FakeRerunAction)) {
        actionGroup.add(action);
      }
    }
    return actionGroup;
  }

  private static ActionGroup getPopupActions() {
    DefaultActionGroup actions = new DefaultActionGroup();
    ActionManager actionManager = ActionManager.getInstance();
    actions.add(actionManager.getAction(RUN_DASHBOARD_CONTENT_TOOLBAR));
    actions.addSeparator();
    actions.add(actionManager.getAction(RUN_DASHBOARD_TREE_TOOLBAR));
    actions.add(actionManager.getAction(RUN_DASHBOARD_POPUP));
    return actions;
  }

  private static class RunConfigurationServiceViewDescriptor implements ServiceViewDescriptor {
    private static final Key<Boolean> SELECTION_QUERIED = Key.create("ServiceViewContentSelectionQueried");

    private final RunConfigurationNode node;

    RunConfigurationServiceViewDescriptor(RunConfigurationNode node) {
      this.node = node;
    }

    @Nullable
    @Override
    public String getId() {
      RunConfiguration configuration = node.getConfigurationSettings().getConfiguration();
      return configuration.getType().getId() + "/" + configuration.getName();
    }

    @Override
    public JComponent getContentComponent() {
      Content content = node.getContent();
      if (content == null) return createEmptyContent();

      ContentManager contentManager = content.getManager();
      return contentManager == null ? null : contentManager.getComponent();
    }

    @NotNull
    @Override
    public ItemPresentation getContentPresentation() {
      Content content = node.getContent();
      if (content != null) {
        return new PresentationData(content.getDisplayName(), null, content.getIcon(), null);
      }
      else {
        RunConfiguration configuration = node.getConfigurationSettings().getConfiguration();
        return new PresentationData(configuration.getName(), null, configuration.getIcon(), null);
      }
    }

    @Override
    public ActionGroup getToolbarActions() {
      return RunConfigurationsServiceViewContributor.getToolbarActions(node.getDescriptor());
    }

    @Override
    public ActionGroup getPopupActions() {
      return RunConfigurationsServiceViewContributor.getPopupActions();
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
      return node.getPresentation();
    }

    @Override
    public DataProvider getDataProvider() {
      Content content = node.getContent();
      if (content == null) return null;

      DataContext context = DataManager.getInstance().getDataContext(content.getComponent());
      return context::getData;
    }

    @Override
    public void onNodeSelected() {
      Content content = node.getContent();
      if (content == null) return;

      content.putUserData(SELECTION_QUERIED, Boolean.TRUE);
      ContentManager contentManager = content.getManager();
      if (contentManager == null || content == contentManager.getSelectedContent()) return;

      // Invoke content selection change later after currently selected content lost a focus.
      SwingUtilities.invokeLater(() -> {
        // Selected node may changed, we do not need to select content if it doesn't correspond currently selected node.
        if (contentManager.isDisposed() ||
            contentManager.getIndexOfContent(content) == -1 ||
            Boolean.TRUE != content.getUserData(SELECTION_QUERIED)) {
          return;
        }

        contentManager.setSelectedContent(content);
      });
    }

    @Override
    public void onNodeUnselected() {
      Content content = node.getContent();
      if (content == null) return;

      content.putUserData(SELECTION_QUERIED, Boolean.FALSE);
      ContentManager contentManager = content.getManager();
      if (contentManager == null || content != contentManager.getSelectedContent()) return;

      // Invoke content selection change later after currently selected content correctly restores its state,
      // since RunnerContentUi performs restoring later after addNotify call chain.
      SwingUtilities.invokeLater(() -> {
        // Selected node may changed, we do not need to remove content from selection if it corresponds currently selected node.
        if (contentManager.isDisposed() ||
            !contentManager.isSelected(content) ||
            Boolean.TRUE == content.getUserData(SELECTION_QUERIED)) {
          return;
        }

        contentManager.removeFromSelection(content);
      });
    }

    @Nullable
    @Override
    public Navigatable getNavigatable() {
      for (RunDashboardCustomizer customizer : node.getCustomizers()) {
        Navigatable navigatable = customizer.getNavigatable(node);
        if (navigatable != null) {
          return navigatable;
        }
      }
      return null;
    }

    @Nullable
    @Override
    public Object getPresentationTag(Object fragment) {
      Map<Object, Object> links = node.getUserData(NODE_LINKS);
      return links == null ? null : links.get(fragment);
    }

    @Nullable
    @Override
    public Runnable getRemover() {
      RunnerAndConfigurationSettings settings = node.getConfigurationSettings();
      RunManager runManager = RunManager.getInstance(settings.getConfiguration().getProject());
      return runManager.hasSettings(settings) ? () -> runManager.removeConfiguration(settings) : null;
    }
  }

  static class RunConfigurationContributor implements ServiceViewProvidingContributor<AbstractTreeNode, RunConfigurationNode> {
    private final RunConfigurationNode myNode;

    RunConfigurationContributor(@NotNull RunConfigurationNode node) {
      myNode = node;
    }

    @NotNull
    @Override
    public RunConfigurationNode asService() {
      return myNode;
    }

    @NotNull
    @Override
    public ServiceViewDescriptor getViewDescriptor() {
      return new RunConfigurationServiceViewDescriptor(myNode);
    }

    @NotNull
    @Override
    public List<AbstractTreeNode> getServices(@NotNull Project project) {
      return new ArrayList<>(myNode.getChildren());
    }

    @NotNull
    @Override
    public ServiceViewDescriptor getServiceDescriptor(@NotNull AbstractTreeNode service) {
      return new ServiceViewDescriptor() {
        @Override
        public ActionGroup getToolbarActions() {
          return RunConfigurationsServiceViewContributor.getToolbarActions(null);
        }

        @Override
        public ActionGroup getPopupActions() {
          return RunConfigurationsServiceViewContributor.getPopupActions();
        }

        @NotNull
        @Override
        public ItemPresentation getPresentation() {
          return service.getPresentation();
        }

        @Nullable
        @Override
        public String getId() {
          ItemPresentation presentation = getPresentation();
          String text = presentation.getPresentableText();
          if (!StringUtil.isEmpty(text)) {
            return text;
          }
          if (presentation instanceof PresentationData) {
            List<PresentableNodeDescriptor.ColoredFragment> fragments = ((PresentationData)presentation).getColoredText();
            if (!fragments.isEmpty()) {
              StringBuilder result = new StringBuilder();
              for (PresentableNodeDescriptor.ColoredFragment fragment : fragments) {
                result.append(fragment.getText());
              }
              return result.toString();
            }
          }
          return null;
        }

        @Nullable
        @Override
        public Runnable getRemover() {
          return service instanceof RunDashboardNode ? ((RunDashboardNode)service).getRemover() : null;
        }
      };
    }
  }
}
