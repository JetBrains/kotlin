/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Exposes the JavaScript [Promise object](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Promise) to Kotlin.
 */
public external class Promise<out T>(executor: (resolve: (Dynamic?) -> Unit, reject: (Dynamic) -> Unit) -> Unit) {
    public fun then(onFulfilled: (Dynamic?) -> Dynamic?): Promise<Dynamic?>
    public fun then(onFulfilled: (Dynamic?) -> Dynamic?, onRejected: (Dynamic) -> Dynamic?): Promise<Dynamic?>
    public fun catch(onRejected: (Dynamic) -> Dynamic?): Promise<Dynamic?>
    public fun finally(onFinally: () -> Unit): Promise<Dynamic?>

    public companion object {
        public fun reject(e: Dynamic): Promise<Dynamic?>
        public fun resolve(e: Dynamic): Promise<Dynamic?>
        public fun resolve(e: Promise<Dynamic?>): Promise<Dynamic?>
    }
}


