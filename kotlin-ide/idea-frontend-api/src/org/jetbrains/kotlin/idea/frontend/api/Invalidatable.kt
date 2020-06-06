/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService

interface Invalidatable {
    fun isValid(): Boolean

    fun invalidationReason(): String

    fun assertIsValid() {
        assert(isValid()) { "Access to invalid $this, invalidation reason is ${invalidationReason()}" }
    }
}

class ReadActionConfinementValidityToken(project: Project) : Invalidatable {
    private val modificationTracker = KotlinModificationTrackerService.getInstance(project).modificationTracker
    private val ownerThread = Thread.currentThread()
    private val onCreatedTimeStamp = modificationTracker.modificationCount


    override fun isValid(): Boolean {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) return false
        if (!application.isReadAccessAllowed) return false
        if (ownerThread != Thread.currentThread()) return false
        return onCreatedTimeStamp == modificationTracker.modificationCount
    }

    override fun invalidationReason(): String {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) return "Called in EDT thread"
        if (!application.isReadAccessAllowed) return "Called outside read action"
        if (ownerThread != Thread.currentThread()) {
            return "Called outside the thread was created in (was created in $ownerThread, but accessed in ${Thread.currentThread()}"
        }
        if (onCreatedTimeStamp != modificationTracker.modificationCount) return "PSI has changed since creation"
        error("Getting invalidation reason for valid invalidatable")
    }
}