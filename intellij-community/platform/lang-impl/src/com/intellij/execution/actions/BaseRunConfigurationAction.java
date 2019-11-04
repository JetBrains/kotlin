// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.LocatableConfiguration;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.lineMarker.RunLineMarkerProvider;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.macro.MacroManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class BaseRunConfigurationAction extends ActionGroup {
  protected static final Logger LOG = Logger.getInstance(BaseRunConfigurationAction.class);

  protected BaseRunConfigurationAction(final String text, final String description, final Icon icon) {
    super(text, description, icon);
    setPopup(true);
    setEnabledInModalContext(true);
  }

  @NotNull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return e != null ? getChildren(e.getDataContext()) : EMPTY_ARRAY;
  }

  private AnAction[] getChildren(DataContext dataContext) {
    final ConfigurationContext context = ConfigurationContext.getFromContext(dataContext);
    if (!Registry.is("suggest.all.run.configurations.from.context") && context.findExisting() != null) {
      return EMPTY_ARRAY;
    }
    return createChildActions(context, getConfigurationsFromContext(context)).toArray(EMPTY_ARRAY);
  }

  @NotNull
  protected List<AnAction> createChildActions(@NotNull ConfigurationContext context,
                                              @NotNull List<? extends ConfigurationFromContext> configurations) {
    if (configurations.size() <= 1) {
      return Collections.emptyList();
    }
    final List<AnAction> childActions = new ArrayList<>();
    for (final ConfigurationFromContext fromContext : configurations) {
      final ConfigurationType configurationType = fromContext.getConfigurationType();
      final String actionName = childActionName(fromContext);
      final AnAction anAction = new AnAction(actionName, configurationType.getDisplayName(), fromContext.getConfiguration().getIcon()) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          perform(fromContext, context);
        }
      };
      anAction.getTemplatePresentation().setText(actionName, false);
      childActions.add(anAction);
    }
    return childActions;
  }

  @NotNull
  private List<ConfigurationFromContext> getConfigurationsFromContext(ConfigurationContext context) {
    final List<ConfigurationFromContext> fromContext = context.getConfigurationsFromContext();
    if (fromContext == null) {
      return Collections.emptyList();
    }

    final List<ConfigurationFromContext> enabledConfigurations = new ArrayList<>();
    for (ConfigurationFromContext configurationFromContext : fromContext) {
      if (isEnabledFor(configurationFromContext.getConfiguration())) {
        enabledConfigurations.add(configurationFromContext);
      }
    }
    return enabledConfigurations;
  }

  protected boolean isEnabledFor(RunConfiguration configuration) {
    return true;
  }

  @Override
  public boolean canBePerformed(@NotNull DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project != null && DumbService.isDumb(project)) {
      return false;
    }

    final ConfigurationContext context = ConfigurationContext.getFromContext(dataContext);
    final RunnerAndConfigurationSettings existing = context.findExisting();
    if (existing == null) {
      final List<ConfigurationFromContext> fromContext = getConfigurationsFromContext(context);
      return fromContext.size() <= 1;
    }
    return true;
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    MacroManager.getInstance().cacheMacrosPreview(e.getDataContext());
    final ConfigurationContext context = ConfigurationContext.getFromContext(dataContext);
    final RunnerAndConfigurationSettings existing = context.findExisting();
    if (existing == null) {
      final List<ConfigurationFromContext> producers = getConfigurationsFromContext(context);
      if (producers.isEmpty()) return;
      if (producers.size() > 1) {
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        Collections.sort(producers, ConfigurationFromContext.NAME_COMPARATOR);
        final ListPopup popup =
          JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<ConfigurationFromContext>(ExecutionBundle.message("configuration.action.chooser.title"), producers) {
            @Override
            @NotNull
            public String getTextFor(final ConfigurationFromContext producer) {
              return childActionName(producer);
            }

            @Override
            public Icon getIconFor(final ConfigurationFromContext producer) {
              return producer.getConfigurationType().getIcon();
            }

            @Override
            public PopupStep<?> onChosen(ConfigurationFromContext producer, boolean finalChoice) {
              perform(producer, context);
              return FINAL_CHOICE;
            }
          });
        final InputEvent event = e.getInputEvent();
        if (event instanceof MouseEvent) {
          popup.show(new RelativePoint((MouseEvent)event));
        } else if (editor != null) {
          popup.showInBestPositionFor(editor);
        } else {
          popup.showInBestPositionFor(dataContext);
        }
      } else {
        perform(producers.get(0), context);
      }
      return;
    }

    if (LOG.isDebugEnabled()) {
      String configurationClass = existing.getConfiguration().getClass().getName();
      LOG.debug(String.format("Use existing run configuration: %s", configurationClass));
    }
    perform(context);
  }

  private void perform(final ConfigurationFromContext configurationFromContext, final ConfigurationContext context) {
    RunnerAndConfigurationSettings configurationSettings = configurationFromContext.getConfigurationSettings();
    context.setConfiguration(configurationSettings);
    configurationFromContext.onFirstRun(context, () -> {
      if (LOG.isDebugEnabled()) {
        RunnerAndConfigurationSettings settings = context.getConfiguration();
        RunConfiguration configuration = settings == null ? null : settings.getConfiguration();
        String configurationClass = configuration == null ? null : configuration.getClass().getName();
        LOG.debug(String.format("Create run configuration: %s", configurationClass));
      }
      perform(context);
    });
  }

  protected abstract void perform(ConfigurationContext context);

  @Override
  public void beforeActionPerformedUpdate(@NotNull AnActionEvent e) {
    fullUpdate(e);
  }

  @Nullable private static Integer ourLastTimeoutStamp = null;

  @Override
  public void update(@NotNull final AnActionEvent event) {
    VirtualFile vFile = event.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
    ThreeState hadAnythingRunnable = vFile == null ? ThreeState.UNSURE : RunLineMarkerProvider.hadAnythingRunnable(vFile);
    if (hadAnythingRunnable == ThreeState.UNSURE) {
      fullUpdate(event);
      return;
    }

    boolean success =
      !alreadyExceededTimeoutOnSimilarAction() &&
      ProgressIndicatorUtils.withTimeout(Registry.intValue("run.configuration.update.timeout"), () -> {
        fullUpdate(event);
        return true;
      }) != null;
    if (!success) {
      recordUpdateTimeout();
      approximatePresentationByPreviousAvailability(event, hadAnythingRunnable);
    }
  }

  private static boolean alreadyExceededTimeoutOnSimilarAction() {
    return Objects.equals(IdeEventQueue.getInstance().getEventCount(), ourLastTimeoutStamp);
  }

  private static void recordUpdateTimeout() {
    ourLastTimeoutStamp = IdeEventQueue.getInstance().getEventCount();
  }

  // we assume that presence of anything runnable in a file changes rarely, so using last recorded state is mostly OK 
  protected void approximatePresentationByPreviousAvailability(AnActionEvent event, ThreeState hadAnythingRunnable) {
    event.getPresentation().copyFrom(getTemplatePresentation());
    event.getPresentation().setEnabledAndVisible(hadAnythingRunnable == ThreeState.YES);
  }

  protected void fullUpdate(@NotNull AnActionEvent event) {
    final ConfigurationContext context = ConfigurationContext.getFromContext(event.getDataContext());
    final Presentation presentation = event.getPresentation();
    final RunnerAndConfigurationSettings existing = context.findExisting();
    RunnerAndConfigurationSettings configuration = existing;
    if (configuration == null) {
      configuration = context.getConfiguration();
    }
    if (configuration == null){
      presentation.setEnabledAndVisible(false);
    }
    else{
      presentation.setEnabledAndVisible(true);
      VirtualFile vFile = event.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
      if (vFile != null) {
        RunLineMarkerProvider.markRunnable(vFile);
      }
      final List<ConfigurationFromContext> fromContext = getConfigurationsFromContext(context);
      if (existing == null && !fromContext.isEmpty()) {
        //todo[nik,anna] it's dirty fix. Otherwise wrong configuration will be returned from context.getConfiguration()
        context.setConfiguration(fromContext.get(0).getConfigurationSettings());
      }
      final String name = suggestRunActionName((LocatableConfiguration)configuration.getConfiguration());
      updatePresentation(presentation, existing != null || fromContext.size() <= 1 ? name : "", context);
    }
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }

  @NotNull
  public static String suggestRunActionName(final LocatableConfiguration configuration) {
    if (configuration instanceof LocatableConfigurationBase && configuration.isGeneratedName()) {
      String actionName = ((LocatableConfigurationBase<?>)configuration).getActionName();
      if (actionName != null) {
        return actionName;
      }
    }
    return ProgramRunnerUtil.shortenName(configuration.getName(), 0);
  }

  @NotNull
  private static String childActionName(ConfigurationFromContext configurationFromContext) {
    RunConfiguration configuration = configurationFromContext.getConfiguration();
    if (!(configuration instanceof LocatableConfiguration)) {
      return configurationFromContext.getConfigurationType().getDisplayName();
    }
    if (configurationFromContext.isFromAlternativeLocation()) {
      String locationDisplayName = configurationFromContext.getAlternativeLocationDisplayName();
      if (locationDisplayName != null) {
        return ((LocatableConfigurationBase<?>)configuration).getActionName() + " " + locationDisplayName;
      }
    }

    return StringUtil.unquoteString(suggestRunActionName((LocatableConfiguration)configurationFromContext.getConfiguration()));
  }

  protected abstract void updatePresentation(Presentation presentation, @NotNull String actionText, ConfigurationContext context);

}
