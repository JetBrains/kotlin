/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

object RhizomedbDefaultErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("Rhizomedb").apply {
        put(
            RhizomedbFirErrors.MANY_ATTRIBUTE_NOT_A_SET,
            "Property marked as @Many should be a Set",
        )
    }
}
