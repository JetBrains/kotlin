// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import javax.swing.Icon

sealed class RunAnythingContext(val label: String, var description: String = "", val icon: Icon? = null) {
  data class ProjectContext(val project: Project) :
    RunAnythingContext(IdeBundle.message("run.anything.context.project"), project.basePath.orEmpty())

  data class ModuleContext(val module: Module) :
    RunAnythingContext(module.name,
                       module.project.guessProjectDir()?.let { project ->
                         FileUtil.getRelativePath(project.path, ModuleRootManager.getInstance(
                           module).contentRoots.let { if (it.size == 1) it[0].path else ModuleUtilCore.getModuleDirPath(module) }, '/')
                       } ?: "undefined",
                       AllIcons.Nodes.Module)

  object BrowseRecentDirectoryContext :
    RunAnythingContext(IdeBundle.message("run.anything.context.browse.directory"), icon = AllIcons.Nodes.Folder)

  data class RecentDirectoryContext(val path: String) :
    RunAnythingContext(FileUtil.getLocationRelativeToUserHome(path), icon = AllIcons.Nodes.Folder)
}