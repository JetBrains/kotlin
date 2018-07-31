/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension
import org.jetbrains.kotlin.gradle.model.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.model.impl.KotlinAndroidExtensionImpl

/**
 * [ToolingModelBuilder] for [KotlinAndroidExtension] models.
 * This model builder is registered for Kapt Gradle sub-plugin.
 */
class KotlinAndroidExtensionModelBuilder : ToolingModelBuilder {

    override fun canBuild(modelName: String): Boolean {
        return modelName == KotlinAndroidExtension::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        if (modelName == KotlinAndroidExtension::class.java.name) {
            val extension = project.extensions.getByType(AndroidExtensionsExtension::class.java)
            return KotlinAndroidExtensionImpl(project.name, extension.isExperimental, extension.defaultCacheImplementation.optionName)
        }
        return null
    }
}