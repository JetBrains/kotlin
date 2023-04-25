/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER") // TODO: Remove after bootstrap update

package kotlin.js

/**
 * Represents universal type for JS interoperability.
 */
@Deprecated("Use JsAny instead", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("JsAny"))
public external interface Dynamic : JsAny

/**
 * Reinterprets this value as a value of the Dynamic type.
 */
@Deprecated("If value is a subtype of JsAny, use JsAny instead. Otherwise, use toJsReference", level = DeprecationLevel.ERROR)
fun Any.asDynamic(): JsAny = this.toJsReference()

/**
 * Reinterprets this value as a value of the Dynamic type.
 */
@Deprecated("Use toJsString instead", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("this.toJsString()"))
@kotlin.internal.InlineOnly
fun String.asDynamic(): JsString = this.toJsString()

private fun jsThrow(e: JsAny) {
    js("throw e;")
}

private fun jsCatch(f: () -> Unit): JsAny? {
    js("""
    let result = null;
    try { 
        f();
    } catch (e) {
       result = e;
    }
    return result;
    """)
}

/**
 * For a Dynamic value caught in JS, returns the corresponding [Throwable]
 * if it was thrown from Kotlin, or null otherwise.
 */
public fun JsAny.toThrowableOrNull(): Throwable? {
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
