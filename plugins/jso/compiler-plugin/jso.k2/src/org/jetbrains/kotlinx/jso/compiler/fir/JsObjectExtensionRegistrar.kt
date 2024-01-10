/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jso.compiler.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlinx.jso.compiler.fir.checkers.FirJsoCheckersComponent
import org.jetbrains.kotlinx.jso.compiler.fir.services.JsSimpleObjectPropertiesProvider

class JsObjectExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FirJsoCheckersComponent
        +::JsObjectFunctionsGenerator
        // services
        +::JsSimpleObjectPropertiesProvider
    }
}