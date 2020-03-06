/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
package kotlin.internal.jdk7

import kotlin.internal.PlatformImplementations

internal open class JDK7PlatformImplementations : PlatformImplementations() {

    override fun addSuppressed(cause: Throwable, exception: Throwable) = cause.addSuppressed(exception)

    override fun getSuppressed(exception: Throwable): List<Throwable> = exception.suppressed.asList()
}
