// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.retype

import com.intellij.ide.util.propComponentProperty
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JRadioButton
import javax.swing.JTextField

/**
 * @author yole
 */

class RetypeOptions(val project: Project) {
  var retypeDelay: Int by propComponentProperty(project, 400)
  var threadDumpDelay: Int by propComponentProperty(project, 100)
  var enableLargeIndexing: Boolean by propComponentProperty(project, false)
  var largeIndexFilesCount: Int by propComponentProperty(project, 50_000)
  var recordScript: Boolean by propComponentProperty(project, true)
  var fileCount: Int by propComponentProperty(project, 10)
  var retypeExtension: String by propComponentProperty(project, "")
  var restoreOriginalText: Boolean by propComponentProperty(project, true)
  var retypeCurrentFile = false
}

class RetypeOptionsDialog(project: Project, private val retypeOptions: RetypeOptions, private val editor: Editor?) : DialogWrapper(project) {
  init {
    init()
    title = "Retype Options"
  }

  override fun createCenterPanel(): JComponent {
    retypeOptions.retypeCurrentFile = editor != null

    return panel {
      row(label = "Typing delay (ms):") {
        spinner(retypeOptions::retypeDelay, 0, 5000, 50)
      }
      row(label = "Thread dump capture delay (ms):") {
        spinner(retypeOptions::threadDumpDelay, 50, 5000, 50)
      }
      row {
        val c = checkBox("Create", retypeOptions::enableLargeIndexing).actsAsLabel()
        spinner(retypeOptions::largeIndexFilesCount, 100, 1_000_000, 1_000)
          .enableIf(c.selected)
        label("files to start background indexing")
      }
      buttonGroup(retypeOptions::retypeCurrentFile) {
        row {
          radioButton(if (editor?.selectionModel?.hasSelection() == true) "Retype selected text" else "Retype current file", true)
            .enabled(editor != null)
        }
        row {
          val r = radioButton("Retype", false)
          spinner(retypeOptions::fileCount, 1, 5000)
            .enableIf(r.selected)
          label("files with different sizes and extension")
          textField(retypeOptions::retypeExtension, 5)
            .enableIf(r.selected)
        }
      }
      row {
        checkBox("Record script for performance testing plugin", retypeOptions::recordScript)
      }
      row {
        checkBox("Restore original text after retype", retypeOptions::restoreOriginalText)
      }
    }
  }
}
