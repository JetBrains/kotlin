/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile

/**
 * @author Vladislav.Soroka
 */
class GradleEditorTabTitleProvider : EditorTabTitleProvider {
  override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
    if (!GradleConstants.KNOWN_GRADLE_FILES.contains(file.name)) return null

    val fileParent = file.parent ?: return null

    if (!UniqueVFilePathBuilder.getInstance().hasFilesWithSameName(project, file)) return null

    val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(file) ?: return null
    val manager = ExternalSystemModulePropertyManager.getInstance(module)

    if (manager.getExternalSystemId() != GradleConstants.SYSTEM_ID.id) return null

    val projectPath = manager.getLinkedProjectPath() ?: return null

    if (FileUtil.pathsEqual(projectPath, fileParent.path)) {
      return "${file.name} (${manager.getLinkedProjectId()})"
    }

    return null
  }
}