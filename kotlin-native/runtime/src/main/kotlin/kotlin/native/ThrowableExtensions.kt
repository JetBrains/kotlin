/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.experimental.ExperimentalNativeApi

/**
 * Returns a list of stack trace addresses representing the stack trace
 * pertaining to this throwable.
 */
// Deprecate this function in favour of KT-57164 when it gets implemented
@ExperimentalNativeApi
public fun Throwable.getStackTraceAddresses(): List<Long> =
        this.getStackTraceAddressesInternal()
