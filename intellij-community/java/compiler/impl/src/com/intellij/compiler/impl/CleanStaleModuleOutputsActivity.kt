// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl

import com.intellij.CommonBundle
import com.intellij.compiler.server.BuildManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.compiler.CompilerBundle
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.CompilerProjectExtension
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Shows notification which suggests to delete stale output directories corresponding to renamed or deleted modules. Such directories may
 * be kept on disk because of a bug (https://youtrack.jetbrains.com/issue/IDEA-185574) in previous IntelliJ IDEA versions.
 */
class CleanStaleModuleOutputsActivity : StartupActivity, DumbAware {
  override fun runActivity(project: Project) {
    val markerFile = File(BuildManager.getInstance().getProjectSystemDirectory(project), "stale_outputs_checked")
    if (markerFile.exists()) return

    fun createMarker() {
      FileUtil.createIfDoesntExist(markerFile)
    }

    val staleOutputs = runReadAction { collectStaleOutputs(project) }
    if (staleOutputs.isEmpty()) {
      createMarker()
      return
    }

    runReadAction {
      val outputPath = CompilerProjectExtension.getInstance(project)!!.compilerOutput!!.presentableUrl
      val notification = Notification(
        "Build", CompilerBundle.message("notification.title.delete.old.output.directories"),
        CompilerBundle.message("notification.content.delete.old.output.directories", staleOutputs.size, outputPath),
        NotificationType.INFORMATION
      ).addAction(object : NotificationAction(CompilerBundle.message("notification.action.text.cleanup")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          notification.expire()
          runCleanup(staleOutputs, project, ::createMarker)
        }
      }).addAction(object : NotificationAction(CompilerBundle.message("notification.action.text.do.not.ask")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          notification.expire()
          createMarker()
        }
      })
      Notifications.Bus.notify(notification, project)
    }
  }

  private fun runCleanup(outputs: List<VirtualFile>, project: Project, onSuccess: () -> Unit) {
    val outputsString: String
    val threshold = 50
    if (outputs.size <= threshold + 2) {
      outputsString = outputs.joinToString("<br>") { it.presentableUrl }
    }
    else {
      val parents = outputs.subList(threshold, outputs.size).mapTo(LinkedHashSet()) {it.parent}.toList()
      outputsString = (outputs.subList(0, threshold).map {it.presentableUrl}
                       + listOf("${outputs.size - threshold} more directories under ${parents.first().presentableUrl}")
                       + parents.drop(1).map { "and ${it.presentableUrl}" }
                      ).joinToString("<br>")
    }

    //until IDEA-186296 is fixed we need to use IDEA's message dialog for potentially long messages
    val answer = Messages.showIdeaMessageDialog(project, CompilerBundle.message("dialog.text.delete.old.outputs", outputs.size, outputsString),
                                             CompilerBundle.message("dialog.title.delete.old.outputs"),
                                             arrayOf(CompilerBundle.message("button.text.delete.old.outputs"), CommonBundle.getCancelButtonText()), 0,null, null)
    if (answer == Messages.CANCEL) return

    val filesToDelete = outputs.map { VfsUtil.virtualToIoFile(it) }
    object : Task.Backgroundable(project, CompilerBundle.message("dialog.title.delete.old.outputs")) {
      override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        filesToDelete.forEachIndexed { i, file ->
          indicator.checkCanceled()
          indicator.fraction = i.toDouble() / filesToDelete.size
          indicator.text = CompilerBundle.message("progress.text.deleting.directory", file.absolutePath)
          FileUtil.delete(file)
        }
        onSuccess()
        indicator.text = CompilerBundle.message("progress.text.synchronizing.output.directories")
        LocalFileSystem.getInstance().refreshIoFiles(filesToDelete, true, false, null)
      }
    }.queue()
  }

  private fun collectStaleOutputs(project: Project): List<VirtualFile> {
    val projectOutput = CompilerProjectExtension.getInstance(project)?.compilerOutput
    if (projectOutput == null) return emptyList()

    val outputsOnDisk = listOf(CompilerModuleExtension.PRODUCTION, CompilerModuleExtension.TEST)
                            .flatMap { projectOutput.findChild(it)?.children?.asIterable() ?: emptyList() }

    val currentOutputs = ModuleManager.getInstance(project).modules.flatMap {
      val extension = CompilerModuleExtension.getInstance(it)
      listOfNotNull(extension?.compilerOutputPath, extension?.compilerOutputPathForTests)
    }

    return outputsOnDisk - currentOutputs
  }
}