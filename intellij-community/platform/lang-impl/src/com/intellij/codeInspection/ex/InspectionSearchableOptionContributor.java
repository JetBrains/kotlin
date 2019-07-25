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
package com.intellij.codeInspection.ex;

import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import com.intellij.profile.codeInspection.ui.header.InspectionToolsConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * @author peter
 */
public class InspectionSearchableOptionContributor extends SearchableOptionContributor {
  private static final Pattern HTML_PATTERN = Pattern.compile("<[^<>]*>");

  @Override
  public void processOptions(@NotNull SearchableOptionProcessor processor) {
    for (InspectionToolWrapper toolWrapper : InspectionToolRegistrar.getInstance().createTools()) {
      String hit = toolWrapper.getDisplayName();
      processor.addOptions(toolWrapper.getDisplayName(), toolWrapper.getShortName(), hit,
                           InspectionToolsConfigurable.ID,
                           InspectionToolsConfigurable.DISPLAY_NAME, false);

      for (String group : toolWrapper.getGroupPath()) {
        processor.addOptions(group, toolWrapper.getShortName(), hit, InspectionToolsConfigurable.ID, InspectionToolsConfigurable.DISPLAY_NAME, false);
      }

      final String description = toolWrapper.loadDescription();
      if (description != null) {
        @NonNls String descriptionText = HTML_PATTERN.matcher(description).replaceAll(" ");
        processor.addOptions(descriptionText, toolWrapper.getShortName(), hit, InspectionToolsConfigurable.ID,
                             InspectionToolsConfigurable.DISPLAY_NAME, false);
      }
    }

  }
}
