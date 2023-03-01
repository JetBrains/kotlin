/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.arguments

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class KotlinTaskProperties(providerFactory: ProviderFactory) {
    val kotlinLanguageVersion: Provider<KotlinVersion> = readKotlinVersionProperty(KOTLIN_LANGUAGE_VERSION_PROPERTY, providerFactory)
    val kotlinApiVersion: Provider<KotlinVersion> = readKotlinVersionProperty(KOTLIN_API_VERSION_PROPERTY, providerFactory)

    companion object {
        private const val KOTLIN_LANGUAGE_VERSION_PROPERTY = "kotlin.test.languageVersion"
        private const val KOTLIN_API_VERSION_PROPERTY = "kotlin.test.apiVersion"

        @Suppress("DEPRECATION")
        private fun readKotlinVersionProperty(propertyName: String, providerFactory: ProviderFactory): Provider<KotlinVersion> {
            return if (GradleVersion.current() < GradleVersion.version("7.4")) {
                providerFactory.gradleProperty(propertyName)
                    .forUseAtConfigurationTime()
                    .map { KotlinVersion.fromVersion(it as String) }
            } else {
                providerFactory.gradleProperty(propertyName)
                    .map { KotlinVersion.fromVersion(it as String) }
            }
        }
    }


}