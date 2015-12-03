/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

public class AndroidSubplugin : KotlinGradleSubplugin {

    override fun getExtraArguments(project: Project, task: AbstractCompile): List<SubpluginOption>? {
        val androidExtension = project.extensions.getByName("android") as? BaseExtension ?: return null

        val sourceSets = androidExtension.sourceSets

        val pluginOptions = arrayListOf<SubpluginOption>()

        val mainSourceSet = sourceSets.getByName("main")
        val manifestFile = mainSourceSet.manifest.srcFile
        val applicationPackage = androidExtension.defaultConfig.applicationId
                ?: getApplicationPackageFromManifest(manifestFile) ?: ""
        pluginOptions += SubpluginOption("package", applicationPackage)

        fun addVariant(sourceSet: AndroidSourceSet) {
            pluginOptions += SubpluginOption("variant", sourceSet.name + ';' +
                    mainSourceSet.res.srcDirs.joinToString(";") { it.absolutePath })
        }

        addVariant(mainSourceSet)

        val flavorSourceSets = AndroidGradleWrapper.getProductFlavorsSourceSets(androidExtension).filterNotNull()
        for (sourceSet in flavorSourceSets) {
            addVariant(sourceSet)
        }

        return pluginOptions
    }

    private fun getApplicationPackageFromManifest(manifestFile: File): String? {
        try {
            return manifestFile.parseXml().documentElement.getAttribute("package")
        }
        catch (e: Exception) {
            return null
        }
    }

    override fun getPluginName() = "org.jetbrains.kotlin.android"

    override fun getGroupName() = "org.jetbrains.kotlin"

    override fun getArtifactName() = "kotlin-android-extensions"

    fun File.parseXml(): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(this)
    }
}