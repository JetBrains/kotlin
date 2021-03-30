/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import kotlinx.metadata.Flag
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.klib.annotations

internal const val KNI_BRIDGE_FUNCTION_PREFIX = "kniBridge"

@Suppress("NOTHING_TO_INLINE")
internal inline fun KmFunction.isKniBridgeFunction() =
    name.startsWith(KNI_BRIDGE_FUNCTION_PREFIX)

@Suppress("NOTHING_TO_INLINE")
internal inline fun KmFunction.isTopLevelDeprecatedFunction(isTopLevel: Boolean) =
    isTopLevel && annotations.any { it.className == DEPRECATED_ANNOTATION_FULL_NAME }

@Suppress("NOTHING_TO_INLINE")
internal inline fun KmProperty.isFakeOverride() =
    Flag.Property.IS_FAKE_OVERRIDE(flags)

@Suppress("NOTHING_TO_INLINE")
internal inline fun KmFunction.isFakeOverride() =
    Flag.Function.IS_FAKE_OVERRIDE(flags)
