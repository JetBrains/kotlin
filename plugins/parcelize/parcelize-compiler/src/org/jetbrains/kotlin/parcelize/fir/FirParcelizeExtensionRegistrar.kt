/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirParcelizeExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FirParcelizeDeclarationGenerator
        +::FirParcelizeCheckersExtension
    }
}
