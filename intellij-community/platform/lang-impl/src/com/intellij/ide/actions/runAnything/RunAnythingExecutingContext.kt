// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Paths
import javax.swing.Icon

abstract class RunAnythingContext(val presentation: ContextData) {
  object ProjectContext : RunAnythingContext(ContextData("Project"))

  data class ModuleContext(val module: Module) : RunAnythingContext(
    ContextData(module.name, module.moduleFile?.parent.toString(), AllIcons.Actions.ModuleDirectory))

  data class RecentDirectoryContext(val path: String) : RunAnythingContext(
    ContextData(FileUtil.getLocationRelativeToUserHome(Paths.get(path).toString()), icon = AllIcons.Nodes.Folder)
  )

  data class ContextData(val label: String, val description: String? = null, val icon: Icon? = null)
}