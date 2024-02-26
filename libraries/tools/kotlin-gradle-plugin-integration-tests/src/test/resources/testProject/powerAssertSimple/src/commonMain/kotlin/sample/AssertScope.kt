/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2020-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package sample

import kotlin.contracts.*

typealias LazyMessage = () -> Any

interface AssertScope {
    fun assert(assertion: Boolean, lazyMessage: LazyMessage? = null)
}

@OptIn(ExperimentalContracts::class)
fun assert(assertion: Boolean, lazyMessage: LazyMessage? = null) {
    contract { returns() implies assertion }
    if (!assertion) {
        throw AssertionError(lazyMessage?.invoke()?.toString())
    }
}

private class SoftAssertScope : AssertScope {
    private val assertions = mutableListOf<Throwable>()

    override fun assert(assertion: Boolean, lazyMessage: LazyMessage?) {
        if (!assertion) {
            assertions.add(AssertionError(lazyMessage?.invoke()?.toString()))
        }
    }

    fun close(exception: Throwable? = null) {
        if (assertions.isNotEmpty()) {
            val base = exception ?: AssertionError("Multiple failed assertions")
            for (assertion in assertions) {
                base.addSuppressed(assertion)
            }
            throw base
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun <R> assertSoftly(block: AssertScope.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    val scope = SoftAssertScope()
    val result = runCatching { scope.block() }
    scope.close(result.exceptionOrNull())
    return result.getOrThrow()
}
