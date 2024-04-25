/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService.BooleanGradleProperty
import org.jetbrains.kotlin.gradle.internal.properties.propertiesService

internal object KaptProperties {
    object BooleanProperties {
        val KAPT_VERBOSE = BooleanGradleProperty("kapt.verbose", false)
    }

    fun isKaptVerbose(project: Project): Provider<Boolean> = project.propertiesService.flatMap {
        it.property(BooleanProperties.KAPT_VERBOSE, project)
    }
}