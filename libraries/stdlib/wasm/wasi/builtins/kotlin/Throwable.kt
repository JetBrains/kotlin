/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.getSimpleName

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
public open class Throwable(open val message: String?, open val cause: kotlin.Throwable?) {
    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    //TODO: Investigate possibility to make WASI stack discoverable (KT-60965)
    internal val stack: String get() = ""

    internal var suppressedExceptionsList: MutableList<Throwable>? = null

    /**
     * Returns the short description of this throwable consisting of the exception class name
     * followed by the exception message if it is not null.
     */
    public override fun toString(): String {
        val s = getSimpleName(this.typeInfo)
        return if (message != null) s + ": " + message.toString() else s
    }
}