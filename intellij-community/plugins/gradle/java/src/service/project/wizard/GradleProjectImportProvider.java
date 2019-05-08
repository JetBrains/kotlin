// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard;

import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * @deprecated Use {@link JavaGradleProjectImportProvider} instead
 */
@Deprecated
public final class GradleProjectImportProvider extends AbstractExternalProjectImportProvider {
  public GradleProjectImportProvider() {
    super(GradleConstants.SYSTEM_ID);
  }

  public GradleProjectImportProvider(@NotNull GradleProjectImportBuilder builder) {
    super(builder, GradleConstants.SYSTEM_ID);
  }

  @Override
  protected ProjectImportBuilder doGetBuilder() {
    return GradleProjectImportBuilder.getInstance();
  }

  @Override
  protected boolean canImportFromFile(VirtualFile file) {
    return GradleConstants.EXTENSION.equals(file.getExtension()) ||
      file.getName().endsWith("." + GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION);
  }

  @NotNull
  @Override
  public String getFileSample() {
    return "<b>Gradle</b> build script (*.gradle)";
  }
}
