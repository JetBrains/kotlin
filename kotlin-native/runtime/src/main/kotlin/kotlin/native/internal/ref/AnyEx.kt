/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.ref

import kotlin.native.internal.InternalForKotlinNative

/**
 * Returns associated ObjC object for given Kotlin object.
 *
 * @see AssociatedObject
 */
@InternalForKotlinNative
public val Any.associatedObject: AssociatedObject
    get() = AssociatedObject(this)