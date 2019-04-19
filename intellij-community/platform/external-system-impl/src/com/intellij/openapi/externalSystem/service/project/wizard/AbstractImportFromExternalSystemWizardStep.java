package com.intellij.openapi.externalSystem.service.project.wizard;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.projectImport.ProjectImportWizardStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Just a holder for the common useful functionality.
 * 
 * @author Denis Zhdanov
 */
public abstract class AbstractImportFromExternalSystemWizardStep extends ProjectImportWizardStep {

  protected AbstractImportFromExternalSystemWizardStep(@NotNull WizardContext context) {
    super(context);
  }

  @Override
  @Nullable
  protected AbstractExternalProjectImportBuilder getBuilder() {
    return (AbstractExternalProjectImportBuilder)getWizardContext().getProjectBuilder();
  }
}
