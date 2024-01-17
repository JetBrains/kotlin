/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

object KtDefaultErrorMessagesJsPlainObjects : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("JsPlainObjects").apply {
        put(
            FirJsPlainObjectsErrors.NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED,
            "Non-external {0} can not be annotated with JsPlainObjects. Only external interfaces are supported.",
            CommonRenderers.STRING
        )
        put(
            FirJsPlainObjectsErrors.ONLY_INTERFACES_ARE_SUPPORTED,
            "External {0} can not be annotated with JsPlainObjects. Only external interfaces are supported.",
            CommonRenderers.STRING
        )
        put(
            FirJsPlainObjectsErrors.IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED,
            "[{0}] is marked as JsPlainObjects, so, it can not be used as a super-type for non-JsPlainObjects declarations",
            CommonRenderers.STRING
        )
    }
}
