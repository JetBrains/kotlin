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
import com.intellij.openapi.project.Project

/**
 * @author traff
 */
open class ConsoleTitleGen @JvmOverloads constructor(private val myProject: Project,
                                                     private val consoleTitle: String,
                                                     private val shouldAddNumberToTitle: Boolean = true) {

  fun makeTitle(): String {

    if (shouldAddNumberToTitle) {
      val activeConsoleNames = getActiveConsoles(consoleTitle)
      var max = -1
      for (name in activeConsoleNames) {
        try {
          val numBegin = name.lastIndexOf("(")
          if (numBegin != -1) {
            val numString = name.substring(numBegin + 1, name.length - 1)
            val num = Integer.parseInt(numString)
            if (num > max) {
              max = num
            }
          }
          else {
            max = 0
          }
        }
        catch (ignored: Exception) {
          //skip
        }
      }
      return when (max) {
        -1 -> consoleTitle
        else -> "$consoleTitle (${max + 1})"
      }
    }

    return consoleTitle
  }


  protected open fun getActiveConsoles(consoleTitle: String) =
    ExecutionHelper.collectConsolesByDisplayName(myProject) { dom -> dom.startsWith(consoleTitle) }
      .filter { it.processHandler?.isProcessTerminated == false }
      .map { it.displayName }
}
