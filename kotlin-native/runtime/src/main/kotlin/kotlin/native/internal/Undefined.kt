/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

/**
 * Returns undefined value of type `T`.
 * This method is unsafe and should be used with care.
 */
@SymbolName("Kotlin_native_internal_undefined")
@GCCritical
internal external fun <T> undefined(): T