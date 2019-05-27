// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.RunnerAndConfigurationSettings;
import org.jdom.Element;

/**
* @author Dmitry Avdeev
*/
class TemplateConfigurable extends BaseRCSettingsConfigurable {
  private final RunnerAndConfigurationSettings myTemplate;

  TemplateConfigurable(RunnerAndConfigurationSettings template) {
    super(new ConfigurationSettingsEditorWrapper(template), template);
    myTemplate = template;
  }

  @Override
  void patchElementsIfNeeded(Element originalElement, Element snapshotElement) {
    snapshotElement.setAttribute(RunnerAndConfigurationSettingsImplKt.TEMPLATE_FLAG_ATTRIBUTE, "true");
  }

  @Override
  public String getDisplayName() {
    return myTemplate.getConfiguration().getName();
  }

  @Override
  public String getHelpTopic() {
    return null;
  }
}
