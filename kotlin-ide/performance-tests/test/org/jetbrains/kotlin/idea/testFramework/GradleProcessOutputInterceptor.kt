/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener.EP_NAME as EP
import com.intellij.testFramework.ExtensionTestUtil.maskExtensions
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.perf.util.gradleMessage
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.lang.Exception
import kotlin.test.assertNull

interface GradleProcessOutputInterceptor {
    companion object {
        fun getInstance(): GradleProcessOutputInterceptor? = EP.extensions.firstIsInstanceOrNull()

        fun install(parentDisposable: Disposable) {
            val installedExtensions = EP.extensions

            assertNull(
                installedExtensions.firstIsInstanceOrNull<GradleProcessOutputInterceptor>(),
                "Another ${GradleProcessOutputInterceptor::class.java.simpleName} is already installed"
            )

            maskExtensions(
                EP,
                listOf(GradleProcessOutputInterceptorImpl()) + installedExtensions,
                parentDisposable
            )
        }
    }

    fun reset()
    fun getOutput(): String
}

private class GradleProcessOutputInterceptorImpl : GradleProcessOutputInterceptor, ExternalSystemTaskNotificationListener {
    private val buffer = StringBuilder()

    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        if (id.projectSystemId == GRADLE_SYSTEM_ID && text.isNotEmpty()) {
            gradleMessage { text }
            buffer.append(text)
        }
    }

    override fun reset() = buffer.setLength(0)
    override fun getOutput() = buffer.toString()

    override fun onSuccess(id: ExternalSystemTaskId) = Unit
    override fun onFailure(id: ExternalSystemTaskId, e: Exception) = Unit
    override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) = Unit
    override fun onCancel(id: ExternalSystemTaskId) = Unit
    override fun onEnd(id: ExternalSystemTaskId) = Unit
    override fun beforeCancel(id: ExternalSystemTaskId) = Unit

    @Suppress("UnstableApiUsage")
    override fun onStart(id: ExternalSystemTaskId) = Unit
}
