 /*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer

internal class JavaSourceSetsAccessorG6(
    private val convention: Convention
) : JavaSourceSetsAccessor {
    override val sourceSets: SourceSetContainer
        get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    override val sourceSetsIfAvailable: SourceSetContainer?
        get() = convention.findPlugin(JavaPluginConvention::class.java)?.sourceSets

    internal class JavaSourceSetAccessorVariantFactoryG6 : JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory {
        override fun getInstance(project: Project): JavaSourceSetsAccessor = JavaSourceSetsAccessorG6(project.convention)
    }
}
