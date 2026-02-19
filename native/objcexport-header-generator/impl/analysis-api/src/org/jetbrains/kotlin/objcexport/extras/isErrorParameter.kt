/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.extras

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.objcexport.analysisApiUtils.errorParameterName
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

private val isErrorParameterKey = extrasKeyOf<Boolean>("isErrorParameter")

/**
 * [Throws] annotation is a special case
 * ```kotlin
 * class Foo {
 *   @Throws(IOException::class)
 *   fun bar(value: Int)
 * }
 * ```
 * Becomes
 * ```c
 * @interface Foo
 * - (id<Foo> _Nullable)valueValue:(int64_t)value error_:(NSError * _Nullable * _Nullable)error __attribute__((swift_name("value(value_:)")));
 * @end
 * ```
 *
 * 1. We add new method parameter with name [errorParameterName] and type `NSError`
 * 2. It is hidden in `swift_name` attribute
 *
 * To do it we need store information about such annotation in [isErrorParameter] and use later during mangling and naming
 */
internal val ObjCParameter.isErrorParameter: Boolean
    get() =
        extras[isErrorParameterKey] ?: false

internal var MutableExtras.isErrorParameter: Boolean
    get() = this[isErrorParameterKey] ?: false
    set(value) {
        this[isErrorParameterKey] = value
    }