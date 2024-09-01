/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
@JsName("Error")
public actual open external class Throwable {
    public actual open val message: String?
    public actual open val cause: Throwable?

    public actual constructor(message: String?, cause: Throwable?)
    public actual constructor(message: String?)
    public actual constructor(cause: Throwable?)
    public actual constructor()

    // TODO: add specialized version to runtime
//    public actual override fun equals(other: Any?): Boolean
//    public actual override fun hashCode(): Int
    public override fun toString(): String
}