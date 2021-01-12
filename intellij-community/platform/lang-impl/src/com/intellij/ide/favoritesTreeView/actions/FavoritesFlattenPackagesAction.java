/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import com.intellij.openapi.project.Project;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesFlattenPackagesAction extends FavoritesToolbarButtonAction {
  public FavoritesFlattenPackagesAction(Project project, FavoritesViewTreeBuilder builder) {
    super(project, builder, IdeBundle.message("action.flatten.packages"), AllIcons.ObjectBrowser.FlattenPackages);
  }

  @Override
  public boolean isOptionEnabled() {
    return getViewSettings().isFlattenPackages();
  }

  @Override
  public void setOption(boolean enabled) {
    getViewSettings().setFlattenPackages(enabled);
  }
}
