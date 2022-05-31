/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.utils.findByType
import org.jetbrains.kotlin.gradle.utils.getByType

interface JavaSourceSetsAccessor {
    val sourceSets: SourceSetContainer

    val sourceSetsIfAvailable: SourceSetContainer?

    interface JavaSourceSetsAccessorVariantFactory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(project: Project): JavaSourceSetsAccessor
    }
}

internal class DefaultJavaSourceSetsAccessorVariantFactory : JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory {
    override fun getInstance(project: Project): JavaSourceSetsAccessor = DefaultJavaSourceSetsAccessor(project.extensions)
}

internal class DefaultJavaSourceSetsAccessor(
    private val projectExtensions: ExtensionContainer
) : JavaSourceSetsAccessor {
    override val sourceSets: SourceSetContainer
        get() = projectExtensions.getByType<JavaPluginExtension>().sourceSets

    override val sourceSetsIfAvailable: SourceSetContainer?
        get() = projectExtensions.findByType<JavaPluginExtension>()?.sourceSets
}