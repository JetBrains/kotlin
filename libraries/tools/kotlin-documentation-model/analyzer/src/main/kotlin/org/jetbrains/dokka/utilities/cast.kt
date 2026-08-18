/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
public inline fun <reified T> Any.cast(): T {
    return this as T
}
