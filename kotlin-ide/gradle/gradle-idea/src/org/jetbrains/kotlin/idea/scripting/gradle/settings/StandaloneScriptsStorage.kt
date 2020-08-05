/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "StandaloneScriptsStorage",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class StandaloneScriptsStorage : PersistentStateComponent<StandaloneScriptsStorage> {
    var files: MutableSet<String> = hashSetOf()

    fun getScripts(): List<String> = files.toList()

    override fun getState(): StandaloneScriptsStorage? {
        return this
    }

    override fun loadState(state: StandaloneScriptsStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): StandaloneScriptsStorage? =
            ServiceManager.getService(project, StandaloneScriptsStorage::class.java)
    }
}