/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.project.Project

/**
 * This service appeared as a solution of the problem when [KotlinOutputPathsData] couldn't be deserialized after having been stored on disk
 * as a part of [DataNode] graph. Though we don't need the service itself, for now its the only way to make things work.
 *
 * Deserialization takes place at [serialization.kt#createDataClassResolver] (located in [platform-api.jar]).
 *
 * Graph contains nodes belonging to both IDEA and its plugins. To deserialize a node IDEA tries to load its class using a chain of
 * class-loaders: its own and those provided by actual set of plugins. The nuance is how to get plugins' ones. The approach is the
 * following. There is an association between a node's payload and a service which is supposed to consume it, see [getTargetDataKey] and
 * [DataNode.key]. Plugin services are guaranteed to be loaded by plugin class loader. No service - no plugin, node is just skipped.
 *
 */
class KotlinOutputPathsDataService : AbstractProjectDataService<KotlinOutputPathsData, Void>() {
    override fun getTargetDataKey() = KotlinOutputPathsData.KEY

    override fun importData(
        toImport: MutableCollection<DataNode<KotlinOutputPathsData>>, projectData: ProjectData?, project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
    }
}