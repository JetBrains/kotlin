// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.bundles

import com.intellij.bootRuntime.command.CommandFactory.Type.*
import com.intellij.bootRuntime.command.CommandFactory.produce
import com.intellij.bootRuntime.command.Processor.process
import com.intellij.openapi.project.Project
import java.io.File

class Local(val project: Project, location: File) : Runtime(location) {

  override val installationPath: File = location

  val version : String by lazy {
    // runs on first access of messageView
    fetchVersion()
  }

  override fun install() {
    process(
      produce(EXTRACT, this),
      produce(COPY, this),
      produce(INSTALL, this)
    )
  }

  override fun toString(): String {
    return "$version [Local]"
  }
}