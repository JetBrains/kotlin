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

package org.jetbrains.kotlin.idea.js

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

private fun addSingleModulePaths(target: Module, result: MutableList<String>) {
    val compilerExtension = CompilerModuleExtension.getInstance(target) ?: return
    result.addIfNotNull(compilerExtension.compilerOutputPath?.path)
    result.addIfNotNull(compilerExtension.compilerOutputPathForTests?.let { "${it.path}/lib" })
}

fun getJsClasspath(module: Module): List<String> {
    val result = ArrayList<String>()
    ModuleRootManager.getInstance(module).orderEntries().recursively().forEachModule {
        addSingleModulePaths(it, result)
        true
    }
    return result
}
