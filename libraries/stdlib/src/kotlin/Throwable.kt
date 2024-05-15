/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
@ActualizeByJvmBuiltinProvider
public expect open class Throwable {
    public open val message: String?
    public open val cause: Throwable?

    public constructor()
    public constructor(message: String?)
    public constructor(cause: Throwable?)
    public constructor(message: String?, cause: Throwable?)
}
