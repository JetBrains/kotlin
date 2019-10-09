// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.ui.IdeUICustomization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class GotoSymbolModel2 extends FilteringGotoByModel<Language> {
  private String[] mySeparators;
  private final boolean myAllContributors;

  public GotoSymbolModel2(@NotNull Project project, @NotNull ChooseByNameContributor[] contributors) {
    super(project, contributors);
    myAllContributors = false;
  }

  public GotoSymbolModel2(@NotNull Project project) {
    super(project, new ChooseByNameContributor[0]);
    myAllContributors = true;
  }

  @Override
  protected List<ChooseByNameContributor> getContributorList() {
    if (myAllContributors) {
      return ChooseByNameRegistry.getInstance().getSymbolModelContributors();
    }
    return super.getContributorList();
  }

  @Override
  protected Language filterValueFor(NavigationItem item) {
    return item instanceof PsiElement ? ((PsiElement) item).getLanguage() : null;
  }

  @Nullable
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
  public String getPromptText() {
    return IdeBundle.message("prompt.gotosymbol.enter.symbol.name");
  }

  @Override
  public String getCheckBoxName() {
    return IdeBundle.message("checkbox.include.non.project.symbols", IdeUICustomization.getInstance().getProjectConceptName());
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
           Boolean.TRUE.toString().equals(propertiesComponent.getValue("GoToSymbol.includeLibraries"));
  }

  @Override
  public void saveInitialCheckBoxState(boolean state) {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    if (Boolean.TRUE.toString().equals(propertiesComponent.getValue("GoToClass.toSaveIncludeLibraries"))){
      propertiesComponent.setValue("GoToSymbol.includeLibraries", Boolean.toString(state));
    }
  }

  @Override
  public String getFullName(@NotNull final Object element) {
    for(ChooseByNameContributor c: getContributorList()) {
      if (c instanceof GotoClassContributor) {
        String result = ((GotoClassContributor) c).getQualifiedName((NavigationItem) element);
        if (result != null) {
          return result;
        }
      }
    }

    String elementName = getElementName(element);
    if (elementName == null) return null;

    if (element instanceof PsiElement) {
      return SymbolPresentationUtil.getSymbolContainerText((PsiElement)element) + "." + elementName;
    }

    return elementName;
  }

  @Override
  @NotNull
  public String[] getSeparators() {
    if (mySeparators == null) {
      mySeparators = GotoClassModel2.getSeparatorsFromContributors(getContributors());
    }
    return mySeparators;
  }

  @Override
  public String getHelpId() {
    return "procedures.navigating.goto.class";
  }

  @Override
  public boolean willOpenEditor() {
    return true;
  }
}
