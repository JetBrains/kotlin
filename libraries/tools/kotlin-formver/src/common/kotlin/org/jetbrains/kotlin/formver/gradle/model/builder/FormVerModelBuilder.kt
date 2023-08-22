/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.formver.gradle.FormVerExtension
import org.jetbrains.kotlin.formver.gradle.model.impl.FormVerImpl
import org.jetbrains.kotlin.gradle.model.FormVer

class FormVerModelBuilder : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean =
        modelName == FormVer::class.java.name

    override fun buildAll(modelName: String, project: Project): Any {
        require(canBuild(modelName)) { "buildAll(\"$modelName\") has been called while canBeBuild is false" }
        val extension = project.extensions.getByType(FormVerExtension::class.java)
        return FormVerImpl(project.name, extension.myLogLevel)
    }

}
