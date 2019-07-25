// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.ide.ui.OptionsTopHitProvider;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey.Malenkov
 */
final class LiveTemplatesOptionsTopHitProvider implements OptionsTopHitProvider.ApplicationLevelProvider {
  @NotNull
  @Override
  public String getId() {
    return "templates";
  }

  @NotNull
  @Override
  public Collection<OptionDescription> getOptions() {
    TemplateSettings settings = TemplateSettings.getInstance();
    if (settings == null) {
      return Collections.emptyList();
    }
    Collection<BooleanOptionDescription> options = new ArrayList<>();
    for (TemplateGroup group : settings.getTemplateGroups()) {
      for (final TemplateImpl element : group.getElements()) {
        options.add(new Option(element));
      }
    }
    return Collections.unmodifiableCollection(options);
  }

  private static final class Option extends BooleanOptionDescription {
    private final TemplateImpl myElement;

    private Option(TemplateImpl element) {
      super(getOptionName(element), LiveTemplatesConfigurable.ID);
      myElement = element;
    }

    @Override
    public boolean isOptionEnabled() {
      return !myElement.isDeactivated();
    }

    @Override
    public void setOptionState(boolean enabled) {
      myElement.setDeactivated(!enabled);
    }

    private static String getOptionName(TemplateImpl element) {
      StringBuilder sb = new StringBuilder().append(element.getGroupName()).append(": ").append(element.getKey());
      String description = element.getDescription();
      if (description != null) {
        sb.append(" (").append(description).append(")");
      }
      return sb.toString();
    }
  }
}
