/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
public external open class Throwable {
    public open val message: String?
    public open val cause: Throwable?

    public constructor(message: String?, cause: Throwable?)
    public constructor(message: String?)
    public constructor(cause: Throwable?)
    public constructor()

    // TODO: add specialized version to runtime
//    public override fun equals(other: Any?): Boolean
//    public override fun hashCode(): Int
    public override fun toString(): String
}