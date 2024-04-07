/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.compose.compiler.gradle.model.impl.ComposeCompilerImpl
import org.jetbrains.kotlin.gradle.model.ComposeCompiler

/**
 * [ToolingModelBuilder] for [ComposeCompiler] models.
 * This model builder is registered for Kotlin Compose compiler plugin.
 */
class ComposeCompilerModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == ComposeCompiler::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any {
        require(canBuild(modelName)) { "buildAll(\"$modelName\") has been called while canBeBuild is false" }
        return ComposeCompilerImpl(project.name)
    }
}
