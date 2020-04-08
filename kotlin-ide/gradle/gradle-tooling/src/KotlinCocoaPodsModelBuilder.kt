/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext

private const val POD_IMPORT_TASK_NAME = "podImport"

interface EnablePodImportTask

class KotlinCocoaPodsModelBuilder : AbstractModelBuilderService() {
    override fun canBuild(modelName: String?): Boolean {
        return EnablePodImportTask::class.java.name == modelName
    }

    override fun buildAll(modelName: String, project: Project, context: ModelBuilderContext): Any? {
        val startParameter = project.gradle.startParameter
        val taskNames = startParameter.taskNames

        if (project.tasks.findByPath(POD_IMPORT_TASK_NAME) != null && POD_IMPORT_TASK_NAME !in taskNames) {
            taskNames.add(POD_IMPORT_TASK_NAME)
            startParameter.setTaskNames(taskNames)
        }
        return null
    }

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(
            project, e, "EnablePodImportTask error"
        ).withDescription("Unable to create $POD_IMPORT_TASK_NAME task.")
    }
}