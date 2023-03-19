/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.generators.*
import java.io.File

class FirLombokRegistrar(private val lombokConfigFile: File?) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +LombokService.getFactory(lombokConfigFile)
        +::GetterGenerator
        +::SetterGenerator
        +::WithGenerator
        +::LombokConstructorsGenerator
        +::BuilderGenerator
        +::ValueFieldVisibilityTransformer
    }
}
