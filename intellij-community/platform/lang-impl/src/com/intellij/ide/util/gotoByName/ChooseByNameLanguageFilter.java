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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * @author yole
 */
public class ChooseByNameLanguageFilter extends ChooseByNameFilter<LanguageRef> {
  public ChooseByNameLanguageFilter(@NotNull ChooseByNamePopup popup,
                                    @NotNull FilteringGotoByModel<LanguageRef> languageFilteringGotoByModel,
                                    @NotNull ChooseByNameFilterConfiguration<LanguageRef> languageChooseByNameFilterConfiguration,
                                    @NotNull Project project) {
    super(popup, languageFilteringGotoByModel, languageChooseByNameFilterConfiguration, project);
  }

  @Override
  protected String textForFilterValue(@NotNull LanguageRef value) {
    return value.getDisplayName();
  }

  @Nullable
  @Override
  protected Icon iconForFilterValue(@NotNull LanguageRef value) {
    return value.getIcon();
  }

  @NotNull
  @Override
  protected Collection<LanguageRef> getAllFilterValues() {
    return LanguageRef.forAllLanguages();
  }
}
