/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test

import kotlin.test.fail

// just a static type check
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> assertStaticTypeIs(@Suppress("UNUSED_PARAMETER") value: @kotlin.internal.NoInfer T) {}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
inline fun <reified T> assertStaticAndRuntimeTypeIs(value: @kotlin.internal.NoInfer T) {
    @Suppress("USELESS_CAST")
    if ((value as Any?) !is T) {
        fail("Expected value $value to have ${T::class} type")
    }
}