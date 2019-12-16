// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder.ModelListener;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.list.ComboBoxPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class SdkPopupFactory {
  private final SdkListModelBuilder myModel;
  @Nullable private final Project myProject;
  @NotNull private final ProjectSdksModel mySdkModel;

  public SdkPopupFactory(@Nullable Project project,
                         @NotNull ProjectSdksModel sdkModel,
                         @NotNull SdkListModelBuilder modelBuilder) {
    myProject = project;
    mySdkModel = sdkModel;
    myModel = modelBuilder;
  }

  @NotNull
  private ComboBoxPopup<SdkListItem> createPopup(@NotNull Runnable onClosed) {
    SdkListItemContext context = new SdkListItemContext();
    ComboBoxPopup<SdkListItem> popup = new ComboBoxPopup<>(context, null);

    ModelListener modelListener = new ModelListener() {
      @Override
      public void syncModel(@NotNull SdkListModel model) {
        context.myModel = model;
        popup.syncWithModelChange();
      }
    };
    myModel.addModelListener(modelListener);

    popup.addListener(new JBPopupListener() {
      @Override
      public void beforeShown(@NotNull LightweightWindowEvent event) {
        myModel.reloadActions(popup.getList(), null);
        myModel.detectItems(popup.getList(), popup);
      }

      @Override
      public void onClosed(@NotNull LightweightWindowEvent event) {
        myModel.removeListener(modelListener);
        onClosed.run();
      }
    });

    return popup;
  }

  public void showPopup(@NotNull AnActionEvent e,
                        @NotNull Runnable onClosed) {
    ComboBoxPopup<SdkListItem> popup = createPopup(onClosed);

    if (e instanceof AnActionButton.AnActionEventWrapper) {
      ((AnActionButton.AnActionEventWrapper)e).showPopup(popup);
    } else {
      popup.showInBestPositionFor(e.getDataContext());
    }
  }

  public void showPopup(@NotNull RelativePoint aPoint,
                        @NotNull Runnable onClosed) {
    createPopup(onClosed).show(aPoint);
  }

  public void showUnderneathToTheRightOf(@NotNull Component component,
                                         @NotNull Runnable onClosed) {
    ComboBoxPopup<SdkListItem> popup = createPopup(onClosed);
    int popupWidth = popup.getList().getPreferredSize().width;
    popup.show(new RelativePoint(component, new Point(component.getWidth() - popupWidth, component.getHeight())));
  }

  private class SdkListItemContext implements ComboBoxPopup.Context<SdkListItem> {
    SdkListModel myModel = new SdkListModel(true, Collections.emptyList());
    private final SdkListPresenter myRenderer;

    private SdkListItemContext() {
      myRenderer = new SdkListPresenter(mySdkModel) {
        @NotNull
        @Override
        protected SdkListModel getModel() {
          return myModel;
        }
      };
    }

    @Nullable
    @Override
    public Project getProject() {
      return myProject;
    }

    @Override
    public int getMaximumRowCount() {
      return 30;
    }

    @NotNull
    @Override
    public ListModel<SdkListItem> getModel() {
      return myModel;
    }

    @NotNull
    @Override
    public ListCellRenderer<? super SdkListItem> getRenderer() {
      return myRenderer;
    }

    @Override
    public void setSelectedItem(SdkListItem value) {
      if (value != null) {
        if (value instanceof SdkListItem.ActionableItem) {
          ((SdkListItem.ActionableItem)value).executeAction();
        }
      }
    }
  }
}
