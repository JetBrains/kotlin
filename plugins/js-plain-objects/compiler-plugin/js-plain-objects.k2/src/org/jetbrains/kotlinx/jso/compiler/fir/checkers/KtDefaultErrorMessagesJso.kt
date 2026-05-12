/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

object KtDefaultErrorMessagesJsPlainObjects : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("JsPlainObjects") { map ->
        map.put(
            FirJsPlainObjectsErrors.NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED,
            "Non-external {0} can not be annotated with JsPlainObject. Only external interfaces are supported.",
            CommonRenderers.STRING
        )
        map.put(
            FirJsPlainObjectsErrors.ONLY_INTERFACES_ARE_SUPPORTED,
            "External {0} can not be annotated with JsPlainObject. Only external interfaces are supported.",
            CommonRenderers.STRING
        )
        map.put(
            FirJsPlainObjectsErrors.IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED,
            "[{0}] is marked as JsPlainObject, it cannot be used as a supertype for non-JsPlainObject declarations.",
            CommonRenderers.STRING
        )
        map.put(
            FirJsPlainObjectsErrors.METHODS_ARE_NOT_ALLOWED_INSIDE_JS_PLAIN_OBJECT,
            "Methods are not allowed inside an interface marked with JsPlainObject.",
        )
        map.put(
            FirJsPlainObjectsErrors.JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS,
            "[{0}] is marked as JsPlainObject, so it can only contain other interfaces marked with JsPlainObject (or marker interfaces) as supertypes.",
            CommonRenderers.STRING
        )
    }
}
