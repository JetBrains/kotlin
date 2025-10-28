/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

object FirJsPlainObjectsErrors : KtDiagnosticsContainer() {
    val NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED by error1<KtElement, String>()
    val ONLY_INTERFACES_ARE_SUPPORTED by error1<KtElement, String>()
    val IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED by error1<KtElement, String>()
    val METHODS_ARE_NOT_ALLOWED_INSIDE_JS_PLAIN_OBJECT by error0<KtElement>()
    val JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS by error1<KtElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDefaultErrorMessagesJsPlainObjects
}
