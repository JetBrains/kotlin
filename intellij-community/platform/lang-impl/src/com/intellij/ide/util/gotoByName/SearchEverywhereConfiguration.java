// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;

/**
 * Configuration for contributors filter in "Search Everywhere" popup.
 */
@State(name = "SearchEverywhereConfiguration", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class SearchEverywhereConfiguration extends ChooseByNameFilterConfiguration<String>  {

  public static SearchEverywhereConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, SearchEverywhereConfiguration.class);
  }

  @Override
  protected String nameForElement(String type) {
    return type;
  }
}
