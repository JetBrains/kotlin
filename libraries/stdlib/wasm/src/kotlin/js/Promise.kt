/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

public external interface JsValue

public external class Promise(executor: (resolve: (JsValue?) -> Unit, reject: (JsValue) -> Unit) -> Unit): JsValue {
    public open fun then(onFulfilled: (JsValue?) -> JsValue?): Promise
    public open fun then(onFulfilled: (JsValue?) -> JsValue?, onRejected: (JsValue) -> JsValue?): Promise
    public open fun catch(onRejected: (JsValue) -> JsValue?): Promise
    public open fun finally(onFinally: () -> Unit): Promise

    public companion object {
        public fun reject(e: JsValue): Promise
        public fun resolve(e: JsValue): Promise
        public fun resolve(e: Promise): Promise
    }
}

@JsFun("e => { throw e; }")
internal external fun jsThrow(e: JsValue)

@JsFun("""(f) => {
    let result = null;
    try { 
        f();
    } catch (e) {
       result = e;
    }
    return result;
}""")
internal external fun jsCatch(f: () -> Unit): JsValue?

public fun JsValue.toThrowableOrNull(): Throwable? {
    val thisAny: Any = this
    if (thisAny is Throwable) return thisAny
    var result: Throwable? = null
    jsCatch {
        try {
            jsThrow(this)
        } catch (e: Throwable) {
            result = e
        }
    }
    return result
}
