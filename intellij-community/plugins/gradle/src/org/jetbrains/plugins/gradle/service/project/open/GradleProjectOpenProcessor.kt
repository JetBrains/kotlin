// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.open

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import icons.GradleIcons
import org.jetbrains.plugins.gradle.util.GradleBundle
import javax.swing.Icon

class GradleProjectOpenProcessor : ProjectOpenProcessor() {
  override fun getName(): String =
    GradleBundle.message("gradle.name")

  override fun getIcon(): Icon? =
    GradleIcons.Gradle

  override fun canOpenProject(file: VirtualFile): Boolean =
    canOpenGradleProject(file)

  override fun doOpenProject(projectFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
    return openGradleProject(projectFile, projectToClose, forceOpenInNewFrame)
  }
}
