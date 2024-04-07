/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.extras

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCType
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

/**
 * Acts as scope for discoverable 'builder functions'
 * - See [objCTypeExtras] factory function
 * - See [MutableExtras.originClassId] as example for declaring a discoverable API for types
 * - See [MutableExtras.requiresForwardDeclaration] "="
 */
internal object ObjCTypeExtrasBuilderContext

/**
 * Convenience function for building extras for [ObjCType]
 */
internal fun objCTypeExtras(builder: context(ObjCTypeExtrasBuilderContext) MutableExtras.() -> Unit): Extras {
    return mutableExtrasOf().also { extras -> builder(ObjCTypeExtrasBuilderContext, extras) }
}
