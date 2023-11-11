/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.tasks.SourceSet

internal class SourceSetCompatibilityHelperG6 : SourceSetCompatibilityHelper {
    override fun getCompileConfigurationName(sourceSet: SourceSet): String {
        @Suppress("DEPRECATION")
        return sourceSet.compileConfigurationName
    }

    override fun getRuntimeConfigurationName(sourceSet: SourceSet): String {
        @Suppress("DEPRECATION")
        return sourceSet.runtimeConfigurationName
    }
}

internal class SourceSetCompatibilityHelperG6VariantFactory : SourceSetCompatibilityHelper.SourceSetCompatibilityHelperVariantFactory {
    override fun getInstance(): SourceSetCompatibilityHelper = SourceSetCompatibilityHelperG6()
}
