/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    open val message: String?
    open val cause: Throwable?

    constructor(message: String?, cause: Throwable?)
    constructor(message: String?)
    constructor(cause: Throwable?)
    constructor()
}