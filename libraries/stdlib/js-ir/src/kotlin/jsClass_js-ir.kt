/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

// DON'T USE! Use `K::class.js` instead.
// The declaration kept only for backward compatibility with older compilers
// TODO remove, but when?
internal external fun <T : Any> jsClass(): JsClass<T>
