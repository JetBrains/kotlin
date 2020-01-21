// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.runners

import com.intellij.execution.ExecutionHelper
import com.intellij.openapi.project.Project

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
