// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.actions.ClearConsoleAction;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ObservableConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DuplexConsoleView<S extends ConsoleView, T extends ConsoleView> extends JPanel implements ConsoleView, 
                                                                                                       ObservableConsoleView,
                                                                                                       DataProvider {
  private final static String PRIMARY_CONSOLE_PANEL = "PRIMARY_CONSOLE_PANEL";
  private final static String SECONDARY_CONSOLE_PANEL = "SECONDARY_CONSOLE_PANEL";

  @NotNull
  private final S myPrimaryConsoleView;
  @NotNull
  private final T mySecondaryConsoleView;
  @Nullable
  private final String myStateStorageKey;

  private boolean myPrimary;
  @Nullable
  private ProcessHandler myProcessHandler;
  @NotNull
  private final SwitchDuplexConsoleViewAction mySwitchConsoleAction;
  private boolean myDisableSwitchConsoleActionOnProcessEnd = true;

  public DuplexConsoleView(@NotNull S primaryConsoleView, @NotNull T secondaryConsoleView) {
    this(primaryConsoleView, secondaryConsoleView, null);
  }

  public DuplexConsoleView(@NotNull S primaryConsoleView, @NotNull T secondaryConsoleView, @Nullable String stateStorageKey) {
    super(new CardLayout());
    myPrimaryConsoleView = primaryConsoleView;
    mySecondaryConsoleView = secondaryConsoleView;
    myStateStorageKey = stateStorageKey;

    add(myPrimaryConsoleView.getComponent(), PRIMARY_CONSOLE_PANEL);
    add(mySecondaryConsoleView.getComponent(), SECONDARY_CONSOLE_PANEL);

    mySwitchConsoleAction = new SwitchDuplexConsoleViewAction();

    myPrimary = true;
    enableConsole(getStoredState());

    Disposer.register(this, myPrimaryConsoleView);
    Disposer.register(this, mySecondaryConsoleView);
  }

  public static <S extends ConsoleView, T extends ConsoleView> DuplexConsoleView<S, T> create(@NotNull S primary,
                                                                                              @NotNull T secondary,
                                                                                              @Nullable String stateStorageKey) {
    return new DuplexConsoleView<>(primary, secondary, stateStorageKey);
  }

  private void setStoredState(boolean primary) {
    if (myStateStorageKey != null) {
      PropertiesComponent.getInstance().setValue(myStateStorageKey, primary);
    }
  }

  private boolean getStoredState() {
    if (myStateStorageKey == null) {
      return false;
    }
    return PropertiesComponent.getInstance().getBoolean(myStateStorageKey);
  }

  public void enableConsole(boolean primary) {
    if (primary == myPrimary) {
      // nothing to do
      return;
    }

    CardLayout cl = (CardLayout)(getLayout());
    cl.show(this, primary ? PRIMARY_CONSOLE_PANEL : SECONDARY_CONSOLE_PANEL);

    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(getSubConsoleView(primary).getComponent(), true));

    myPrimary = primary;
  }

  public boolean isPrimaryConsoleEnabled() {
    return myPrimary;
  }

  @NotNull
  public S getPrimaryConsoleView() {
    return myPrimaryConsoleView;
  }

  @NotNull
  public T getSecondaryConsoleView() {
    return mySecondaryConsoleView;
  }

  public ConsoleView getSubConsoleView(boolean primary) {
    return primary ? getPrimaryConsoleView() : getSecondaryConsoleView();
  }

  @Override
  public void print(@NotNull String s, @NotNull ConsoleViewContentType contentType) {
    myPrimaryConsoleView.print(s, contentType);
    mySecondaryConsoleView.print(s, contentType);
  }

  @Override
  public void clear() {
    myPrimaryConsoleView.clear();
    mySecondaryConsoleView.clear();
  }

  @Override
  public void scrollTo(int offset) {
    myPrimaryConsoleView.scrollTo(offset);
    mySecondaryConsoleView.scrollTo(offset);
  }

  @Override
  public void attachToProcess(ProcessHandler processHandler) {
    myProcessHandler = processHandler;

    myPrimaryConsoleView.attachToProcess(processHandler);
    mySecondaryConsoleView.attachToProcess(processHandler);
  }

  @Override
  public void setOutputPaused(boolean value) {
    myPrimaryConsoleView.setOutputPaused(value);
    mySecondaryConsoleView.setOutputPaused(value);
  }

  @Override
  public boolean isOutputPaused() {
    return false;
  }

  @Override
  public boolean hasDeferredOutput() {
    return myPrimaryConsoleView.hasDeferredOutput() && mySecondaryConsoleView.hasDeferredOutput();
  }

  @Override
  public void performWhenNoDeferredOutput(@NotNull Runnable runnable) {
  }

  @Override
  public void setHelpId(@NotNull String helpId) {
    myPrimaryConsoleView.setHelpId(helpId);
    mySecondaryConsoleView.setHelpId(helpId);
  }

  @Override
  public void addMessageFilter(@NotNull Filter filter) {
    myPrimaryConsoleView.addMessageFilter(filter);
    mySecondaryConsoleView.addMessageFilter(filter);
  }

  @Override
  public void printHyperlink(@NotNull String hyperlinkText, HyperlinkInfo info) {
    myPrimaryConsoleView.printHyperlink(hyperlinkText, info);
    mySecondaryConsoleView.printHyperlink(hyperlinkText, info);
  }

  @Override
  public int getContentSize() {
    return myPrimaryConsoleView.getContentSize();
  }

  @Override
  public boolean canPause() {
    return false;
  }


  @NotNull
  @Override
  public AnAction[] createConsoleActions() {
    List<AnAction> actions = Lists.newArrayList();
    actions.addAll(mergeConsoleActions(Arrays.asList(myPrimaryConsoleView.createConsoleActions()), 
                                       Arrays.asList(mySecondaryConsoleView.createConsoleActions())));
    actions.add(mySwitchConsoleAction);

    LanguageConsoleView langConsole = ContainerUtil.findInstance(Arrays.asList(myPrimaryConsoleView, mySecondaryConsoleView), LanguageConsoleView.class);
    ConsoleHistoryController controller = langConsole != null ? ConsoleHistoryController.getController(langConsole) : null;
    if (controller != null) actions.add(controller.getBrowseHistory());

    return actions.toArray(AnAction.EMPTY_ARRAY);
  }

  @Override
  public void allowHeavyFilters() {
    myPrimaryConsoleView.allowHeavyFilters();
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return this;
  }

  @Override
  public void dispose() {
    // registered children in constructor
  }

  @Override
  public void addChangeListener(@NotNull ChangeListener listener, @NotNull Disposable parent) {
    if (myPrimaryConsoleView instanceof ObservableConsoleView) {
      ((ObservableConsoleView)myPrimaryConsoleView).addChangeListener(listener, parent);
    }
    if (mySecondaryConsoleView instanceof ObservableConsoleView) {
      ((ObservableConsoleView)mySecondaryConsoleView).addChangeListener(listener, parent);
    }
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    final ConsoleView consoleView = getSubConsoleView(isPrimaryConsoleEnabled());
    if (consoleView instanceof DataProvider) {
      return ((DataProvider)consoleView).getData(dataId);
    }
    else {
      return null;
    }
  }

  @NotNull
  public Presentation getSwitchConsoleActionPresentation() {
    return mySwitchConsoleAction.getTemplatePresentation();
  }

  public void setDisableSwitchConsoleActionOnProcessEnd(boolean disableSwitchConsoleActionOnProcessEnd) {
    myDisableSwitchConsoleActionOnProcessEnd = disableSwitchConsoleActionOnProcessEnd;
  }
  
  @NotNull
  private List<AnAction> mergeConsoleActions(@NotNull List<? extends AnAction> actions1, @NotNull Collection<? extends AnAction> actions2) {
    return ContainerUtil.map(actions1, action1 -> {
      final AnAction action2 = ContainerUtil.find(actions2, action -> action1.getClass() == action.getClass()
                                                                      && StringUtil.equals(action1.getTemplatePresentation().getText(),
                                                                                           action.getTemplatePresentation().getText()));
      if (action2 instanceof ToggleUseSoftWrapsToolbarAction) {
        return new MergedWrapTextAction(((ToggleUseSoftWrapsToolbarAction)action1), (ToggleUseSoftWrapsToolbarAction)action2);
      }
      else if (action2 instanceof ScrollToTheEndToolbarAction) {
        return new MergedToggleAction(((ToggleAction)action1), (ToggleAction)action2);
      }
      else if (action2 instanceof ClearConsoleAction) {
        return new MergedAction(action1, action2);
      }
      else {
        return action1;
      }
    });
  }

  private class MergedWrapTextAction extends MergedToggleAction {

    private MergedWrapTextAction(@NotNull ToggleUseSoftWrapsToolbarAction action1, @NotNull ToggleUseSoftWrapsToolbarAction action2) {
      super(action1, action2);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      super.setSelected(e, state);
      DuplexConsoleView.this.getComponent().revalidate();
    }
  }
  
  private class SwitchDuplexConsoleViewAction extends ToggleAction implements DumbAware {

    SwitchDuplexConsoleViewAction() {
      super(ExecutionBundle.message("run.configuration.show.command.line.action.name"), null,
            AllIcons.Debugger.Console);
    }

    @Override
    public boolean isSelected(@NotNull final AnActionEvent event) {
      return !isPrimaryConsoleEnabled();
    }

    @Override
    public void setSelected(@NotNull final AnActionEvent event, final boolean flag) {
      enableConsole(!flag);
      setStoredState(!flag);
      ApplicationManager.getApplication().invokeLater(() -> update(event));
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      super.update(event);
      if(!myDisableSwitchConsoleActionOnProcessEnd) return;

      final Presentation presentation = event.getPresentation();
      final boolean isRunning = myProcessHandler != null && !myProcessHandler.isProcessTerminated();
      if (isRunning) {
        presentation.setEnabled(true);
      }
      else {
        enableConsole(true);
        Toggleable.setSelected(presentation, false);
        presentation.setEnabled(false);
      }
    }
  }
  
  private static class MergedToggleAction extends ToggleAction implements DumbAware {
    @NotNull
    private final ToggleAction myAction1;
    @NotNull
    private final ToggleAction myAction2;

    private MergedToggleAction(@NotNull ToggleAction action1, @NotNull ToggleAction action2) {
      myAction1 = action1;
      myAction2 = action2;
      copyFrom(action1);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return myAction1.isSelected(e);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      myAction1.setSelected(e, state);
      myAction2.setSelected(e, state);
    }
  }

  private static class MergedAction extends AnAction implements DumbAware {
    @NotNull
    private final AnAction myAction1;
    @NotNull
    private final AnAction myAction2;

    private MergedAction(@NotNull AnAction action1, @NotNull AnAction action2) {
      myAction1 = action1;
      myAction2 = action2;
      copyFrom(action1);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myAction1.actionPerformed(e);
      myAction2.actionPerformed(e);
    }
  }
  
}
