/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory

internal object KotlinCompilationLanguageVersionConfigurator : KotlinCompilationImplFactory.PreConfigure {
    override fun configure(compilation: KotlinCompilationImpl) {
        compilation.project.kotlinPropertiesProvider.languageVersion?.let { languageVersionString ->
            compilation.compilerOptions.options.languageVersion.convention(KotlinVersion.fromVersion(languageVersionString))
        }
    }
}