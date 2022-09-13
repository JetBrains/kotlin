/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container.assignment.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.container.assignment.gradle.ValueContainerAssignmentExtension
import org.jetbrains.kotlin.container.assignment.gradle.model.impl.ValueContainerAssignmentImpl
import org.jetbrains.kotlin.gradle.model.ValueContainerAssignment

/**
 * [ToolingModelBuilder] for [ValueContainerAssignment] models.
 * This model builder is registered for Kotlin Value Container Assignment sub-plugin.
 */
class ValueContainerAssignmentModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName.equals(ValueContainerAssignment::class.java.name)
    }

    override fun buildAll(modelName: String, project: Project): Any {
        require(canBuild(modelName)) { "buildAll(\"$modelName\") has been called while canBeBuild is false" }
        val extension = project.extensions.getByType(ValueContainerAssignmentExtension::class.java)
        return ValueContainerAssignmentImpl(project.name, extension.myAnnotations)
    }
}
