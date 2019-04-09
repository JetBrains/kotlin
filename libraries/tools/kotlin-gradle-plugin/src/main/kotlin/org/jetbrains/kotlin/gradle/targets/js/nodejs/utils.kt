/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project

internal fun nodeJsSetupWithoutTasks(project: Project) {
    val nodeJsProject = NodeJsPlugin.ensureAppliedInHierarchy(project)
    val nodeJs = NodeJsExtension[nodeJsProject]
    val nodeJsEnv = nodeJs.buildEnv()
    if (nodeJs.download) {
        if (!nodeJsEnv.nodeBinDir.isDirectory) {
            (nodeJsProject.tasks.findByName(NodeJsSetupTask.NAME) as NodeJsSetupTask).exec()
        }
    }
}