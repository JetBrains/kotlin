/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import getKClass
import kotlin.reflect.KClass
import kotlin.reflect.js.internal.KClassImpl

/**
 * Represents the constructor of a class. Instances of `JsClass` can be passed to JavaScript APIs that expect a constructor reference.
 */
public external interface JsClass<T : Any> {
    /**
     * Returns the unqualified name of the class represented by this instance.
     */
    public val name: String
}

/**
 * Obtains a constructor reference for the given `KClass`.
 */
public val <T : Any> KClass<T>.js: JsClass<T>
    get() = (this as KClassImpl<T>).jClass

/**
 * Obtains a `KClass` instance for the given constructor reference.
 */
public val <T : Any> JsClass<T>.kotlin: KClass<T>
    get() = getKClass(this)
