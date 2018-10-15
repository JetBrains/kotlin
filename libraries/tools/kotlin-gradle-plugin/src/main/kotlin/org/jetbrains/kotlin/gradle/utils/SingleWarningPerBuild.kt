/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import java.util.*

internal object SingleWarningPerBuild {
    private val rootModuleReceivedWarning = WeakHashMap<Project, MutableSet<String>>()

    fun show(project: Project, warningText: String) {
        val receivedWarnings = rootModuleReceivedWarning.computeIfAbsent(project.rootProject) { mutableSetOf() }
        if (receivedWarnings.add(warningText)) {
            project.logger.warn(warningText)
        }
    }
}