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
package com.intellij.codeInsight.template.impl;

import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author peter
 */
public class TemplateSearchableOptionContributor extends SearchableOptionContributor {
  @Override
  public void processOptions(@NotNull SearchableOptionProcessor processor) {
    Set<String> processedGroups = ContainerUtil.newHashSet();
    
    for (TemplateImpl template : TemplateSettings.getInstance().getTemplates()) {
      String groupName = template.getGroupName();
      if (processedGroups.add(groupName)) {
        processor.addOptions(groupName, null, groupName + " live template group", LiveTemplatesConfigurable.ID, null, false);
      }

      String key = template.getKey();
      String hit = key + " live template";

      processor.addOptions(key, null, hit, LiveTemplatesConfigurable.ID, null, false);
      
      String description = template.getDescription();
      if (description != null) {
        processor.addOptions(description, null, hit, LiveTemplatesConfigurable.ID, null, false);
      }
    }
  }
}
