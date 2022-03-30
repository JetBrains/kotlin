/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@JsPolyfill("""
(function() {
    if (typeof globalThis === 'object') return; 
    Object.defineProperty(Object.prototype, 'globalThis', {
        get: function() {
            return this;
        },
        configurable: true
    });
    globalThis.globalThis = globalThis;
    delete Object.prototype.globalThis;
}());
""")
internal external val globalThis: dynamic