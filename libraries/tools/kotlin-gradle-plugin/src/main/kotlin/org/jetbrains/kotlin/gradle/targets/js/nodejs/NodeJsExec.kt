/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.tasks.AbstractExecTask

open class NodeJsExec : AbstractExecTask<NodeJsExec>(NodeJsExec::class.java) {
    init {
        val nodeJs = NodeJsPlugin.apply(project).root
        dependsOn(nodeJs.npmResolveTask)

        executable = nodeJs.environment.nodeExecutable
    }
}