/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package java.util

public open class ConcurrentModificationException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}