// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile

data class ModuleIndexableRootsProvider(val module: Module) : IndexableRootsProvider {
  override val presentableName
    get() = "Roots of module ${module.name}"

  override fun getRootsToIndex(): Set<VirtualFile> = runReadAction {
    ModuleRootManager.getInstance(module).fileIndex.moduleRootsToIterate
  }

}