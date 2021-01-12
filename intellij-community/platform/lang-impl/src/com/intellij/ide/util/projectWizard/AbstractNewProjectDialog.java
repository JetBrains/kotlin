// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.projectWizard;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.DialogWrapperPeer;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Dennis.Ushakov
 */
public abstract class AbstractNewProjectDialog extends DialogWrapper {
  private Pair<JPanel, JBList<AnAction>> myPair;

  public AbstractNewProjectDialog() {
    super(ProjectManager.getInstance().getDefaultProject());
    init();
  }

  @Override
  protected void init() {
    super.init();
    DialogWrapperPeer peer = getPeer();
    JRootPane pane = peer.getRootPane();
    if (pane != null) {
      JBDimension size = JBUI.size(FlatWelcomeFrame.MAX_DEFAULT_WIDTH, FlatWelcomeFrame.DEFAULT_HEIGHT);
      pane.setMinimumSize(size);
      pane.setPreferredSize(size);
    }
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    setTitle(AbstractNewProjectStep.EP_NAME.hasAnyExtensions() ? ProjectBundle.message("dialog.title.new.project")
                                                                  : ProjectBundle.message("dialog.title.create.project"));
    DefaultActionGroup root = createRootStep();
    Disposer.register(getDisposable(), () -> root.removeAll());

    Pair<JPanel, JBList<AnAction>> pair = FlatWelcomeFrame.createActionGroupPanel(root, null, getDisposable());
    JPanel component = pair.first;
    myPair = pair;
    UiNotifyConnector.doWhenFirstShown(myPair.second, () -> ScrollingUtil.ensureSelectionExists(myPair.second));

    FlatWelcomeFrame.installQuickSearch(pair.second);
    return component;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return FlatWelcomeFrame.getPreferredFocusedComponent(myPair);
  }

  @Nullable
  @Override
  protected JComponent createSouthPanel() {
    return null;
  }

  @NotNull
  @Override
  protected DialogStyle getStyle() {
    return DialogStyle.COMPACT;
  }

  protected abstract DefaultActionGroup createRootStep();

  @Override
  protected String getHelpId() {
    return "create_new_project_dialog";
  }

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[0];
  }

}
