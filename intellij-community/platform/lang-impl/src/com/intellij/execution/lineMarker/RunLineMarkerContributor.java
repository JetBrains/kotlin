// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.lineMarker;

import com.intellij.execution.TestStateStorage;
import com.intellij.execution.testframework.TestIconMapper;
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.TextWithMnemonic;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

public abstract class RunLineMarkerContributor {
  public static final Function<PsiElement, String> RUN_TEST_TOOLTIP_PROVIDER = it -> "Run Test";

  static final LanguageExtension<RunLineMarkerContributor> EXTENSION = new LanguageExtension<>("com.intellij.runLineMarkerContributor");

  @NotNull
  public static Info withExecutorActions(@NotNull Icon icon) {
    return new Info(icon, ExecutorAction.getActions(1), RUN_TEST_TOOLTIP_PROVIDER);
  }

  public static class Info {
    public final Icon icon;
    public final AnAction[] actions;

    public final Function<PsiElement, String> tooltipProvider;

    public Info(Icon icon, @NotNull AnAction[] actions, @Nullable Function<PsiElement, String> tooltipProvider) {
      this.icon = icon;
      this.actions = actions;
      this.tooltipProvider = tooltipProvider;
    }

    public Info(Icon icon, @Nullable com.intellij.util.Function<PsiElement, String> tooltipProvider, @NotNull AnAction... actions) {
      this.icon = icon;
      this.actions = actions;
      this.tooltipProvider = tooltipProvider == null ? null : it -> tooltipProvider.fun(it);
    }

    public Info(@NotNull final AnAction action) {
      this(action.getTemplatePresentation().getIcon(), new AnAction[]{action}, element -> getText(action, element));
    }

    /**
     * Checks if this Info should replace another one, that is if the other should be discarded.
     */
    public boolean shouldReplace(@NotNull Info other) {
      return false;
    }
  }

  @Nullable
  public abstract Info getInfo(@NotNull PsiElement element);

  /**
   * @param file any file with a language this contributor is registered for
   * @return whether there's no possibility that a {@link com.intellij.execution.actions.RunConfigurationProducer}'would
   * return a configuration not returned by this contributor in this file. Used to speed up "Run..." context action update.
   */
  public boolean producesAllPossibleConfigurations(@NotNull PsiFile file) {
    return true;
  }

  @Nullable("null means disabled")
  protected static String getText(@NotNull AnAction action, @NotNull PsiElement element) {
    DataContext parent = DataManager.getInstance().getDataContext();
    DataContext dataContext = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT.getName(), element, parent);
    if (!(action instanceof ExecutorAction)) {
      return null;
    }
//    return ((ExecutorAction)action).getActionName(dataContext);
    AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext);
    action.update(event);
    if (!event.getPresentation().isEnabledAndVisible()) {
      return null;
    }
    String name = ((ExecutorAction)action).getActionName(dataContext);
    return name == null ? null : TextWithMnemonic.parse(name).getText();
  }

  @NotNull
  protected static Icon getTestStateIcon(String url, @NotNull Project project, boolean isClass) {
    return getTestStateIcon(TestStateStorage.getInstance(project).getState(url), isClass);
  }

  @NotNull
  protected static Icon getTestStateIcon(@Nullable TestStateStorage.Record state, boolean isClass) {
    if (state != null) {
      TestStateInfo.Magnitude magnitude = TestIconMapper.getMagnitude(state.magnitude);
      if (magnitude != null) {
        switch (magnitude) {
          case ERROR_INDEX:
          case FAILED_INDEX:
            return AllIcons.RunConfigurations.TestState.Red2;
          case PASSED_INDEX:
          case COMPLETE_INDEX:
            return AllIcons.RunConfigurations.TestState.Green2;
          default:
        }
      }
    }
    return isClass ? AllIcons.RunConfigurations.TestState.Run_run : AllIcons.RunConfigurations.TestState.Run;
  }
}
