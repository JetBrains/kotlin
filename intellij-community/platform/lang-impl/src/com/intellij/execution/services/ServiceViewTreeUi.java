// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.ExecutionBundle;
import com.intellij.ide.navigationToolbar.NavBarBorder;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.*;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

class ServiceViewTreeUi implements ServiceViewUi {
  private final JPanel myMainPanel;
  private final SimpleToolWindowPanel myContentPanel = new SimpleToolWindowPanel(false);
  private final Splitter mySplitter;
  private final JPanel myMasterPanel;
  private final JPanel myDetailsPanel;
  private final JPanel myNavBarPanel;
  private final JBPanelWithEmptyText myMessagePanel = new JBPanelWithEmptyText().withEmptyText(
    ExecutionBundle.message("service.view.empty.selection.text"));
  private final Set<JComponent> myDetailsComponents = ContainerUtil.createWeakSet();
  private ActionToolbar myServiceActionToolbar;
  private ActionToolbar myMasterActionToolbar;

  ServiceViewTreeUi(@NotNull ServiceViewState state) {
    myMainPanel = new SimpleToolWindowPanel(false);

    myNavBarPanel = new JPanel(new BorderLayout());
    myNavBarPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
    myMainPanel.add(myNavBarPanel, BorderLayout.NORTH);

    mySplitter = new OnePixelSplitter(false, state.contentProportion);
    myMainPanel.add(myContentPanel, BorderLayout.CENTER);
    myContentPanel.setContent(mySplitter);

    myMasterPanel = new JPanel(new BorderLayout());
    mySplitter.setFirstComponent(myMasterPanel);

    myDetailsPanel = new JPanel(new BorderLayout());
    myMessagePanel.setFocusable(true);
    myDetailsPanel.add(myMessagePanel, BorderLayout.CENTER);
    mySplitter.setSecondComponent(myDetailsPanel);

    if (state.showServicesTree) {
      myNavBarPanel.setVisible(false);
    }
    else {
      myMasterPanel.setVisible(false);
    }

    ComponentUtil
      .putClientProperty(myMainPanel, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, (Iterable<? extends Component>)(Iterable<JComponent>)() ->
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
    myContentPanel.setToolbar(actionProvider.wrapServiceToolbar(myServiceActionToolbar));
  }

  @Override
  public void setMasterComponent(@NotNull JComponent component, @NotNull ServiceViewActionProvider actionProvider) {
    myMasterPanel.add(ScrollPaneFactory.createScrollPane(component, SideBorder.TOP), BorderLayout.CENTER);

    myMasterActionToolbar = actionProvider.createMasterComponentToolbar(component);
    JComponent toolbarComponent = myMasterActionToolbar.getComponent();
    toolbarComponent.setBorder(JBUI.Borders.empty(1, 0, 2, 0));
    myMasterPanel.add(toolbarComponent, BorderLayout.NORTH);

    actionProvider.installPopupHandler(component);
  }

  @Override
  public void setNavBar(@NotNull JComponent component) {
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(component);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    scrollPane.setHorizontalScrollBar(null);
    scrollPane.setBorder(new NavBarBorder());
    JPanel navBarPanelWrapper = new JPanel(new BorderLayout()) {
      @Override
      public void doLayout() {
        // align vertically
        Rectangle r = getBounds();
        Insets insets = getInsets();
        int x = insets.left;
        Dimension preferredSize = scrollPane.getPreferredSize();
        scrollPane.setBounds(x, (r.height - preferredSize.height) / 2, r.width - insets.left - insets.right, preferredSize.height);
      }
    };
    navBarPanelWrapper.add(scrollPane, BorderLayout.CENTER);
    myNavBarPanel.add(navBarPanelWrapper, BorderLayout.CENTER);
  }

  @Override
  public void setMasterComponentVisible(boolean visible) {
    myMasterPanel.setVisible(visible);
    myNavBarPanel.setVisible(!visible);
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
