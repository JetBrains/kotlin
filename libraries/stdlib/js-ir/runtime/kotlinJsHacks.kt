/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@PublishedApi
internal fun <T : Enum<T>> enumValuesIntrinsic(): Array<T> =
    throw IllegalStateException("Should be replaced by compiler")

@PublishedApi
internal fun <T : Enum<T>> enumValueOfIntrinsic(@Suppress("UNUSED_PARAMETER") name: String): T =
    throw IllegalStateException("Should be replaced by compiler")

@PublishedApi
internal fun safePropertyGet(self: dynamic, getterName: String, propName: String): dynamic {
    val getter = self[getterName]
    return if (getter != null) getter.call(self) else self[propName]
}

@PublishedApi
internal fun safePropertySet(self: dynamic, setterName: String, propName: String, value: dynamic) {
    val setter = self[setterName]
    if (setter != null) setter.call(self, value) else self[propName] = value
}

/**
 * Implements annotated function in JavaScript.
 * [code] string must contain JS expression that evaluates to JS function with signature that matches annotated kotlin function
 *
 * For example, a function that adds two Doubles:
 *
 *      @JsFun("(x, y) => x + y")
 *      fun jsAdd(x: Double, y: Double): Double =
 *          error("...")
 *
 * Code gets inserted as is without syntax verification.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
internal annotation class JsFun(val code: String)