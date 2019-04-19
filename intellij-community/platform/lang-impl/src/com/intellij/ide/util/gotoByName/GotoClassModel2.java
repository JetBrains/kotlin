/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide.util.gotoByName;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoClassPresentationUpdater;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IdeUICustomization;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GotoClassModel2 extends FilteringGotoByModel<Language> {
  private String[] mySeparators;
  
  public GotoClassModel2(@NotNull Project project) {
    super(project, ChooseByNameRegistry.getInstance().getClassModelContributors());
  }

  @Override
  protected Language filterValueFor(NavigationItem item) {
    return item instanceof PsiElement ? ((PsiElement) item).getLanguage() : null;
  }

  @Override
  protected synchronized Collection<Language> getFilterItems() {
    final Collection<Language> result = super.getFilterItems();
    if (result == null) {
      return null;
    }
    final Collection<Language> items = new HashSet<>(result);
    items.add(Language.ANY);
    return items;
  }

  @Override
  @Nullable
  public String getPromptText() {
    return IdeBundle.message("prompt.gotoclass.enter.class.name", StringUtil.toLowerCase(GotoClassPresentationUpdater.getActionTitle()));
  }

  @Override
  public String getCheckBoxName() {
    return IdeBundle.message("checkbox.include.non.project.classes", IdeUICustomization.getInstance().getProjectConceptName());
  }

  @NotNull
  @Override
  public String getNotInMessage() {
    return IdeBundle.message("label.no.matches.found.in.project", IdeUICustomization.getInstance().getProjectConceptName());
  }

  @NotNull
  @Override
  public String getNotFoundMessage() {
    return IdeBundle.message("label.no.matches.found");
  }


  @Override
  public boolean loadInitialCheckBoxState() {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    return Boolean.TRUE.toString().equals(propertiesComponent.getValue("GoToClass.toSaveIncludeLibraries")) &&
           Boolean.TRUE.toString().equals(propertiesComponent.getValue("GoToClass.includeLibraries"));
  }

  @Override
  public void saveInitialCheckBoxState(boolean state) {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    if (Boolean.TRUE.toString().equals(propertiesComponent.getValue("GoToClass.toSaveIncludeLibraries"))){
      propertiesComponent.setValue("GoToClass.includeLibraries", Boolean.toString(state));
    }
  }

  @Override
  public String getFullName(@NotNull final Object element) {
    if (element instanceof PsiElement && !((PsiElement)element).isValid()) {
      return null;
    }

    for (ChooseByNameContributor c : getContributors()) {
      if (c instanceof GotoClassContributor) {
        String result = ((GotoClassContributor)c).getQualifiedName((NavigationItem)element);
        if (result != null) return result;
      }
    }

    return getElementName(element);
  }

  @Override
  @NotNull
  public String[] getSeparators() {
    if (mySeparators == null) {
      mySeparators = getSeparatorsFromContributors(getContributors());
    }
    return mySeparators;
  }

  public static String[] getSeparatorsFromContributors(ChooseByNameContributor[] contributors) {
    final Set<String> separators = new HashSet<>();
    separators.add(".");
    for(ChooseByNameContributor c: contributors) {
      if (c instanceof GotoClassContributor) {
        ContainerUtil.addIfNotNull(separators, ((GotoClassContributor)c).getQualifiedNameSeparator());
      }
    }
    return ArrayUtil.toStringArray(separators);
  }

  @Override
  public String getHelpId() {
    return "procedures.navigating.goto.class";
  }

  @NotNull
  @Override
  public String removeModelSpecificMarkup(@NotNull String pattern) {
    if (pattern.startsWith("@")) return pattern.substring(1);
    return pattern;
  }

  @Override
  public boolean willOpenEditor() {
    return true;
  }

  @Override
  public boolean sameNamesForProjectAndLibraries() {
    return false;
  }
}
