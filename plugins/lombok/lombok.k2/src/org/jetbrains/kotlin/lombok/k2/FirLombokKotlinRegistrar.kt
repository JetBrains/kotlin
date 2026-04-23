/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.lombok.k2.checkers.FirLombokCheckersExtension
import org.jetbrains.kotlin.lombok.k2.generators.LoggerGenerator
import org.jetbrains.kotlin.lombok.k2.generators.ToStringGenerator

class FirLombokKotlinRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::LoggerGenerator
        +::ToStringGenerator
        +::FirLombokCheckersExtension

        registerDiagnosticContainers(LombokFirDiagnostics)
    }
}
