// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder.ModelListener;
import com.intellij.openapi.roots.ui.configuration.SdkPopup.SdkPopupListener;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.list.ComboBoxPopup;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class SdkPopupFactory {
  private final SdkListModelBuilder myModelBuilder;
  @Nullable private final Project myProject;
  @NotNull private final ProjectSdksModel mySdkModel;

  public SdkPopupFactory(@Nullable Project project,
                         @NotNull ProjectSdksModel sdkModel,
                         @NotNull SdkListModelBuilder modelBuilder) {
    myProject = project;
    mySdkModel = sdkModel;
    myModelBuilder = modelBuilder;
  }

  @NotNull
  public SdkPopup createPopup(@NotNull JComponent parent,
                              @NotNull SdkPopupListener listener) {
    SdkListItemContext context = new SdkListItemContext();
    SdkPopupImpl popup = new SdkPopupImpl(context, value -> {
      if (myModelBuilder.executeAction(parent, value, listener::onNewItemAdded)) {
        return;
      }
      listener.onExistingItemSelected(value);
    });

    ModelListener modelListener = new ModelListener() {
      @Override
      public void syncModel(@NotNull SdkListModel model) {
        context.myModel = model;
        popup.syncWithModelChange();
      }
    };

    myModelBuilder.addModelListener(modelListener);

    popup.addListener(new JBPopupListener() {
      @Override
      public void beforeShown(@NotNull LightweightWindowEvent event) {
        myModelBuilder.reloadActions();
        myModelBuilder.detectItems(popup.getList(), popup);
      }

      @Override
      public void onClosed(@NotNull LightweightWindowEvent event) {
        myModelBuilder.removeListener(modelListener);
        listener.onClosed();
      }
    });

    return popup;
  }

  private class SdkPopupImpl extends ComboBoxPopup<SdkListItem> implements SdkPopup {
    SdkPopupImpl(SdkListItemContext context, Consumer<SdkListItem> onItemSelected) {
      super(context, null, onItemSelected);
    }

    @Override
    public void showPopup(@NotNull AnActionEvent e) {
      if (e instanceof AnActionButton.AnActionEventWrapper) {
        ((AnActionButton.AnActionEventWrapper)e).showPopup(this);
      } else {
        showInBestPositionFor(e.getDataContext());
      }
    }

    @Override
    public void showUnderneathToTheRightOf(@NotNull Component component) {
      int popupWidth = getList().getPreferredSize().width;
      show(new RelativePoint(component, new Point(component.getWidth() - popupWidth, component.getHeight())));
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
  }
}
