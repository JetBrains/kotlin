// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.frameworkSupport

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import org.gradle.util.GradleVersion

class KotlinBuildScriptDataBuilder : BuildScriptDataBuilder {
  constructor(buildScriptFile: VirtualFile) : super(buildScriptFile)

  constructor(buildScriptFile: VirtualFile, gradleVersion: GradleVersion) : super(buildScriptFile, gradleVersion)

  override fun addPluginsLines(lines: MutableList<String>, padding: Function<String, String>) {
    if (plugins.isEmpty()) {
      return
    }
    lines.add("apply {")
    lines.addAll(plugins.map { padding.`fun`(it) })
    lines.add("}")
    lines.add("")
  }
}
