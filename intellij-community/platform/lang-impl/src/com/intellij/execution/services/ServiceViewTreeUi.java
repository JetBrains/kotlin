// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

class ServiceViewTreeUi implements ServiceViewUi {
  private final JPanel myMainPanel;
  private final Splitter mySplitter;
  private final JPanel myMasterPanel;
  private final JPanel myDetailsPanel;
  private final JBPanelWithEmptyText myMessagePanel;
  private final Set<JComponent> myDetailsComponents = ContainerUtil.createWeakSet();
  private ActionToolbar myServiceActionToolbar;
  private ActionToolbar myMasterActionToolbar;

  ServiceViewTreeUi(@NotNull ServiceViewState state) {
    myMainPanel = new JPanel(new BorderLayout());

    mySplitter = new OnePixelSplitter(false, state.contentProportion);
    myMainPanel.add(mySplitter, BorderLayout.CENTER);

    myMasterPanel = new JPanel(new BorderLayout());
    mySplitter.setFirstComponent(myMasterPanel);

    myDetailsPanel = new JPanel(new BorderLayout());
    myMessagePanel = new JBPanelWithEmptyText().withEmptyText("Select service in tree to view details");
    myDetailsPanel.add(myMessagePanel, BorderLayout.CENTER);
    mySplitter.setSecondComponent(myDetailsPanel);

    UIUtil.putClientProperty(myMainPanel, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, (Iterable<JComponent>)() ->
      JBIterable.from(myDetailsComponents).append(myMessagePanel).filter(component -> myDetailsPanel != component.getParent()).iterator());
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myMainPanel;
  }

  @Override
  public void saveState(@NotNull ServiceViewState state) {
    state.contentProportion = mySplitter.getProportion();
  }

  @Override
  public void setServiceToolbar(@NotNull ServiceViewActionProvider actionProvider) {
    myServiceActionToolbar = actionProvider.createServiceToolbar(myMainPanel);
    myMainPanel.add(myServiceActionToolbar.getComponent(), BorderLayout.WEST);
  }

  @Override
  public void setMasterPanel(@NotNull JComponent component, @NotNull ServiceViewActionProvider actionProvider) {
    myMasterPanel.add(ScrollPaneFactory.createScrollPane(component, SideBorder.LEFT), BorderLayout.CENTER);

    myMasterActionToolbar = actionProvider.createMasterComponentToolbar(component);
    JComponent toolbarComponent = myMasterActionToolbar.getComponent();
    toolbarComponent.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT | SideBorder.BOTTOM));
    myMasterPanel.add(toolbarComponent, BorderLayout.NORTH);

    actionProvider.installPopupHandler(component);
  }

  @Override
  public void setDetailsComponent(@Nullable JComponent component) {
    if (component == null) {
      component = myMessagePanel;
    }
    if (component.getParent() == myDetailsPanel) return;

    myDetailsComponents.add(component);
    myDetailsPanel.removeAll();
    myDetailsPanel.add(component, BorderLayout.CENTER);
    myDetailsPanel.revalidate();
    myDetailsPanel.repaint();

    ActionToolbar serviceActionToolbar = myServiceActionToolbar;
    if (serviceActionToolbar != null) {
      serviceActionToolbar.updateActionsImmediately();
    }
    ActionToolbar masterActionToolbar = myMasterActionToolbar;
    if (masterActionToolbar != null) {
      masterActionToolbar.updateActionsImmediately();
    }
  }

  @Nullable
  @Override
  public JComponent getDetailsComponent() {
    int count = myDetailsPanel.getComponentCount();
    if (count == 0) return null;

    Component component = myDetailsPanel.getComponent(0);
    return component == myMessagePanel ? null : (JComponent)component;
  }
}
