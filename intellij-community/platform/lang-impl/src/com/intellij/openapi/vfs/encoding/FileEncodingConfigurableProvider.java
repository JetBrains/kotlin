// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.encoding;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;

public final class FileEncodingConfigurableProvider extends ConfigurableProvider {
  private final Project myProject;

  public FileEncodingConfigurableProvider(Project project) {
    myProject = project;
  }

  @Override
  public Configurable createConfigurable() {
    return new FileEncodingConfigurable(myProject);
  }
}
