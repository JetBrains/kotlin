// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class SimpleChooseByNameModel implements ChooseByNameModel {
  private final Project myProject;
  private final String myPrompt;
  private final String myHelpId;

  protected SimpleChooseByNameModel(@NotNull Project project, @NotNull String prompt, @Nullable String helpId) {
    myProject = project;
    myPrompt = prompt;
    myHelpId = helpId;
  }

  public abstract String[] getNames();

  protected abstract Object[] getElementsByName(String name, String pattern);


  public Project getProject() {
    return myProject;
  }

  @Override
  public String getPromptText() {
    return myPrompt;
  }

  @NotNull
  @Override
  public String getNotInMessage() {
    return InspectionsBundle.message("nothing.found");
  }

  @NotNull
  @Override
  public String getNotFoundMessage() {
    return InspectionsBundle.message("nothing.found");
  }

  @Override
  public String getCheckBoxName() {
    return null;
  }


  @Override
  public boolean loadInitialCheckBoxState() {
    return false;
  }

  @Override
  public void saveInitialCheckBoxState(boolean state) {
  }

  @NotNull
  @Override
  public String[] getNames(boolean checkBoxState) {
    return getNames();
  }

  @NotNull
  @Override
  public Object[] getElementsByName(@NotNull String name, boolean checkBoxState, @NotNull String pattern) {
    return getElementsByName(name, pattern);
  }

  @NotNull
  @Override
  public String[] getSeparators() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Override
  public String getFullName(@NotNull Object element) {
    return getElementName(element);
  }

  @Override
  public String getHelpId() {
    return myHelpId;
  }

  @Override
  public boolean willOpenEditor() {
    return false;
  }

  @Override
  public boolean useMiddleMatching() {
    return false;
  }
}
