/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.components.*
import com.intellij.openapi.components.StoragePathMacros.WORKSPACE_FILE
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "KotlinCodeInsightWorkspaceSettings", storages = [Storage(WORKSPACE_FILE)])
class KotlinCodeInsightWorkspaceSettings : PersistentStateComponent<KotlinCodeInsightWorkspaceSettings> {

    @JvmField
    var optimizeImportsOnTheFly = false

    override fun getState() = this

    override fun loadState(state: KotlinCodeInsightWorkspaceSettings) = XmlSerializerUtil.copyBean(state, this)

    companion object {

        fun getInstance(project: Project): KotlinCodeInsightWorkspaceSettings {
            return ServiceManager.getService(project, KotlinCodeInsightWorkspaceSettings::class.java)
        }

    }

}

@State(name = "KotlinCodeInsightSettings", storages = [Storage("editor.codeinsight.xml")])
class KotlinCodeInsightSettings : PersistentStateComponent<KotlinCodeInsightSettings> {

    @JvmField
    var addUnambiguousImportsOnTheFly = false

    override fun getState() = this

    override fun loadState(state: KotlinCodeInsightSettings) = XmlSerializerUtil.copyBean(state, this)

    companion object {

        fun getInstance(): KotlinCodeInsightSettings {
            return ServiceManager.getService(KotlinCodeInsightSettings::class.java)
        }

    }

}