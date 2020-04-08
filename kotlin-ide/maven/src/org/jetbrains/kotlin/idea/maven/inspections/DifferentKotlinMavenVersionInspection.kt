/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.annotations.TestOnly
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.idea.inspections.PluginVersionDependentInspection
import org.jetbrains.kotlin.idea.maven.KotlinMavenBundle
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion

class DifferentKotlinMavenVersionInspection : DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java),
    PluginVersionDependentInspection {
    private val idePluginVersion by lazy { bundledRuntimeVersion() }

    override var testVersionMessage: String? = null
        @TestOnly set

    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) {
            return
        }

        val project = domFileElement.module?.project ?: return
        val mavenManager = MavenProjectsManager.getInstance(project) ?: return

        if (!mavenManager.isMavenizedProject || !mavenManager.isManagedFile(domFileElement.file.virtualFile)) {
            return
        }

        val pomFile = PomFile.forFileOrNull(domFileElement.file) ?: return
        pomFile.findKotlinPlugins().filter { it.version.exists() && it.version.stringValue != idePluginVersion }.forEach { plugin ->
            createProblem(holder, plugin)
        }
    }

    private fun createProblem(holder: DomElementAnnotationHolder, plugin: MavenDomPlugin) {
        val versionFromMaven = plugin.version.stringValue
        val versionFromIde = testVersionMessage ?: idePluginVersion

        holder.createProblem(
            plugin.version,
            HighlightSeverity.WARNING,
            KotlinMavenBundle.message("version.different.maven.ide", versionFromMaven.toString(), versionFromIde)
        )
    }
}