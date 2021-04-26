/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.model.Kapt
import org.jetbrains.kotlin.gradle.model.KaptSourceSet
import org.jetbrains.kotlin.gradle.model.impl.KaptImpl
import org.jetbrains.kotlin.gradle.model.impl.KaptSourceSetImpl

/**
 * [ToolingModelBuilder] for [Kapt] models.
 * This model builder is registered for Kapt Gradle sub-plugin.
 */
class KaptModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == Kapt::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        if (modelName == Kapt::class.java.name) {
            val kaptTasks = project.tasks.withType(KaptTask::class.java)
            return KaptImpl(project.name, kaptTasks.map { it.createKaptSourceSet() })
        }
        return null
    }

    companion object {

        private fun KaptTask.createKaptSourceSet(): KaptSourceSet {
            return KaptSourceSetImpl(
                this.sourceSetName.get(),
                if (this.sourceSetName.get().contains(
                        "test",
                        true
                    )
                ) KaptSourceSet.KaptSourceSetType.TEST else KaptSourceSet.KaptSourceSetType.PRODUCTION,
                destinationDir,
                kotlinSourcesDestinationDir,
                classesDir
            )
        }
    }
}