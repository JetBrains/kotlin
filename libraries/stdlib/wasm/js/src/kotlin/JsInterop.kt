/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Implements annotated function in JavaScript and automatically imports is to Wasm.
 * [code] string must contain JS expression that evaluates to JS function with signature that matches annotated kotlin function
 *
 * For example, a function that adds two Doubles via JS:
 *
 *      @JsFun("(x, y) => x + y")
 *      fun jsAdd(x: Double, y: Double): Double =
 *          error("...")
 *
 * This is a temporary annotation because K/Wasm <-> JS interop is not designed yet.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
public annotation class JsFun(val code: String)