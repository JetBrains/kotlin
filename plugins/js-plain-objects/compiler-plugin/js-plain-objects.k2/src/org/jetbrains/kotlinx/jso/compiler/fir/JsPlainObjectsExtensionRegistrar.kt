/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlinx.jspo.compiler.fir.checkers.FirJsPlainObjectsCheckersComponent
import org.jetbrains.kotlinx.jspo.compiler.fir.services.JsPlainObjectsPropertiesProvider

class JsPlainObjectsExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FirJsPlainObjectsCheckersComponent
        +::JsPlainObjectsFunctionsGenerator
        // services
        +::JsPlainObjectsPropertiesProvider
    }
}