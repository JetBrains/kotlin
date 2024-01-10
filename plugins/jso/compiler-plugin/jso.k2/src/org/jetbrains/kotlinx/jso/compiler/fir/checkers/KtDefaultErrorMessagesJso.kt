/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jso.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

object KtDefaultErrorMessagesJso : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("JsSimpleObject").apply {
        put(
            FirJsoErrors.NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED,
            "Non-external {0} can not be annotated with JsSimpleObject. Only external interfaces are supported.",
            CommonRenderers.STRING
        )
        put(
            FirJsoErrors.ONLY_INTERFACES_ARE_SUPPORTED,
            "External {0} can not be annotated with JsSimpleObject. Only external interfaces are supported.",
            CommonRenderers.STRING
        )
        put(
            FirJsoErrors.IMPLEMENTING_OF_JSO_IS_NOT_SUPPORTED,
            "[{0}] is marked as JsSimpleObject, so, it can not be used as a super-type for non-JsSimpleObject declarations",
            CommonRenderers.STRING
        )
    }
}
