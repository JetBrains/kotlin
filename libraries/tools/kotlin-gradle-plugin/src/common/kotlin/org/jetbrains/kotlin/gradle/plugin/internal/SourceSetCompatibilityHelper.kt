/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories

interface SourceSetCompatibilityHelper {

    fun getCompileConfigurationName(sourceSet: SourceSet): String?

    fun getRuntimeConfigurationName(sourceSet: SourceSet): String?

    interface SourceSetCompatibilityHelperVariantFactory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(): SourceSetCompatibilityHelper
    }
}

internal class DefaultSourceSetCompatibilityHelperVariantFactory : SourceSetCompatibilityHelper.SourceSetCompatibilityHelperVariantFactory {
    override fun getInstance(): SourceSetCompatibilityHelper = DefaultSourceSetCompatibilityHelper()
}

internal class DefaultSourceSetCompatibilityHelper() : SourceSetCompatibilityHelper {
    override fun getCompileConfigurationName(sourceSet: SourceSet): String? = null

    override fun getRuntimeConfigurationName(sourceSet: SourceSet): String? = null
}