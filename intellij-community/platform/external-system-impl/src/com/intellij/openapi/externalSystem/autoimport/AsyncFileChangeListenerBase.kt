// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.intellij.openapi.vfs.newvfs.events.*

abstract class AsyncFileChangeListenerBase : AsyncFileListener {

  protected open val processRecursively: Boolean = true

  protected abstract fun init()

  protected abstract fun apply()

  protected abstract fun isRelevant(path: String): Boolean

  protected abstract fun updateFile(file: VirtualFile, event: VFileEvent)

  override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier {
    val separator = ChangeSeparator()
    separator.processChangeEvents(events)
    return object : AsyncFileListener.ChangeApplier {
      override fun beforeVfsChange() {
        init()
        separator.applyBefore()
      }

      override fun afterVfsChange() {
        separator.applyAfter()
        apply()
      }
    }
  }

  private fun process(f: VirtualFile, event: VFileEvent) {
    when (processRecursively) {
      true -> processRecursively(f, event)
      else -> processFile(f, event)
    }
  }

  private fun processFile(f: VirtualFile, event: VFileEvent) {
    if (isRelevant(f.path)) {
      updateFile(f, event)
    }
  }

  private fun processRecursively(f: VirtualFile, event: VFileEvent) {
    VfsUtilCore.visitChildrenRecursively(f, object : VirtualFileVisitor<Void>() {
      override fun visitFile(f: VirtualFile): Boolean {
        if (isRelevant(f.path)) {
          updateFile(f, event)
        }
        return true
      }

      override fun getChildrenIterable(f: VirtualFile): Iterable<VirtualFile>? {
        return if (f.isDirectory && f is NewVirtualFile) f.iterInDbChildren() else null
      }
    })
  }

  private fun ChangeSeparator.processChangeEvents(events: List<VFileEvent>) {
    for (each in events) {
      ProgressManager.checkCanceled()

      when (each) {
        is VFilePropertyChangeEvent -> if (each.isRename) {
          val oldFile = each.file
          val parent = oldFile.parent
          before {
            process(oldFile, each)
          }
          after {
            val newName = each.newValue as String
            val newFile = parent?.findChild(newName)
            if (newFile != null) process(newFile, each)
          }
        }
        is VFileMoveEvent -> {
          val oldFile = each.file
          val name = oldFile.name
          before {
            process(oldFile, each)
          }
          after {
            val newFile = each.newParent.findChild(name)
            if (newFile != null) process(newFile, each)
          }
        }
        is VFileCopyEvent -> after {
          val newFile = each.newParent.findChild(each.newChildName)
          if (newFile != null) process(newFile, each)
        }
        is VFileCreateEvent -> after {
          val file = each.file
          if (file != null) process(file, each)
        }
        is VFileDeleteEvent, is VFileContentChangeEvent -> before {
          val file = each.file
          if (file != null) process(file, each)
        }
      }
    }
  }

  private class ChangeSeparator {
    private val beforeAppliers = ArrayList<() -> Unit>()
    private val afterAppliers = ArrayList<() -> Unit>()

    fun before(action: () -> Unit) = beforeAppliers.add(action)

    fun after(action: () -> Unit) = afterAppliers.add(action)

    fun applyBefore() = beforeAppliers.forEach { it() }

    fun applyAfter() = afterAppliers.forEach { it() }
  }
}
