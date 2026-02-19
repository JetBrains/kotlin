/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE
import org.jetbrains.kotlin.gradle.plugin.extraProperties

@ExternalKotlinTargetApi
interface ExternalKotlinTargetApiUtils {
    fun publishJvmEnvironmentAttribute(publish: Boolean = true)
}

@ExternalKotlinTargetApi
val Project.externalKotlinTargetApiUtils: ExternalKotlinTargetApiUtils
    get() = object : ExternalKotlinTargetApiUtils {
        override fun publishJvmEnvironmentAttribute(publish: Boolean) {
            project.extraProperties.set(KOTLIN_PUBLISH_JVM_ENVIRONMENT_ATTRIBUTE, publish.toString())
        }
    }
