/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * The fragment identifier is recorded as umanifest fragment identifier and as the fragment directory name in the Uklib archive
 */
private val KotlinSourceSet.uklibFragmentIdentifier: String
    get() = name
internal val KotlinCompilation<*>.uklibFragmentIdentifier: String
    get() = defaultSourceSet.uklibFragmentIdentifier
