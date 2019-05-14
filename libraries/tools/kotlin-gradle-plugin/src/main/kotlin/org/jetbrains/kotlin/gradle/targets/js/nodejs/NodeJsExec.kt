/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.io.File

open class NodeJsExec : AbstractExecTask<NodeJsExec>(NodeJsExec::class.java) {
    private val npmProject by lazy { project.npmProject }

    init {
        val nodeJs = NodeJsPlugin.apply(project).root
        dependsOn(nodeJs.nodeJsSetupTask)

        executable = nodeJs.environment.nodeExecutable
        workingDir = project.npmProject.nodeWorkDir
    }

    @Internal
    var script: String? = null

    fun require(moduleName: String) {
        args("--require", moduleName)
    }

    fun script(file: File) {
        check(script == null) {
            "Script was already set to $script"
        }
        script = file.canonicalFile.relativeToOrSelf(npmProject.nodeWorkDir).path
    }
}