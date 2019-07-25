// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.ide.ui.search.SearchableOptionContributor;
import com.intellij.ide.ui.search.SearchableOptionProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 */
public class TemplateSearchableOptionContributor extends SearchableOptionContributor {
  @Override
  public void processOptions(@NotNull SearchableOptionProcessor processor) {
    Set<String> processedGroups = new HashSet<>();

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
