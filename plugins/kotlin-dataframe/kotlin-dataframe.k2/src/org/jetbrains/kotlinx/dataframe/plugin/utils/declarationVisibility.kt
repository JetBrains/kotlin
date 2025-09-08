/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.descriptors.EffectiveVisibility

/**
 * Plugin-generated declarations refer to the "origin" (i.e. @DataSchema) declaration from another file.
 * "Origin" declaration visibility should allow this
 */
internal val ALLOWED_DECLARATION_VISIBILITY = setOf(EffectiveVisibility.Public, EffectiveVisibility.Internal)