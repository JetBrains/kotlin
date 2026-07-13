/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

@JsName("Number")
internal external object JsNumberObject {
    fun isNaN(value: Number): Boolean
    fun isFinite(value: Number): Boolean
}
