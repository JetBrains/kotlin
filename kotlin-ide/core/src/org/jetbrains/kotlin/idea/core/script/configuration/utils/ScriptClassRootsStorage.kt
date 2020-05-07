/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * This cache is used by [org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptingSupport] only.
 * @see org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptingSupport.collectConfigurations
 *
 */
@State(
    name = "ScriptClassRootsStorage",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class ScriptClassRootsStorage(val project: Project) : PersistentStateComponent<ScriptClassRootsStorage> {
    var classpath: Set<String> = hashSetOf()
    var sources: Set<String> = hashSetOf()
    var sdks: Set<String> = hashSetOf()

    override fun getState(): ScriptClassRootsStorage? {
        return this
    }

    override fun loadState(state: ScriptClassRootsStorage) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): ScriptClassRootsStorage =
            ServiceManager.getService(project, ScriptClassRootsStorage::class.java)
    }
}
