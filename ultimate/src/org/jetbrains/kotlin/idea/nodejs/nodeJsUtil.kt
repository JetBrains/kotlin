/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.nodejs

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.js.getJsClasspath
import java.io.File

const val NODE_PATH_VAR = "NODE_PATH"

fun Module.getNodeJsEnvironmentVars(): EnvironmentVariablesData {
    val nodeJsClasspath = getJsClasspath(this).joinToString(File.pathSeparator) {
        val basePath = project.basePath ?: return@joinToString it
        FileUtil.getRelativePath(basePath, it, '/') ?: it
    }
    return EnvironmentVariablesData.create(mapOf(NODE_PATH_VAR to nodeJsClasspath), true)
}
