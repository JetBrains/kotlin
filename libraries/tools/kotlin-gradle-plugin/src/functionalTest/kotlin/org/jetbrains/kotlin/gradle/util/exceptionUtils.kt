/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.internal.exceptions.MultiCauseException

val Throwable.allCauses: Set<Throwable>
    get() {
        val result = mutableSetOf<Throwable>()
        val queue = ArrayDeque<Throwable>()
        queue.add(this)

        while (queue.isNotEmpty()) {
            val error = queue.removeFirst()
            if (!result.add(error)) continue

            if (error is MultiCauseException) {
                queue.addAll(error.causes)
            } else {
                if (error.cause != null) queue.addLast(error.cause!!)
            }
        }

        return result
    }