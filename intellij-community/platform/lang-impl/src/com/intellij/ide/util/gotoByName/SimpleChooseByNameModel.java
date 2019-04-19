/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
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
    return ArrayUtil.EMPTY_STRING_ARRAY;
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
