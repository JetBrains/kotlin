// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class contains UI related to filtering functionality.
 */
public abstract class ChooseByNameFilter<T> {
  /**
   * a parent popup
   */
  private final ChooseByNamePopup myParentPopup;
  /**
   * action toolbar
   */
  private final ActionToolbar myToolbar;
  /**
   * a file type chooser, only one instance is used
   */
  private final ElementsChooser<T> myChooser;
  /**
   * A panel that contains chooser
   */
  private final JPanel myChooserPanel;
  /**
   * a file type popup, the value is non-null if popup is active
   */
  private JBPopup myPopup;
  /**
   * a project to use. The project is used for dimension service.
   */
  private final Project myProject;

  /**
   * A constructor
   *
   * @param popup               a parent popup
   * @param model               a model for popup
   * @param filterConfiguration storage for selected filter values
   * @param project             a context project
   */
  public ChooseByNameFilter(@NotNull ChooseByNamePopup popup,
                            @NotNull FilteringGotoByModel<T> model,
                            @NotNull ChooseByNameFilterConfiguration<T> filterConfiguration,
                            @NotNull Project project) {
    myParentPopup = popup;
    DefaultActionGroup actionGroup = new DefaultActionGroup("go.to.file.filter", false);
    ToggleAction action = new FilterAction() {
      @Override
      protected boolean isActive() {
        return !filterConfiguration.getState().getFilteredOutFileTypeNames().isEmpty();
      }
    };
    actionGroup.add(action);
    myToolbar = ActionManager.getInstance().createActionToolbar("gotfile.filter", actionGroup, true);
    myToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    myToolbar.updateActionsImmediately();
    myToolbar.getComponent().setFocusable(false);
    myToolbar.getComponent().setBorder(null);
    myProject = project;
    myChooser = createChooser(model, filterConfiguration);
    myChooserPanel = createChooserPanel();
    popup.setToolArea(myToolbar.getComponent());
  }

  /**
   * @return a panel with chooser and buttons
   */
  private JPanel createChooserPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(myChooser);
    JPanel buttons = new JPanel();
    JButton all = new JButton("All");
    all.addActionListener(__ -> myChooser.setAllElementsMarked(true));
    buttons.add(all);
    JButton none = new JButton("None");
    none.addActionListener(__ -> myChooser.setAllElementsMarked(false));
    buttons.add(none);
    JButton invert = new JButton("Invert");
    invert.addActionListener(__ -> myChooser.invertSelection());
    buttons.add(invert);
    panel.add(buttons);
    return panel;
  }

  /**
   * Create a file type chooser
   *
   *
   * @param model a model to update
   * @return a created file chooser
   */
  @NotNull
  protected ElementsChooser<T> createChooser(@NotNull final FilteringGotoByModel<T> model,
                                             @NotNull final ChooseByNameFilterConfiguration<? super T> filterConfiguration) {
    List<T> elements = new ArrayList<>(getAllFilterValues());
    final ElementsChooser<T> chooser = new ElementsChooser<T>(elements, true) {
      @Override
      protected String getItemText(@NotNull final T value) {
        return textForFilterValue(value);
      }

      @Override
      protected Icon getItemIcon(@NotNull final T value) {
        return iconForFilterValue(value);
      }
    };
    chooser.setFocusable(false);
    final int count = chooser.getElementCount();
    for (int i = 0; i < count; i++) {
      T type = chooser.getElementAt(i);
      if (!DumbService.getInstance(myProject).isDumb() && !filterConfiguration.isFileTypeVisible(type)) {
        chooser.setElementMarked(type, false);
      }
    }
    updateModel(model, chooser, true);
    chooser.addElementsMarkListener((ElementsChooser.ElementsMarkListener<T>)(element, isMarked) -> {
      filterConfiguration.setVisible(element, isMarked);
      updateModel(model, chooser, false);
    });
    return chooser;
  }

  protected abstract String textForFilterValue(@NotNull T value);

  @Nullable
  protected abstract Icon iconForFilterValue(@NotNull T value);

  @NotNull
  protected abstract Collection<T> getAllFilterValues();

  /**
   * Update model basing on the chooser state
   *
   * @param gotoFileModel a model
   * @param chooser       a file type chooser
   */
  protected void updateModel(@NotNull FilteringGotoByModel<T> gotoFileModel, @NotNull ElementsChooser<T> chooser, boolean initial) {
    final List<T> markedElements = chooser.getMarkedElements();
    gotoFileModel.setFilterItems(markedElements);
    myParentPopup.rebuildList(initial);
  }

  /**
   * Create and show popup
   */
  private void createPopup() {
    if (myPopup != null) {
      return;
    }
    myPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(myChooserPanel, myChooser).setModalContext(false).setFocusable(false)
        .setResizable(true).setCancelOnClickOutside(false).setMinSize(new Dimension(200, 200))
        .setDimensionServiceKey(myProject, "GotoFile_FileTypePopup", false).createPopup();
    myPopup.addListener(new JBPopupListener() {
      @Override
      public void onClosed(@NotNull LightweightWindowEvent event) {
        myPopup = null;
      }
    });
    myPopup.showUnderneathOf(myToolbar.getComponent());
  }

  /**
   * close the file type filter
   */
  public void close() {
    if (myPopup != null) {
      Disposer.dispose(myPopup);
    }
  }

  private class FilterAction extends ToggleAction implements DumbAware {
    FilterAction() {
      super("Filter", "Filter files by type", AllIcons.General.Filter);
    }

    @Override
    public boolean isSelected(@NotNull final AnActionEvent e) {
      return myPopup != null;
    }

    @Override
    public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
      if (state) {
        createPopup();
      }
      else {
        close();
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Icon icon = getTemplatePresentation().getIcon();
      e.getPresentation().setIcon(isActive() ? ExecutionUtil.getLiveIndicator(icon) : icon);
    }
    
    protected boolean isActive() {
      return false;
    }
    
  }
}
