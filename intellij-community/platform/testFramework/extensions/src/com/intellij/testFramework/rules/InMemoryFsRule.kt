/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.testFramework.rules

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.URLEncoder
import java.nio.file.FileSystem
import kotlin.properties.Delegates

class InMemoryFsRule : ExternalResource() {
  private var _fs: FileSystem? = null

  private var sanitizedName: String by Delegates.notNull()

  override fun apply(base: Statement, description: Description): Statement {
    sanitizedName = URLEncoder.encode(description.methodName, Charsets.UTF_8.name())
    return super.apply(base, description)
  }

  val fs: FileSystem
    get() {
      var r = _fs
      if (r == null) {
        r = MemoryFileSystemBuilder
          .newLinux()
          .setCurrentWorkingDirectory("/")
          .build(sanitizedName)
        _fs = r
      }
      return r!!
    }

  override fun after() {
    _fs?.close()
    _fs = null
  }
}