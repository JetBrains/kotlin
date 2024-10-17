/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.ProducesBuiltinMetadata

package kotlin

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
public actual open class Throwable actual constructor(public open actual val message: String?, public open actual val cause: Throwable?) {
    public actual constructor(message: String?) : this(message, null)

    public actual constructor(cause: Throwable?) : this(cause?.toString(), cause)

    public actual constructor() : this(null, null)
}
