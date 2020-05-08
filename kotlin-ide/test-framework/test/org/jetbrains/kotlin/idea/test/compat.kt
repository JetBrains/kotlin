/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.codeInsight.daemon.impl.EditorTracker
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager

// BUNCH: 192
fun editorTrackerProjectOpened(project: Project) {
    EditorTracker.getInstance(project)
}

// BUNCH: 193
fun runPostStartupActivitiesOnce(project: Project) {
    (StartupManager.getInstance(project) as StartupManagerImpl).runPostStartupActivitiesRegisteredDynamically()
}
