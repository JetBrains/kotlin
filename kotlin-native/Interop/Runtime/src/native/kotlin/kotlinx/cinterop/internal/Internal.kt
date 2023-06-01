/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop.internal

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCObject

/**
 * Detaches the Objective-C object from this Kotlin wrapper. More specifically, releases the Obj-C reference and zeroes
 * the field where it is stored.
 *
 * This doesn't affect other possible Kotlin wrappers of this Objective-C object. Typically, when an Objective-C
 * object gets into Kotlin, a new Kotlin wrapper is created, even if there is another wrapper already exists. To get
 * the Objective-C object actually deallocated, each Kotlin wrapper should first be either GCed or detached with this
 * function.
 *
 * If you use this object (Kotlin wrapper) after calling this function, the program behavior is undefined.
 * In particular, it can crash.
 */
@InternalForKotlinNative
@GCUnsafeCall("Kotlin_objc_detachObjCObject")
@OptIn(BetaInteropApi::class)
public external fun detachObjCObject(obj: ObjCObject)
