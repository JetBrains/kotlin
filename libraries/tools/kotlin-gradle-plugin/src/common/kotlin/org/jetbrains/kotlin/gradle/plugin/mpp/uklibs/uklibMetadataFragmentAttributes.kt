/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal val KotlinSourceSet.metadataFragmentAttributes: Set<UklibFragmentPlatformAttribute>
    // FIXME: Recheck in tests that "compilations" doesn't leak associated compilations targets?
    get() = internal.compilations.filterNot {
        it is KotlinMetadataCompilation
    }.map {
        it.uklibFragmentPlatformAttribute
    }.toSet()
internal val KotlinCompilation<*>.metadataFragmentAttributes: Set<UklibFragmentPlatformAttribute>
    get() = defaultSourceSet.metadataFragmentAttributes