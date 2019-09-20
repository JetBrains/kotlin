// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

@Deprecated
@State(
  name = "ExportableTemplateSettings",
  storages = @Storage(value = "template.settings.xml", roamingType = RoamingType.DISABLED)
)
final class ExportableTemplateSettings implements PersistentStateComponent<ExportableTemplateSettings> {
  public Collection<TemplateSettings.TemplateKey> deletedKeys = new SmartList<>();

  @Nullable
  @Override
  public ExportableTemplateSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull ExportableTemplateSettings state) {
    TemplateSettings templateSettings = TemplateSettings.getInstance();
    List<TemplateSettings.TemplateKey> deletedTemplates = templateSettings.getDeletedTemplates();
    deletedTemplates.clear();
    deletedTemplates.addAll(state.deletedKeys);
    templateSettings.applyNewDeletedTemplates();
  }
}
