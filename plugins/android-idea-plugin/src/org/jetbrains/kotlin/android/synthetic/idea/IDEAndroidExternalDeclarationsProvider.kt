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

package org.jetbrains.kotlin.android.synthetic.idea

import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.idea.res.IDESyntheticFileGenerator
import org.jetbrains.kotlin.android.synthetic.res.SyntheticFileGenerator
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.moduleInfo

public class IDEAndroidExternalDeclarationsProvider(private val project: Project) : ExternalDeclarationsProvider {
    override fun getExternalDeclarations(moduleInfo: ModuleInfo?): Collection<KtFile> {
        if (moduleInfo !is ModuleSourceInfo) return listOf()

        val module = moduleInfo.module
        val parser = ModuleServiceManager.getService(module, javaClass<SyntheticFileGenerator>()) as? IDESyntheticFileGenerator
        val syntheticFiles = parser?.getSyntheticFiles()
        syntheticFiles?.forEach { it.moduleInfo = moduleInfo }

        return syntheticFiles ?: listOf()
    }
}