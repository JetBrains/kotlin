/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ReplState
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirSnippetConfigurationExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirSnippetScopesExtensionImpl

class FirReplRegistrar(private val replState: ReplState): FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirSnippetConfigurationExtensionImpl.getFactory(replState)
        +FirSnippetScopesExtensionImpl.getFactory(replState)
    }
}