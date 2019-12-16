// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder.ModelListener;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Condition;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.popup.list.ComboBoxPopup;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;

public class SdkPopup {
  private final SdkListModelBuilder myModel;
  @NotNull private final JComponent myParentComponent;
  @Nullable private final Project myProject;
  @NotNull private final ProjectSdksModel mySdkModel;

  /**
   * Creates new Sdk selector combobox
   *
   * @param project        current project (if any)
   * @param sdkModel       the sdks model
   * @param sdkTypeFilter  sdk types filter predicate to show
   * @param sdkFilter      filters Sdk instances that are listed, it implicitly includes the {@param sdkTypeFilter}
   * @param creationFilter a filter of SdkType that allowed to create a new Sdk with that control
   * @param onNewSdkAdded  a callback that is executed once a new Sdk is added to the list
   */
  public SdkPopup(@NotNull JComponent parentComponent,
                  @Nullable Project project,
                  @NotNull ProjectSdksModel sdkModel,
                  @Nullable Condition<? super SdkTypeId> sdkTypeFilter,
                  @Nullable Condition<? super Sdk> sdkFilter,
                  @Nullable Condition<? super SdkTypeId> creationFilter,
                  @Nullable Consumer<? super Sdk> onNewSdkAdded) {
    myParentComponent = parentComponent;
    myProject = project;
    mySdkModel = sdkModel;
    myModel = new SdkListModelBuilder(
      project,
      sdkModel,
      sdkTypeFilter,
      creationFilter,
      sdkFilter);

    myModel.addModelListener(new ModelListener() {
      @Override
      public void onNewSdkAdded(@NotNull Sdk sdk) {
        if (onNewSdkAdded != null) {
          onNewSdkAdded.consume(sdk);
        }
      }
    });
  }

  public void showPopup(@NotNull AnActionEvent e) {
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
      public void onClosed(@NotNull LightweightWindowEvent event) {
        myModel.removeListener(modelListener);
      }
    });

    myModel.reloadActions(myParentComponent, null);
    myModel.detectItems(myParentComponent);

    if (e instanceof AnActionButton.AnActionEventWrapper) {
      ((AnActionButton.AnActionEventWrapper)e).showPopup(popup);
    } else {
      popup.showInBestPositionFor(e.getDataContext());
    }
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
