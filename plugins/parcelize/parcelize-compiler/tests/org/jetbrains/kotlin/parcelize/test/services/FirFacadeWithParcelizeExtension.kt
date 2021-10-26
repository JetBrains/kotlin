/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.parcelize.fir.FirParcelizeExtensionRegistrar
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade

val FirFacadeWithParcelizeExtension: Constructor<FirFrontendFacade> = { testServices ->
    FirFrontendFacade(testServices) {
        it.registerExtensions(FirParcelizeExtensionRegistrar().configure())
    }
}
