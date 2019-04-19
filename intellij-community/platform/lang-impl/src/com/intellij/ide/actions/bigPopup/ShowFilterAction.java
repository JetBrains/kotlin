// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.bigPopup;

import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class ShowFilterAction extends ToggleAction implements DumbAware {
  @NotNull private final Disposable myParentDisposable;
  @NotNull private final Project myProject;
  private JBPopup myFilterPopup;

  public ShowFilterAction(@NotNull Disposable parentDisposable, @NotNull Project project) {
    super("Filter", "Filter files by type", AllIcons.General.Filter);
    myParentDisposable = parentDisposable;
    myProject = project;
  }

  @Override
  public boolean isSelected(@NotNull final AnActionEvent e) {
    return myFilterPopup != null && !myFilterPopup.isDisposed();
  }

  @Override
  public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
    if (state) {
      showPopup(e.getInputEvent().getComponent());
    }
    else {
      if (myFilterPopup != null && !myFilterPopup.isDisposed()) {
        myFilterPopup.cancel();
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Icon icon = getTemplatePresentation().getIcon();
    e.getPresentation().setIcon(isActive() ? ExecutionUtil.getLiveIndicator(icon) : icon);
    e.getPresentation().setEnabled(isEnabled());
    e.getPresentation().putClientProperty(SELECTED_PROPERTY, isSelected(e));
  }

  protected abstract boolean isEnabled();

  protected abstract boolean isActive();

  private void showPopup(Component anchor) {
    if (myFilterPopup != null) {
      return;
    }
    JBPopupListener popupCloseListener = new JBPopupListener() {
      @Override
      public void onClosed(@NotNull LightweightWindowEvent event) {
        myFilterPopup = null;
      }
    };
    myFilterPopup = JBPopupFactory.getInstance()
      .createComponentPopupBuilder(createFilterPanel(), null)
      .setModalContext(false)
      .setFocusable(false)
      .setResizable(true)
      .setCancelOnClickOutside(false)
      .setMinSize(new Dimension(200, 200))
      .setDimensionServiceKey(myProject, "Search_Everywhere_Filter_Popup", false)
      .addListener(popupCloseListener)
      .createPopup();
    Disposer.register(myParentDisposable, myFilterPopup);
    myFilterPopup.showUnderneathOf(anchor);
  }

  private JComponent createFilterPanel() {
    ElementsChooser<?> chooser = createChooser();

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(chooser);
    JPanel buttons = new JPanel();
    JButton all = new JButton("All");
    all.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        chooser.setAllElementsMarked(true);
      }
    });
    buttons.add(all);
    JButton none = new JButton("None");
    none.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        chooser.setAllElementsMarked(false);
      }
    });
    buttons.add(none);
    JButton invert = new JButton("Invert");
    invert.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        chooser.invertSelection();
      }
    });
    buttons.add(invert);
    panel.add(buttons);
    return panel;
  }

  protected abstract ElementsChooser<?> createChooser();
}
