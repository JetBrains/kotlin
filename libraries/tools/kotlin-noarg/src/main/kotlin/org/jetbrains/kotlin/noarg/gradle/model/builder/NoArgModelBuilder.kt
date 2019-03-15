/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.model.NoArg
import org.jetbrains.kotlin.noarg.gradle.NoArgExtension
import org.jetbrains.kotlin.noarg.gradle.model.impl.NoArgImpl

/**
 * [ToolingModelBuilder] for [NoArg] models.
 * This model builder is registered for Kotlin No Arg sub-plugin.
 */
class NoArgModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == NoArg::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        if (modelName == NoArg::class.java.name) {
            val extension = project.extensions.getByType(NoArgExtension::class.java)
            return NoArgImpl(project.name, extension.myAnnotations, extension.myPresets, extension.invokeInitializers)
        }
        return null
    }
}