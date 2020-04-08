/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.gradle.execution

import com.intellij.openapi.module.Module
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings

// BUNCH: 191
fun isDelegatedBuild(module: Module): Boolean {
    val projectUrl = module.project.presentableUrl
    if (projectUrl == null || !GradleProjectSettings.isDelegatedBuildEnabled(module)) {
        return false
    }
    return true
}