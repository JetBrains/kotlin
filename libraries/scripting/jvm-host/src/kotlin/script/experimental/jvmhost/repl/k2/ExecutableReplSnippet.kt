/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl.k2

/**
 * Interface for all REPL snippets, that makes it easier to inject the ReplState.
 *
 * TODO Should it be `$replState` to make it explicit that it is an internal variable and prevent potential clashes?
 */
interface ExecutableReplSnippet {
    suspend fun execute(replState: ReplState)
}
