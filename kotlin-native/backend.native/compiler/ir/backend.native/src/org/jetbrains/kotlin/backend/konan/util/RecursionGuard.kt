/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

class RecursionGuard {
    class RecursionBreachException(breadcrumb: Any) : Exception("$breadcrumb already encountered during recursion.")

    companion object {
        private val threadLocal = ThreadLocal<HashSet<Any>>()

        fun <T : Any, R> withBreadcrumb(obj: T, action: T.() -> R): R {
            if (hasBreadcrumb(obj)) {
                throw RecursionBreachException(obj)
            }

            storage.add(obj)

            try {
                return action(obj)
            } finally {
                storage.remove(obj)
            }
        }

        fun hasBreadcrumb(obj: Any): Boolean = storage.contains(obj)

        private val storage: HashSet<Any> = threadLocal.get() ?: HashSet<Any>().also { threadLocal.set(it) }
    }
}