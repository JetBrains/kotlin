/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

private val DIAGNOSTIC_FACTORY_TO_RENDERER by lazy {
    DiagnosticFactoryToRendererMap("Native").apply {

    }
}

class DefaultErrorMessagesNative : DefaultErrorMessages.Extension {
    override fun getMap(): DiagnosticFactoryToRendererMap = DIAGNOSTIC_FACTORY_TO_RENDERER
}
