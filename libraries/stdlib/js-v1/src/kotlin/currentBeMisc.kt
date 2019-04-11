/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

// Mirrors signature from JS IR BE
// Used for js.translator/testData/box/number/mulInt32.kt
@library
@JsName("imulEmulated")
internal fun imul(x: Int, y: Int): Int = definedExternally