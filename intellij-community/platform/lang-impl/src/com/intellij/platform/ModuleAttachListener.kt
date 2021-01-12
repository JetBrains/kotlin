// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform

import com.intellij.openapi.module.Module
import com.intellij.util.messages.Topic
import java.nio.file.Path

interface ModuleAttachListener {

  companion object {
    @JvmField
    val TOPIC = Topic("attach or detach module changes", ModuleAttachListener::class.java)
  }

  fun afterAttach(module: Module, primaryModule: Module?, imlFile: Path) {}
  fun beforeDetach(module: Module) {}
}