/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.model.Lombok
import org.jetbrains.kotlin.lombok.gradle.LombokExtension
import org.jetbrains.kotlin.lombok.gradle.model.impl.LombokImpl

class LombokModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean = modelName == Lombok::class.java.name

    override fun buildAll(modelName: String, project: Project): Any {
        require(canBuild(modelName)) { "buildAll(\"$modelName\") has been called while canBeBuild is false" }
        val extension = project.extensions.getByType(LombokExtension::class.java)
        return LombokImpl(project.name, extension.configurationFile)
    }
}
