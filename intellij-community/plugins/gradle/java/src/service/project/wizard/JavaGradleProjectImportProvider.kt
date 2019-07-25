// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.service.project.open.canImportProjectFrom
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID

/**
 * Do not use this import provider directly.
 * @see JavaGradleProjectImportBuilder
 */
class JavaGradleProjectImportProvider : AbstractExternalProjectImportProvider(JavaGradleProjectImportBuilder(), SYSTEM_ID) {

  override fun createSteps(context: WizardContext): Array<ModuleWizardStep> {
    return ModuleWizardStep.EMPTY_ARRAY
  }

  override fun getPathToBeImported(file: VirtualFile): String = getDefaultPath(file)

  override fun canImportFromFile(file: VirtualFile) = canImportProjectFrom(file)

  override fun getFileSample() = "<b>Gradle</b> build script (${GradleConstants.BUILD_FILE_EXTENSIONS.joinToString { "*.$it" }})"
}