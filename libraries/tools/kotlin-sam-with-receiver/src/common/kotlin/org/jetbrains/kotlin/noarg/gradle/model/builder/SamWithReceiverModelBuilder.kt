/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.model.SamWithReceiver
import org.jetbrains.kotlin.noarg.gradle.model.impl.SamWithReceiverImpl
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension

/**
 * [ToolingModelBuilder] for [SamWithReceiver] models.
 * This model builder is registered for Kotlin Sam With Receiver sub-plugin.
 */
class SamWithReceiverModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == SamWithReceiver::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any {
        require(canBuild(modelName)) { "buildAll(\"$modelName\") has been called while canBeBuild is false" }
        val extension = project.extensions.getByType(SamWithReceiverExtension::class.java)
        return SamWithReceiverImpl(project.name, extension.myAnnotations, extension.myPresets)
    }
}
