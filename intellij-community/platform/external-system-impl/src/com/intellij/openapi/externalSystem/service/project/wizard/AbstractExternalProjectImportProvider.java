// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.wizard;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectImportProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Provides 'import from external model' functionality.
 */
public abstract class AbstractExternalProjectImportProvider extends ProjectImportProvider {
  @NotNull
  private final ProjectSystemId myExternalSystemId;

  public AbstractExternalProjectImportProvider(ProjectImportBuilder builder, @NotNull ProjectSystemId externalSystemId) {
    super(builder);
    myExternalSystemId = externalSystemId;
  }

  public AbstractExternalProjectImportProvider(@NotNull ProjectSystemId externalSystemId) {
    myExternalSystemId = externalSystemId;
  }

  @NotNull
  public ProjectSystemId getExternalSystemId() {
    return myExternalSystemId;
  }

  @Override
  public ModuleWizardStep[] createSteps(WizardContext context) {
    return new ModuleWizardStep[] { new SelectExternalProjectStep(context) };
  }

  @Override
  public String getPathToBeImported(VirtualFile file) {
    return file.getPath();
  }
}
