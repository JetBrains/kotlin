// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.ui;

import com.intellij.ide.HelpTooltip;
import com.intellij.ide.IdeEventQueue;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.RefactoringBundle;
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
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.ui.RefactoringDialog");

  private Action myRefactorAction;
  private Action myPreviewAction;
  private boolean myCbPreviewResults;
  protected final Project myProject;

  protected RefactoringDialog(@NotNull Project project, boolean canBeParent) {
    super(project, canBeParent);
    myCbPreviewResults = true;
    myProject = project;
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
    LOG.assertTrue(TransactionGuard.getInstance().getContextTransaction() != null, "Refactorings should be invoked inside transaction");
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
    if (DumbService.isDumb(myProject)) {
      Messages.showMessageDialog(myProject, "Refactoring is not available while indexing is in progress", "Indexing", null);
      return;
    }

    doAction();
  }

  protected boolean areButtonsValid() { return true; }

  protected void canRun() throws ConfigurationException {
    if (!areButtonsValid()) throw new ConfigurationException(null);
  }

  @Override
  protected void setHelpTooltip(JButton helpButton) {
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

  @NotNull
  @Override
  protected Action[] createActions() {
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