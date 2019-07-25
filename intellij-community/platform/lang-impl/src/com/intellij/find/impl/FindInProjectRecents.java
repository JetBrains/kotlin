// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl;

import com.intellij.find.FindInProjectSettings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;

@State(
  name = "FindInProjectRecents",
  storages = {
    @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE),
    @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
  })
final class FindInProjectRecents extends FindInProjectSettingsBase implements FindInProjectSettings {
  public static FindInProjectSettings getInstance(Project project) {
    return ServiceManager.getService(project, FindInProjectSettings.class);
  }
}
