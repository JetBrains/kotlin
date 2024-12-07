/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.metadataDependencyResolutionsOrEmpty
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.metadataTransformation

internal inline fun <reified T : MetadataDependencyResolution> KotlinSourceSet.resolveMetadata(): List<T> {
    return internal.metadataTransformation.metadataDependencyResolutionsOrEmpty.filterIsInstance<T>()
}
