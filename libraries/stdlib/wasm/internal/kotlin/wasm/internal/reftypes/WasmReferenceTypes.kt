/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:ExcludedFromCodegen

package kotlin.wasm.internal.reftypes

import kotlin.wasm.internal.*

// These interfaces correspond to Wasm GC reference types with the same name.
// They are not proper Kotlin interfaces and should be used with care.
//
// WARNING! Do not upcast to Any, nullable types, type parameters, etc.
//          Do not use Any methods
//          Do not use type operators `is`, `as`, etc.
//          Do not use ==, ===
//
// Use dedicated intrinsics instead.

internal interface anyref
internal interface eqref : anyref
internal interface structref : eqref
internal interface i31ref : eqref
internal interface funcref : anyref