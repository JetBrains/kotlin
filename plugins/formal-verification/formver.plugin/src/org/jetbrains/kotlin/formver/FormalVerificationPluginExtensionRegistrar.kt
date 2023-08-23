/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FormalVerificationPluginExtensionRegistrar(private val logLevel: LogLevel) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +PluginAdditionalCheckers.getFactory(logLevel)
    }
}

