/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

internal class ConfigurationCacheStartParameterAccessorG76(
    private val gradle: Gradle,
) : ConfigurationCacheStartParameterAccessor {
    override val isConfigurationCacheEnabled: Boolean by lazy {
        gradle.startParameter.isConfigurationCacheRequested
    }
    override val isConfigurationCacheRequested: Boolean
        get() = isConfigurationCacheEnabled

    internal class Factory : ConfigurationCacheStartParameterAccessor.Factory {
        override fun getInstance(project: Project) = ConfigurationCacheStartParameterAccessorG76(project.gradle)
    }
}
