/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsQualifier("Object")
package kotlin.js
// ES6 Object polyfills

@PublishedApi
@JsName("values")
@JsPolyfill("""
if (typeof Object.values === "undefined") {
    Object.values = function(obj) {
        return Object.keys(obj).map(function (key) { return obj[key] })
    };
}
""")
internal external fun nativeObjectValues(obj: Any): Array<dynamic>