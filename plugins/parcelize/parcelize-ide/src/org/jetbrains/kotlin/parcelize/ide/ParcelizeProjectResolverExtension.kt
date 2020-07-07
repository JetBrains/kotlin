/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.ide

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.util.Key
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

@Suppress("unused")
class ParcelizeProjectResolverExtension : AbstractProjectResolverExtension() {
    companion object {
        val KEY = Key<ParcelizeGradleModel>("ParcelizeModel")
    }

    override fun getExtraProjectModelClasses() = setOf(ParcelizeGradleModel::class.java)
    override fun getToolingExtensionsClasses() = setOf(ParcelizeModelBuilderService::class.java, Unit::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val parcelizeModel = resolverCtx.getExtraProject(gradleModule, ParcelizeGradleModel::class.java)

        if (parcelizeModel != null) {
            ideModule.putCopyableUserData(KEY, ParcelizeGradleModelImpl(isEnabled = parcelizeModel.isEnabled))
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}

