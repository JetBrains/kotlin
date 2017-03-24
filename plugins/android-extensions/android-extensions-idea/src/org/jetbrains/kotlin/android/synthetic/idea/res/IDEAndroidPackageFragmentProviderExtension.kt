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

package org.jetbrains.kotlin.android.synthetic.idea.res

import com.android.tools.idea.gradle.parser.GradleBuildFile
import com.android.tools.idea.gradle.project.facet.gradle.GradleFacet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager
import org.jetbrains.kotlin.android.synthetic.res.AndroidPackageFragmentProviderExtension
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile

class IDEAndroidPackageFragmentProviderExtension(val project: Project) : AndroidPackageFragmentProviderExtension() {
    private val psiManager = PsiManager.getInstance(project)

    override fun getLayoutXmlFileManager(project: Project, moduleInfo: ModuleInfo?): AndroidLayoutXmlFileManager? {
        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return null
        val module = moduleSourceInfo.module
        if (!isAndroidExtensionsEnabled(module) && !isTestMode(module)) return null
        return ModuleServiceManager.getService(module, AndroidLayoutXmlFileManager::class.java)
    }

    private fun isTestMode(module: Module): Boolean {
        return ApplicationManager.getApplication().isUnitTestMode && AndroidFacet.getInstance(module) != null
    }

    private fun isAndroidExtensionsEnabled(module: Module): Boolean {
        // Android Extensions should be always enabled for Android/JPS
        if (isLegacyIdeaAndroidModule(module)) return true

        val androidGradleFacet = GradleFacet.getInstance(module) ?: return false
        val buildFile = androidGradleFacet.gradleModuleModel?.buildFile ?: return false
        val buildGroovyFile = psiManager.findFile(buildFile) as? GroovyFile ?: return false
        return GradleBuildFile.getPlugins(buildGroovyFile).contains("kotlin-android-extensions")
    }

    private fun isLegacyIdeaAndroidModule(module: Module): Boolean {
        val facet = AndroidFacet.getInstance(module)
        return facet != null && !facet.requiresAndroidModel()
    }
}