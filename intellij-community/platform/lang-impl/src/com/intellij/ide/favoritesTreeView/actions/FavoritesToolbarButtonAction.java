// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.favoritesTreeView.FavoritesViewSettings;
import com.intellij.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.AnActionButton;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.intellij.openapi.actionSystem.ActionPlaces.isPopupPlace;

/**
 * @author Konstantin Bulenkov
 */
public abstract class FavoritesToolbarButtonAction extends AnActionButton implements Toggleable, DumbAware {
  private FavoritesViewTreeBuilder myBuilder;
  private FavoritesViewSettings mySettings;

  public FavoritesToolbarButtonAction(Project project, FavoritesViewTreeBuilder builder,
                                      @Nls(capitalization = Nls.Capitalization.Title) String name, Icon icon) {
    super(name, icon);
    myBuilder = builder;
    mySettings = FavoritesManager.getInstance(project).getViewSettings();
    setContextComponent(myBuilder.getTree());
    Disposer.register(project, new Disposable() {
      @Override
      public void dispose() {
        myBuilder = null;
        mySettings = null;
      }
    });
  }

  public abstract boolean isOptionEnabled();

  public abstract void setOption(boolean enabled);

  public FavoritesViewSettings getViewSettings() {
    return mySettings;
  }

  public FavoritesViewTreeBuilder getBuilder() {
    return myBuilder;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    setOption(!isOptionEnabled());
    myBuilder.updateFromRootCB();
  }

  @Override
  public void updateButton(@NotNull AnActionEvent e) {
    super.updateButton(e);
    Toggleable.setSelected(e.getPresentation(), isOptionEnabled());
    if (isPopupPlace(e.getPlace())) e.getPresentation().setIcon(null);
  }
}
