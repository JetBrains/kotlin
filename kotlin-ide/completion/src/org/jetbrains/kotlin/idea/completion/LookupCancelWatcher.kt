/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class LookupCancelWatcher : StartupActivity {
    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect()
        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorReleased(event: EditorFactoryEvent) {
                    LookupCancelService.getServiceIfCreated(project)?.disposeLastReminiscence(event.editor)
                }
            },
            connection
        )

        connection.subscribe(LookupManagerListener.TOPIC, LookupManagerListener { _, newLookup ->
            if (newLookup == null) return@LookupManagerListener
            newLookup.addLookupListener(LookupCancelService.getInstance(newLookup.project).lookupCancelListener)
        })
    }
}
