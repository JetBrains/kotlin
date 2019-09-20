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
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.PropertyName;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesViewSettings implements ViewSettings {

  @PropertyName("favorites.view.settings.show.members")
  public boolean myShowMembers = false;

  @PropertyName("favorites.view.settings.flatten.packages")
  public boolean myFlattenPackages = false;

  @PropertyName("favorites.view.settings.autoscroll.to.source")
  public boolean myAutoScrollToSource = false;

  @PropertyName("favorites.view.settings.autoscroll.from.source")
  public boolean myAutoScrollFromSource = false;

  @PropertyName("favorites.view.settings.hide.empty.middle.packages")
  public boolean myHideEmptyMiddlePackages = true;

  @PropertyName("favorites.view.settings.abbreviate.qualified.package.names")
  public boolean myAbbreviateQualifiedPackages = false;


  public FavoritesViewSettings() {
    PropertiesComponent.getInstance().loadFields(this);
  }

  @Override
  public boolean isShowMembers() {
    return myShowMembers;
  }

  public void setShowMembers(boolean showMembers) {
    myShowMembers = showMembers;
    save();
  }

  private void save() {
    PropertiesComponent.getInstance().saveFields(this);
  }

  @Override
  public boolean isStructureView() {
    return false;
  }

  @Override
  public boolean isShowModules() {
    return true;
  }

  @Override
  public boolean isFlattenPackages() {
    return myFlattenPackages;
  }

  public boolean isAutoScrollFromSource() {
    return myAutoScrollFromSource;
  }

  public void setAutoScrollFromSource(boolean autoScrollFromSource) {
    myAutoScrollFromSource = autoScrollFromSource;
    save();
  }

  public void setFlattenPackages(boolean flattenPackages) {
    myFlattenPackages = flattenPackages;
    save();
  }

  @Override
  public boolean isAbbreviatePackageNames() {
    return myAbbreviateQualifiedPackages;
  }

  @Override
  public boolean isHideEmptyMiddlePackages() {
    return myHideEmptyMiddlePackages;
  }

  @Override
  public boolean isShowLibraryContents() {
    return false;
  }

  public boolean isAutoScrollToSource() {
    return myAutoScrollToSource;
  }

  public void setAutoScrollToSource(boolean autoScrollToSource) {
    myAutoScrollToSource = autoScrollToSource;
    save();
  }

  public void setHideEmptyMiddlePackages(boolean hide) {
    myHideEmptyMiddlePackages = hide;
    save();
  }

  public void setAbbreviateQualifiedPackages(boolean abbreviate) {
    myAbbreviateQualifiedPackages = abbreviate;
    save();
  }
}
