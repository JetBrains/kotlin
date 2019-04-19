package com.intellij.openapi.externalSystem.service.project.wizard;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Handles the following responsibilities:
 * <pre>
 * <ul>
 *   <li>allows end user to define external system config file to import from;</li>
 *   <li>processes the input and reacts accordingly - shows error message if the project is invalid or proceeds to the next screen;</li>
 * </ul>
 * </pre>
 *
 * @author Denis Zhdanov
 */
public class SelectExternalProjectStep extends AbstractImportFromExternalSystemWizardStep {

  private final JPanel myComponent = new JPanel(new BorderLayout());

  @NotNull private AbstractImportFromExternalSystemControl myControl;

  private boolean myExternalSystemSettingsInitialised;

  public SelectExternalProjectStep(@NotNull WizardContext context) {
    super(context);
  }

  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  @Override
  public void updateStep() {
    if (!myExternalSystemSettingsInitialised) {
      initExternalProjectSettingsControl();
    }
  }

  @Override
  public void updateDataModel() {
  }

  // TODO den uncomment
  //@Override
  //public String getHelpId() {
  //  return GradleConstants.HELP_TOPIC_IMPORT_SELECT_PROJECT_STEP;
  //}

  @Override
  public boolean validate() throws ConfigurationException {
    WizardContext wizardContext = getWizardContext();
    boolean isDefaultFormat = true;
    if (myControl.getProjectFormatPanel() != null) {
      isDefaultFormat = myControl.getProjectFormatPanel().isDefault();
    }
    if (!myControl.validate(wizardContext, isDefaultFormat)) return false;
    AbstractExternalProjectImportBuilder builder = getBuilder();
    if (builder == null) {
      return false;
    }
    myControl.apply();
    if (myControl.getProjectFormatPanel() != null) {
      myControl.getProjectFormatPanel().updateData(wizardContext);
    }
    builder.ensureProjectIsDefined(wizardContext);
    return true;
  }

  private void initExternalProjectSettingsControl() {
    AbstractExternalProjectImportBuilder builder = getBuilder();
    if (builder == null) {
      return;
    }
    builder.prepare(getWizardContext());
    myControl = builder.getControl(getWizardContext().getProject());
    myComponent.add(myControl.getComponent());
    myExternalSystemSettingsInitialised = true;
  }
}
