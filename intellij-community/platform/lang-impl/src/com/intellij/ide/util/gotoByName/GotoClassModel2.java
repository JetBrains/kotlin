// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoClassPresentationUpdater;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.WeakReferenceDisposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IdeUICustomization;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GotoClassModel2 extends FilteringGotoByModel<LanguageRef> {
  private String[] mySeparators;

  public GotoClassModel2(@NotNull Project project) {
    super(project, new ChooseByNameContributor[0]);
  }

  @Override
  protected List<ChooseByNameContributor> getContributorList() {
    return ChooseByNameContributor.CLASS_EP_NAME.getExtensionList();
  }

  @Override
  protected LanguageRef filterValueFor(NavigationItem item) {
    return LanguageRef.forNavigationitem(item);
  }

  @Override
  protected synchronized Collection<LanguageRef> getFilterItems() {
    final Collection<LanguageRef> result = super.getFilterItems();
    if (result == null) {
      return null;
    }
    final Collection<LanguageRef> items = new HashSet<>(result);
    items.add(LanguageRef.forLanguage(Language.ANY));
    return items;
  }

  @Override
  @Nullable
  public String getPromptText() {
    return IdeBundle.message("prompt.gotoclass.enter.class.name", StringUtil.toLowerCase(GotoClassPresentationUpdater.getActionTitle()));
  }

  @Override
  public String getCheckBoxName() {
    return IdeUICustomization.getInstance().projectMessage("checkbox.include.non.project.items");
  }

  @NotNull
  @Override
  public String getNotInMessage() {
    return IdeUICustomization.getInstance().projectMessage("label.no.matches.found.in.project");
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

    for (ChooseByNameContributor c : getContributorList()) {
      if (c instanceof GotoClassContributor) {
        String result = ((GotoClassContributor)c).getQualifiedName((NavigationItem)element);
        if (result != null) return result;
      }
    }

    return getElementName(element);
  }

  @Override
  public String @NotNull [] getSeparators() {
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
    return ArrayUtilRt.toStringArray(separators);
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
