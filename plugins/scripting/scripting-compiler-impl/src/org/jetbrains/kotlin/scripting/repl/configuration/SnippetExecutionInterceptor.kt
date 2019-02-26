/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.configuration

interface SnippetExecutionInterceptor {
    fun <T> execute(block: () -> T): T

    companion object Plain : SnippetExecutionInterceptor {
        override fun <T> execute(block: () -> T) = block()
    }
}
