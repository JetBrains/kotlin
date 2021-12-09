/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

@PublishedApi
@JsName("cosh")
@JsNativeImplementation("""
if (typeof Math.cosh === "undefined") {
    Math.cosh = function(x) {
        var y = Math.exp(x);
        var y1 = 1 / y;
        if (!isFinite(y) || !isFinite(y1)) return Math.exp(Math.abs(x) - Math.LN2);
        return (y + y1) / 2;
    };
}
""")
internal external fun nativeCosh(value: Double): Double