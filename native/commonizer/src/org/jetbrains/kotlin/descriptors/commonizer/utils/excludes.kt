/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor

internal const val KNI_BRIDGE_FUNCTION_PREFIX = "kniBridge"

internal fun SimpleFunctionDescriptor.isKniBridgeFunction() =
    name.asString().startsWith(KNI_BRIDGE_FUNCTION_PREFIX)

internal fun SimpleFunctionDescriptor.isDeprecatedTopLevelFunction() =
    containingDeclaration is PackageFragmentDescriptor && annotations.hasAnnotation(DEPRECATED_ANNOTATION_FQN)
