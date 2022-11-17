/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

public external interface JsValue

public external class Promise<out T>(executor: (resolve: (JsValue?) -> Unit, reject: (JsValue) -> Unit) -> Unit): JsValue {
    public open fun then(onFulfilled: (JsValue?) -> JsValue?): UntypedPromise
    public open fun then(onFulfilled: (JsValue?) -> JsValue?, onRejected: (JsValue) -> JsValue?): UntypedPromise
    public open fun catch(onRejected: (JsValue) -> JsValue?): UntypedPromise
    public open fun finally(onFinally: () -> Unit): UntypedPromise

    public companion object {
        public fun reject(e: JsValue): UntypedPromise
        public fun resolve(e: JsValue): UntypedPromise
        public fun resolve(e: UntypedPromise): UntypedPromise
    }
}

public typealias UntypedPromise = Promise<JsValue?>

@JsFun("e => { throw e; }")
private external fun jsThrow(e: JsValue)

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
