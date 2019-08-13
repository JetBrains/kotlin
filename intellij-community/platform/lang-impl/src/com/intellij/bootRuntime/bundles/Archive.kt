// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.bundles

import com.intellij.bootRuntime.command.CommandFactory.Type.*
import com.intellij.bootRuntime.command.CommandFactory.produce
import com.intellij.bootRuntime.command.Processor.process
import com.intellij.openapi.project.Project
import java.io.File

class Archive(val project: Project, initialLocation: File) : Runtime(initialLocation) {

  override fun install() {
    process(
      produce(EXTRACT, this),
      produce(COPY, this),
      produce(INSTALL, this)
    )
  }
}