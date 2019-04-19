/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.openapi.externalSystem.test

import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull

class TestExternalSystemLocalSettings extends AbstractExternalSystemLocalSettings<AbstractExternalSystemLocalSettings.State> {
  TestExternalSystemLocalSettings(@NotNull Project project) {
    super(ExternalSystemTestUtil.TEST_EXTERNAL_SYSTEM_ID, project, new AbstractExternalSystemLocalSettings.State())
  }
}
