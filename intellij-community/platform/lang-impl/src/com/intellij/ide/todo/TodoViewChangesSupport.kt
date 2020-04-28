// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.todo

import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
open class TodoViewChangesSupport {

  open fun isContentVisible(project: Project) : Boolean {
    return false
  }

  open fun getTabName(project: Project) : String {
    return "Changes"
  }

  open fun installListener(project: Project,
                           connection: MessageBusConnection,
                           contentManagerFunc: () -> ContentManager?,
                           contentFunc: () -> Content): Listener {
    return object : Listener {
      override fun setVisible(value: Boolean) {
      }
    }
  }

  open fun createPanel(project: Project, settings: TodoPanelSettings, content: Content, factory: TodoTreeBuilderFactory) : TodoPanel? {
    return null
  }

  open fun createPanel(project: Project, settings: TodoPanelSettings, content: Content) : TodoPanel? {
    return null
  }

  interface Listener {
    fun setVisible(value: Boolean)
  }
}