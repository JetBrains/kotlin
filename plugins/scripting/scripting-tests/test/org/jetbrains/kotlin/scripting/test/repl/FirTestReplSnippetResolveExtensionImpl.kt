/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.repl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.extensions.FirReplSnippetResolveExtension
import org.jetbrains.kotlin.fir.scopes.FirScope
import kotlin.script.experimental.host.ScriptingHostConfiguration

class FirTestReplSnippetResolveExtensionImpl(
    session: FirSession,
    // TODO: left here because it seems it will be needed soon, remove supression if used or remove the param if it is not the case
    @Suppress("UNUSED_PARAMETER", "unused") hostConfiguration: ScriptingHostConfiguration,
) : FirReplSnippetResolveExtension(session) {

    override fun getSnippetScope(snippet: FirReplSnippet): FirScope? {
        return null
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirTestReplSnippetResolveExtensionImpl(session, hostConfiguration) }
        }
    }
}