// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor;

import com.intellij.openapi.extensions.ProjectExtensionPointName;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Register implementation of {@link AutoImportOptionsProvider} in the plugin.xml to provide additional options in Editor | Auto Import section:
 * <p/>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;autoImportOptionsProvider instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 */
public class AutoImportOptionsProviderEP extends ConfigurableEP<AutoImportOptionsProvider> {
  public static final ProjectExtensionPointName<AutoImportOptionsProviderEP> EP_NAME = new ProjectExtensionPointName<>("com.intellij.autoImportOptionsProvider");

  public AutoImportOptionsProviderEP(@NotNull Project project) {
    super(project);
  }
}
