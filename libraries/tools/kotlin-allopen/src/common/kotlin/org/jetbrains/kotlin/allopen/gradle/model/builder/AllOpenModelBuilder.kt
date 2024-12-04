/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.allopen.gradle.model.impl.AllOpenImpl
import org.jetbrains.kotlin.gradle.model.AllOpen

/**
 * [ToolingModelBuilder] for [AllOpen] models.
 * This model builder is registered for Kotlin All Open sub-plugin.
 */
class AllOpenModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == AllOpen::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any {
        require(canBuild(modelName)) { "buildAll(\"$modelName\") has been called while canBeBuild is false" }
        val extension = project.extensions.getByType(AllOpenExtension::class.java)
        return AllOpenImpl(project.name, extension.myAnnotations, extension.myPresets)
    }
}
