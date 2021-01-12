// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import java.util.*

interface ReaderModeListener : EventListener {
  fun modeChanged(project: Project)
}

@Topic.ProjectLevel
val READER_MODE_TOPIC = Topic(ReaderModeListener::class.java)

class ReaderModeSettingsListener : ReaderModeListener {
  override fun modeChanged(project: Project) {
    FileEditorManager.getInstance(project).allEditors.forEach { editor -> ReaderModeFileEditorListener.applyReaderMode(project, editor, true) }
  }
}