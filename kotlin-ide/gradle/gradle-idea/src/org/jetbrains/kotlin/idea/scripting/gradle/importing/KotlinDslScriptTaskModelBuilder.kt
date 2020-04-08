/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.scripting.gradle.importing

import org.gradle.api.Project
import org.gradle.tooling.model.kotlin.dsl.KotlinDslModelsParameters.PREPARATION_TASK_NAME
import org.jetbrains.kotlin.idea.scripting.gradle.kotlinDslScriptsModelImportSupported
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext

interface KotlinDslScriptAdditionalTask

class KotlinDslScriptTaskModelBuilder : AbstractModelBuilderService() {
    override fun canBuild(modelName: String): Boolean {
        return KotlinDslScriptAdditionalTask::class.java.name == modelName
    }

    override fun buildAll(modelName: String, project: Project, context: ModelBuilderContext): Any? {
        if (kotlinDslScriptsModelImportSupported(project.gradle.gradleVersion)) {
            val startParameter = project.gradle.startParameter

            val tasks = HashSet(startParameter.taskNames)
            tasks.add(PREPARATION_TASK_NAME)
            startParameter.setTaskNames(tasks)
        }
        return null
    }

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(
            project, e, "Kotlin DSL script model errors"
        ).withDescription("Unable to set $PREPARATION_TASK_NAME sync task.")
    }
}