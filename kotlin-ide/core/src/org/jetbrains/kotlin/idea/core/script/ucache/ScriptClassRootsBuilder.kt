/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.ucache

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

class ScriptClassRootsBuilder(
    val project: Project,
    private val classes: MutableSet<String> = mutableSetOf(),
    private val sources: MutableSet<String> = mutableSetOf(),
    private val scripts: MutableMap<String, ScriptClassRootsCache.LightScriptInfo> = mutableMapOf()
) {
    val sdks = ScriptSdksBuilder(project)

    private var customDefinitionsUsed: Boolean = false

    fun build(): ScriptClassRootsCache =
        ScriptClassRootsCache(
            project, scripts, classes, sources,
            customDefinitionsUsed, sdks.build()
        )

    fun useCustomScriptDefinition() {
        customDefinitionsUsed = true
    }

    fun add(
        vFile: VirtualFile,
        configuration: ScriptCompilationConfigurationWrapper
    ) {
        sdks.addSdk(configuration.javaHome)

        configuration.dependenciesClassPath.forEach { classes.add(it.absolutePath) }
        configuration.dependenciesSources.forEach { sources.add(it.absolutePath) }

        scripts[vFile.path] = ScriptClassRootsCache.DirectScriptInfo(configuration)

        useCustomScriptDefinition()
    }

    fun addCustom(
        path: String,
        scriptClassesRoots: Collection<String>,
        sourceSourcesRoots: Collection<String>,
        info: ScriptClassRootsCache.LightScriptInfo
    ) {
        classes.addAll(scriptClassesRoots)
        sources.addAll(sourceSourcesRoots)
        scripts[path] = info
    }

    fun addTemplateClassesRoots(classesRoots: Collection<String>) {
        classes.addAll(classesRoots)
    }

    @Deprecated("Don't use, used only from DefaultScriptingSupport for saving to storage")
    fun add(other: ScriptClassRootsBuilder) {
        classes.addAll(other.classes)
        sources.addAll(other.sources)
        sdks.addAll(other.sdks)
        scripts.putAll(other.scripts)
    }

    fun toStorage(storage: ScriptClassRootsStorage) {
        storage.classpath = classes
        storage.sources = sources
        sdks.toStorage(storage)
    }

    companion object {
        fun fromStorage(project: Project, storage: ScriptClassRootsStorage) = ScriptClassRootsBuilder(
            project,
            storage.classpath.toMutableSet(),
            storage.sources.toMutableSet(),
        ).also {
            it.sdks.fromStorage(storage)
        }
    }
}
