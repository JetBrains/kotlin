// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dump.paths

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.roots.ContentIterator
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexImpl

internal class DumpProjectPortableRootsAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    object: Task.Modal(project, "Collecting project roots...", true) {
      override fun run(indicator: ProgressIndicator) {
        val fileBasedIndex = FileBasedIndex.getInstance() as FileBasedIndexImpl
        val providers = fileBasedIndex.getOrderedIndexableFilesProviders(project)

        val result = PortableFilesDumpCollector(project)
        for (provider in providers) {
          indicator.text2 = provider.rootsScanningProgressText
          provider.iterateFiles(project, ContentIterator {
            result.addFile(it)
            true
          }, ConcurrentBitSet())
        }

        val text = result.serializeToText()
        ApplicationManager.getApplication().invokeLater(Runnable {
          val file = ScratchRootType.getInstance().createScratchFile(
            project,
            "project-files-dump.txt",
            Language.findInstancesByMimeType("text/plain").firstOrNull() ?: Language.ANY,
            text,
            ScratchFileService.Option.create_new_always
          ) ?: return@Runnable

          PsiNavigationSupport.getInstance().createNavigatable(project, file, 0).navigate(true)
        }, project.disposed)
      }
    }.queue()
  }
}
