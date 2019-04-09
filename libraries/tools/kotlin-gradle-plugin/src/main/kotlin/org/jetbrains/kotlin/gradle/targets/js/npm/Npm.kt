/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJsSetupWithoutTasks

object Npm: NpmApi {
    override fun setup(project: Project) {
        nodeJsSetupWithoutTasks(project)
    }

    override fun resolveRootProject(project: Project) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}