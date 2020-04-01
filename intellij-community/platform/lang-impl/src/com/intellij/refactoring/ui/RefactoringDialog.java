// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.ui;

import com.intellij.ide.HelpTooltip;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.idea.ActionsBundle;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Author: msk
 */
public abstract class RefactoringDialog extends DialogWrapper {
  private Action myRefactorAction;
  private Action myPreviewAction;
  private boolean myCbPreviewResults;
  protected final Project myProject;

  protected RefactoringDialog(@NotNull Project project, boolean canBeParent) {
    this(project, canBeParent, false);
  }

  protected RefactoringDialog(@NotNull Project project, boolean canBeParent, boolean addOpenInEditorCheckbox) {
    super(project, canBeParent);
    myCbPreviewResults = true;
    myProject = project;
    if (addOpenInEditorCheckbox) {
      addOpenInEditorCheckbox();
    }
  }

  /**
   * Must be called before {@link #init()}.
   */
  protected void addOpenInEditorCheckbox() {
    setDoNotAskOption(new DoNotAskOption.Adapter() {
      @Override
      public void rememberChoice(boolean selected, int exitCode) {
        PropertiesComponent.getInstance().setValue(getRefactoringId() + ".OpenInEditor", selected, true);
        report(selected, "open.in.editor.saved");
      }

      @Override
      public boolean isSelectedByDefault() {
        boolean selected = PropertiesComponent.getInstance().getBoolean(getRefactoringId() + ".OpenInEditor", true);
        return report(selected, "open.in.editor.shown");
      }

      private boolean report(boolean selected, String eventId) {
        String refactoringClassName = RefactoringDialog.this.getClass().getName();
        FeatureUsageData data = new FeatureUsageData().addData("selected", selected).addData("class_name", refactoringClassName);
        FUCounterUsageLogger.getInstance().logEvent("refactoring.dialog", eventId, data);
        return selected;
      }

      @NotNull
      @Override
      public String getDoNotShowMessage() {
        return RefactoringBundle.message("open.in.editor.label");
      }
    });
  }

  @NonNls
  @NotNull
  protected String getRefactoringId() {
    return getClass().getName();
  }

  public boolean isOpenInEditor() {
    return myCheckBoxDoNotShowDialog != null && myCheckBoxDoNotShowDialog.isSelected();
  }

  public final boolean isPreviewUsages() {
    return myCbPreviewResults;
  }

  public void setPreviewResults(boolean previewResults) {
    myCbPreviewResults = previewResults;
  }

  @Override
  public void show() {
    IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
    super.show();
  }

  @Override
  protected void createDefaultActions() {
    super.createDefaultActions();
    myRefactorAction = new RefactorAction();
    myPreviewAction = new PreviewAction();
  }

  /**
   * @return default implementation of "Refactor" action.
   */
  protected final Action getRefactorAction() {
    return myRefactorAction;
  }

  /**
   * @return default implementation of "Preview" action.
   */
  protected final Action getPreviewAction() {
    return myPreviewAction;
  }

  protected abstract void doAction();

  private void doPreviewAction() {
    myCbPreviewResults = true;
    doAction();
  }

  protected void doRefactorAction() {
    myCbPreviewResults = false;
    doAction();
  }

  protected final void closeOKAction() { super.doOKAction(); }

  @Override
  protected final void doOKAction() {
    if (!DumbService.isDumbAware(this) && DumbService.isDumb(myProject)) {
      Messages.showMessageDialog(myProject, RefactoringBundle.message("refactoring.not.available.indexing"),
                                 RefactoringBundle.message("refactoring.indexing.warning.title"), null);
      return;
    }

    doAction();
  }

  protected boolean areButtonsValid() { return true; }

  protected void canRun() throws ConfigurationException {
    if (!areButtonsValid()) throw new ConfigurationException(null);
  }

  @Override
  protected void setHelpTooltip(@NotNull JButton helpButton) {
    if (Registry.is("ide.helptooltip.enabled")) {
      new HelpTooltip().setDescription(ActionsBundle.actionDescription("HelpTopics")).installOn(helpButton);
    }
    else {
      super.setHelpTooltip(helpButton);
    }
  }

  protected void validateButtons() {
    boolean enabled = true;
    try {
      setErrorText(null);
      canRun();
    }
    catch (ConfigurationException e) {
      enabled = false;
      setErrorText(e.getMessage());
    }
    getPreviewAction().setEnabled(enabled);
    getRefactorAction().setEnabled(enabled);
  }

  protected boolean hasHelpAction() {
    return true;
  }

  protected boolean hasPreviewButton() {
    return true;
  }

  @Override
  protected Action @NotNull [] createActions() {
    List<Action> actions = new ArrayList<>();
    actions.add(getRefactorAction());
    if (hasPreviewButton()) {
      actions.add(getPreviewAction());
    }
    actions.add(getCancelAction());
    if (hasHelpAction()) {
      actions.add(getHelpAction());
    }
    if (SystemInfo.isMac) {
      Collections.reverse(actions);
    }
    return actions.toArray(new Action[0]);
  }

  protected Project getProject() {
    return myProject;
  }

  private class RefactorAction extends AbstractAction {
    RefactorAction() {
      super(RefactoringBundle.message("refactor.button"));
      putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      doRefactorAction();
    }
  }

  private class PreviewAction extends AbstractAction {
    PreviewAction() {
      super(RefactoringBundle.message("preview.button"));
      if (SystemInfo.isMac) {
        putValue(FOCUSED_ACTION, Boolean.TRUE);
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      doPreviewAction();
    }
  }

  protected void invokeRefactoring(BaseRefactoringProcessor processor) {
    final Runnable prepareSuccessfulCallback = () -> close(DialogWrapper.OK_EXIT_CODE);
    processor.setPrepareSuccessfulSwingThreadCallback(prepareSuccessfulCallback);
    processor.setPreviewUsages(isPreviewUsages());
    processor.run();
  }
}