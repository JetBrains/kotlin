package org.jetbrains.plugins.gradle.ui;

import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory;
import org.jetbrains.plugins.gradle.util.GradleConstants;

public class GradleToolWindowFactory extends AbstractExternalSystemToolWindowFactory {
  public GradleToolWindowFactory() {
    super(GradleConstants.SYSTEM_ID);
  }
}
