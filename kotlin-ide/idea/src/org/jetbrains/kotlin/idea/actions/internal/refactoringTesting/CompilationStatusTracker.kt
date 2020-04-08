/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilationStatusListener
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompilerTopics.COMPILATION_STATUS
import com.intellij.openapi.project.Project

internal class CompilationStatusTracker(private val project: Project) {

    private val compilationStatusListener = object : CompilationStatusListener {

        init {
            project.messageBus.connect(project).subscribe(COMPILATION_STATUS, this)
        }

        var hasCompilerError = false
            private set

        var compilationFinished = false
            private set

        fun reset() {
            compilationFinished = false
            hasCompilerError = false
        }

        override fun compilationFinished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
            hasCompilerError = hasCompilerError || aborted || errors != 0
            compilationFinished = true
        }
    }

    private val runBuildAction by lazyPub {
        val am = ActionManager.getInstance();
        val action = am.getAction("CompileProject")

        val event = AnActionEvent(
            null,
            DataManager.getInstance().dataContext,
            ActionPlaces.UNKNOWN, Presentation(),
            ActionManager.getInstance(), 0
        )

        return@lazyPub { action.actionPerformed(event) }
    }

    fun checkByBuild(cancelledChecker: () -> Boolean): Boolean {

        compilationStatusListener.reset()

        ApplicationManager.getApplication().invokeAndWait {
            runBuildAction()
        }

        while (!cancelledChecker() && !compilationStatusListener.compilationFinished) {
            Thread.yield()
        }

        return !compilationStatusListener.hasCompilerError
    }
}