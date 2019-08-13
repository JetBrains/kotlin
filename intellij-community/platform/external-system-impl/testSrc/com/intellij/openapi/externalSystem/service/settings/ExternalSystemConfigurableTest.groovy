// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings

import com.intellij.openapi.externalSystem.test.AbstractExternalSystemTest

class ExternalSystemConfigurableTest extends AbstractExternalSystemTest {

  @SuppressWarnings("GroovyAssignabilityCheck")
  void 'test es settings controls reset'() {
    setupExternalProject { 'project' {} }
    def configurable = new TestExternalSystemConfigurable(project)
    configurable.createComponent()
    configurable.reset()
    def control = configurable.projectSettingsControls.first() as TestExternalProjectSettingsControl
    assertNotNull(control.project)
  }
}
