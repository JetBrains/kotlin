/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.projectView.actions

import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager

/**
 * @author nik
 */
class MarkAsContentRootAction : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
    val module = MarkRootActionBase.getModule(e, files)
    if (module == null || files == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val fileIndex = ProjectRootManager.getInstance(module.project).fileIndex
    e.presentation.isEnabledAndVisible = files.all {
      it.isDirectory && fileIndex.isExcluded(it) && ProjectRootsUtil.findExcludeFolder(module, it) == null
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
    val module = MarkRootActionBase.getModule(e, files) ?: return
    val model = ModuleRootManager.getInstance(module).modifiableModel
    files.forEach {
      model.addContentEntry(it)
    }
    MarkRootActionBase.commitModel(module, model)
  }
}
