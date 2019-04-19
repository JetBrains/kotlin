/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public abstract class FavoritesToolbarButtonAction extends AnActionButton implements Toggleable, DumbAware {
  private FavoritesViewTreeBuilder myBuilder;
  private FavoritesViewSettings mySettings;

  public FavoritesToolbarButtonAction(Project project, FavoritesViewTreeBuilder builder, String name, Icon icon) {
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
    e.getPresentation().putClientProperty(SELECTED_PROPERTY, isOptionEnabled());
  }
}
