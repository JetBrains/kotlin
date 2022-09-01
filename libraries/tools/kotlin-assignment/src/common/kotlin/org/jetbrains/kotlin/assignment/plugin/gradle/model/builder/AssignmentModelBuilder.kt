/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.assignment.plugin.gradle.AssignmentExtension
import org.jetbrains.kotlin.assignment.plugin.gradle.model.impl.AssignmentImpl
import org.jetbrains.kotlin.gradle.model.Assignment

/**
 * [ToolingModelBuilder] for [ValueContainerAssignment] models.
 * This model builder is registered for Kotlin Value Container Assignment sub-plugin.
 */
class AssignmentModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName.equals(Assignment::class.java.name)
    }

    override fun buildAll(modelName: String, project: Project): Any {
        require(canBuild(modelName)) { "buildAll(\"$modelName\") has been called while canBeBuild is false" }
        val extension = project.extensions.getByType(AssignmentExtension::class.java)
        return AssignmentImpl(project.name, extension.myAnnotations)
    }
}
