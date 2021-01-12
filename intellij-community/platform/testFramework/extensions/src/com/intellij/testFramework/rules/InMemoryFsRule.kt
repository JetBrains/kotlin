// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.rules

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.URLEncoder
import java.nio.file.FileSystem
import kotlin.properties.Delegates

class InMemoryFsRule(private val windows: Boolean = false) : ExternalResource() {
  private var _fs: FileSystem? = null
  private var sanitizedName: String by Delegates.notNull()

  override fun apply(base: Statement, description: Description): Statement {
    sanitizedName = URLEncoder.encode(description.methodName, Charsets.UTF_8.name())
    return super.apply(base, description)
  }

  val fs: FileSystem
    get() {
      if (_fs == null) {
        _fs = (if (windows) MemoryFileSystemBuilder.newWindows().setCurrentWorkingDirectory("C:\\")
               else MemoryFileSystemBuilder.newLinux().setCurrentWorkingDirectory("/")).build(sanitizedName)
      }
      return _fs!!
    }

  override fun after() {
    _fs?.close()
    _fs = null
  }
}