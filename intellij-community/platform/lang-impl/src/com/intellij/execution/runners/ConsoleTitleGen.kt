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
package com.intellij.execution.runners

import com.intellij.execution.ExecutionHelper
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import java.util.stream.Collectors

/**
 * @author traff
 */
open class ConsoleTitleGen @JvmOverloads constructor(private val myProject: Project, private val consoleTitle: String, private val shouldAddNumberToTitle: Boolean = true) {

  fun makeTitle(): String {

    if (shouldAddNumberToTitle) {
      val activeConsoleNames = getActiveConsoles(consoleTitle)
      var max = 0
      for (name in activeConsoleNames) {
        if (max == 0) {
          max = 1
        }
        try {
          val num = Integer.parseInt(name.substring(consoleTitle.length + 1, name.length - 1))
          if (num > max) {
            max = num
          }
        }
        catch (ignored: Exception) {
          //skip
        }

      }
      if (max >= 1) {
        return consoleTitle + "(" + (max + 1) + ")"
      }
    }

    return consoleTitle
  }


  protected open fun getActiveConsoles(consoleTitle: String): List<String> {
    val consoles = ExecutionHelper.collectConsolesByDisplayName(myProject) { dom -> dom.contains(consoleTitle) }

    return consoles.filter({ input ->
      val handler = input.processHandler
      handler != null && !handler.isProcessTerminated
    }).map({ it.displayName });
  }
}
