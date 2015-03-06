/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.plugin.android

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import com.intellij.openapi.module.ModuleServiceManager
import org.jetbrains.kotlin.lang.resolve.android.AndroidUIXmlProcessor
import org.jetbrains.kotlin.psi.moduleInfo

public class IDEAndroidExternalDeclarationsProvider(private val project: Project) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(moduleInfo: ModuleInfo?): Collection<JetFile> {
        if (moduleInfo !is ModuleSourceInfo) return listOf()

        val module = moduleInfo.module
        val parser = ModuleServiceManager.getService<AndroidUIXmlProcessor>(module, javaClass<AndroidUIXmlProcessor>())
        val syntheticFiles = parser.parseToPsi()
        syntheticFiles?.forEach { it.moduleInfo = moduleInfo }

        return syntheticFiles ?: listOf()
    }
}