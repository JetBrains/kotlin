/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration

import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration

/**
 * Helper method for making it easy to enable REPL functionality for the scripting host.
 *
 * @param fileExtension define which files are being treated as REPL snippets based on their
 * file extension. Use `null` to accept all files as REPL snippets.
 */
fun ScriptingHostConfiguration.Builder.configureDefaultRepl(fileExtension: String?) {
    repl {
        firReplHistoryProvider(FirReplHistoryProviderImpl())
        isReplSnippetSource { sourceFile, scriptSource ->
            fileExtension == null || (sourceFile?.name?.endsWith(".$fileExtension", ignoreCase = true) == true)
        }
    }
}
