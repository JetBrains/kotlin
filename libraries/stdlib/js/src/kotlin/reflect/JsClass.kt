/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import getKClass
import kotlin.reflect.KClass
import kotlin.reflect.js.internal.KClassImpl

/**
 * Represents the constructor of a class. Instances of `JsClass` can be passed to JavaScript APIs that expect a constructor reference.
 */
external interface JsClass<T : Any> {
    /**
     * Returns the unqualified name of the class represented by this instance.
     */
    val name: String
}

@Deprecated("Use class literal and extension property `js` instead.", replaceWith = ReplaceWith("T::class.js"), level = DeprecationLevel.ERROR)
external fun <T : Any> jsClass(): JsClass<T>

@Deprecated("Use class literal and extension property `js` instead.", replaceWith = ReplaceWith("this::class.js"), level = DeprecationLevel.ERROR)
val <T : Any> T.jsClass: JsClass<T>
    get() = when (jsTypeOf(this)) {
        "string" -> js("String")
        "number" -> js("Number")
        "boolean" -> js("Boolean")
        else -> js("Object").getPrototypeOf(this).constructor
    }

/**
 * Obtains a constructor reference for the given `KClass`.
 */
val <T : Any> KClass<T>.js: JsClass<T>
    get() = (this as KClassImpl<T>).jClass

/**
 * Obtains a `KClass` instance for the given constructor reference.
 */
val <T : Any> JsClass<T>.kotlin: KClass<T>
    get() = getKClass(this)
