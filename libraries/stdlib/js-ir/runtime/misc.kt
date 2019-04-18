/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

// TODO: Polyfill
internal fun imul(a_local: Int, b_local: Int) =
    js("((a_local & 0xffff0000) * (b_local & 0xffff) + (a_local & 0xffff) * (b_local | 0)) | 0").unsafeCast<Int>()