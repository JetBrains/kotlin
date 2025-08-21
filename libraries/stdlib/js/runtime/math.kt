/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
@file:JsQualifier("Math")
package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode

@JsPolyfill("""
if (typeof Math.imul === "undefined") {
  Math.imul = function imul(a, b) {
    return ((a & 0xffff0000) * (b & 0xffff) + (a & 0xffff) * (b | 0)) | 0; 
  }
}
""")
@UsedFromCompilerGeneratedCode
internal external fun imul(a_local: Int, b_local: Int): Int
