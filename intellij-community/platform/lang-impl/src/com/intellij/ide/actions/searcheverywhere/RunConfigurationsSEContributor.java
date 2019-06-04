// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ChooseRunConfigurationPopup;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class RunConfigurationsSEContributor implements SearchEverywhereContributor<ChooseRunConfigurationPopup.ItemWrapper> {

  private final SearchEverywhereCommandInfo RUN_COMMAND =
    new SearchEverywhereCommandInfo("run", IdeBundle.message("searcheverywhere.runconfigurations.command.run.description"), this);
  private final SearchEverywhereCommandInfo DEBUG_COMMAND =
    new SearchEverywhereCommandInfo("debug", IdeBundle.message("searcheverywhere.runconfigurations.command.debug.description"), this);

  private final static int RUN_MODE = 0;
  private final static int DEBUG_MODE = 1;

  private final Project myProject;
  private final Component myContextComponent;
  private final Supplier<String> myCommandSupplier;

  public RunConfigurationsSEContributor(Project project, Component component, Supplier<String> commandSupplier) {
    myProject = project;
    myContextComponent = component;
    myCommandSupplier = commandSupplier;
  }

  @NotNull
  @Override
  public String getSearchProviderId() {
    return getClass().getSimpleName();
  }

  @NotNull
  @Override
  public String getGroupName() {
    return IdeBundle.message("searcheverywhere.run.configs.tab.name");
  }

  @Override
  public int getSortWeight() {
    return 350;
  }

  @Override
  public boolean showInFindResults() {
    return false;
  }

  @Override
  public boolean processSelectedItem(@NotNull ChooseRunConfigurationPopup.ItemWrapper selected, int modifiers, @NotNull String searchText) {
    RunnerAndConfigurationSettings settings = ObjectUtils.tryCast(selected.getValue(), RunnerAndConfigurationSettings.class);
    if (settings != null) {
      int mode = getMode(searchText, modifiers);
      Executor executor = findExecutor(settings, mode);
      if (executor != null) {
        DataManager dataManager = DataManager.getInstance();
        selected.perform(myProject, executor, dataManager.getDataContext(myContextComponent));
      }
    }

    return true;
  }

  @Nullable
  @Override
  public Object getDataForItem(@NotNull ChooseRunConfigurationPopup.ItemWrapper element, @NotNull String dataId) {
    return null;
  }

  @NotNull
  @Override
  public ListCellRenderer<? super ChooseRunConfigurationPopup.ItemWrapper> getElementsRenderer() {
    return renderer;
  }

  @NotNull
  @Override
  public List<SearchEverywhereCommandInfo> getSupportedCommands() {
    return Arrays.asList(RUN_COMMAND, DEBUG_COMMAND);
  }

  @Override
  public void fetchElements(@NotNull String pattern,
                            @NotNull ProgressIndicator progressIndicator,
                            @NotNull Processor<? super ChooseRunConfigurationPopup.ItemWrapper> consumer) {

    if (StringUtil.isEmptyOrSpaces(pattern)) return;

    pattern = filterString(pattern);
    MinusculeMatcher matcher = NameUtil.buildMatcher(pattern).build();
    for (ChooseRunConfigurationPopup.ItemWrapper wrapper : ChooseRunConfigurationPopup.createFlatSettingsList(myProject)) {
      if (matcher.matches(wrapper.getText()) && !consumer.process(wrapper)) {
        return;
      }
    }
  }

  @MagicConstant(intValues = {RUN_MODE, DEBUG_MODE})
  private int getMode(String searchText, int modifiers) {
    if (isCommand(searchText, DEBUG_COMMAND)) {
      return DEBUG_MODE;
    }
    else if (isCommand(searchText, RUN_COMMAND)) {
      return RUN_MODE;
    }
    else {
      return (modifiers & InputEvent.SHIFT_MASK) == 0 ? DEBUG_MODE : RUN_MODE;
    }
  }

  private static Optional<String> extractFirstWord(String input) {
    if (!StringUtil.isEmptyOrSpaces(input) && input.contains(" ")) {
      return Optional.of(input.split(" ")[0]);
    }

    return Optional.empty();
  }

  private String filterString(String input) {
    return extractFirstWord(input)
      .filter(firstWord -> RUN_COMMAND.getCommandWithPrefix().startsWith(firstWord) ||
                           DEBUG_COMMAND.getCommandWithPrefix().startsWith(firstWord))
      .map(firstWord -> input.substring(firstWord.length() + 1))
      .orElse(input);
  }

  private static boolean isCommand(String input, SearchEverywhereCommandInfo command) {
    if (input == null) {
      return false;
    }

    return extractFirstWord(input)
      .map(firstWord -> command.getCommandWithPrefix().startsWith(firstWord))
      .orElse(false);
  }

  private final Renderer renderer = new Renderer();

  private class Renderer extends JPanel implements ListCellRenderer<ChooseRunConfigurationPopup.ItemWrapper> {

    private final SimpleColoredComponent runConfigInfo = new SimpleColoredComponent();
    private final SimpleColoredComponent executorInfo = new SimpleColoredComponent();

    private Renderer() {
      super(new BorderLayout());
      add(runConfigInfo, BorderLayout.CENTER);
      add(executorInfo, BorderLayout.EAST);
      setBorder(JBUI.Borders.empty(1, UIUtil.isUnderWin10LookAndFeel() ? 0 : JBUIScale.scale(UIUtil.getListCellHPadding())));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ChooseRunConfigurationPopup.ItemWrapper> list,
                                                  ChooseRunConfigurationPopup.ItemWrapper wrapper,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      runConfigInfo.clear();
      executorInfo.clear();

      setBackground(UIUtil.getListBackground(isSelected, true));
      setFont(list.getFont());
      Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();
      runConfigInfo.append(wrapper.getText(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, foreground));
      runConfigInfo.setIcon(wrapper.getIcon());

      fillExecutorInfo(wrapper, list, isSelected);

      return this;
    }

    private void fillExecutorInfo(ChooseRunConfigurationPopup.ItemWrapper wrapper, JList<?> list, boolean selected) {

      SimpleTextAttributes commandAttributes = selected
                                               ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.getSelectionForeground())
                                               : SimpleTextAttributes.GRAYED_ATTRIBUTES;
      SimpleTextAttributes shortcutAttributes = selected
                                                ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.getSelectionForeground())
                                                : SimpleTextAttributes.GRAY_ATTRIBUTES;

      String input = myCommandSupplier.get();
      if (isCommand(input, RUN_COMMAND)) {
        fillWithMode(wrapper, RUN_MODE, commandAttributes);
        return;
      }
      if (isCommand(input, DEBUG_COMMAND)) {
        fillWithMode(wrapper, DEBUG_MODE, commandAttributes);
        return;
      }

      Executor debugExecutor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);
      Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();

      KeyStroke enterStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
      KeyStroke shiftEnterStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
      if (debugExecutor != null) {
        executorInfo.append(debugExecutor.getId(), commandAttributes);
        executorInfo.append("(" + KeymapUtil.getKeystrokeText(enterStroke) + ")", shortcutAttributes);
        if (runExecutor != null) {
          executorInfo.append(" / " + runExecutor.getId(), commandAttributes);
          executorInfo.append("(" + KeymapUtil.getKeystrokeText(shiftEnterStroke) + ")", shortcutAttributes);
        }
      }
      else {
        if (runExecutor != null) {
          executorInfo.append(runExecutor.getId(), commandAttributes);
          executorInfo.append("(" + KeymapUtil.getKeystrokeText(enterStroke) + ")", shortcutAttributes);
        }
      }
    }

    private void fillWithMode(ChooseRunConfigurationPopup.ItemWrapper wrapper, @MagicConstant(intValues = {RUN_MODE, DEBUG_MODE}) int mode,
                              SimpleTextAttributes attributes) {
      Optional.ofNullable(ObjectUtils.tryCast(wrapper.getValue(), RunnerAndConfigurationSettings.class))
        .map(settings -> findExecutor(settings, mode))
        .ifPresent(executor -> {
          executorInfo.append(executor.getId(), attributes);
          executorInfo.setIcon(executor.getToolWindowIcon());
        });
    }
  }

  @Nullable
  private static Executor findExecutor(@NotNull RunnerAndConfigurationSettings settings,
                                       @MagicConstant(intValues = {RUN_MODE, DEBUG_MODE}) int mode) {
    Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();
    Executor debugExecutor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);

    Executor executor = mode == RUN_MODE ? runExecutor : debugExecutor;
    if (executor == null) {
      return null;
    }

    RunConfiguration runConf = settings.getConfiguration();
    if (ProgramRunner.getRunner(executor.getId(), runConf) == null) {
      executor = runExecutor == executor ? debugExecutor : runExecutor;
    }

    return executor;
  }
}
