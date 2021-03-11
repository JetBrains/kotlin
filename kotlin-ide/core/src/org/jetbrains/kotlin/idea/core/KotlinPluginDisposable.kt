package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class KotlinPluginDisposable : Disposable {
    companion object {
        fun getInstance(project: Project): Disposable {
            return project.getService(KotlinPluginDisposable::class.java)
        }
    }

    override fun dispose() {}
}