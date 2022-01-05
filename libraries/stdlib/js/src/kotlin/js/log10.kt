/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Math")
package kotlin.js

@PublishedApi
@JsName("log10")
@JsNativeImplementation("""
if (typeof Math.log10 === "undefined") {
    Math.log10 = function(x) {
        return Math.log(x) * Math.LOG10E;
    };
}
""")
internal external fun nativeLog10(value: Double): Double