/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.facts.Fact
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.fileOrNull

/**
 * [PropertyAccessorInlineLowering] that records, for every file the initializer of a const property is copied into,
 * the use of that initializer: after the lowering the IR references no declaration whose change would invalidate
 * the file, while the compiled bitcode keeps the copied value.
 */
internal class KonanPropertyAccessorInlineLowering(
        private val generationState: NativeGenerationState,
) : PropertyAccessorInlineLowering(generationState.context) {

    override fun onConstPropertyInlined(property: IrProperty, backingField: IrField, container: IrDeclaration) {
        // Same-file uses are skipped: a file is invalidated by its own defs anyway.
        val callSiteFile = container.fileOrNull ?: return
        if (property.fileOrNull == null || property.fileOrNull == callSiteFile) return

        generationState.context.factsTracker.uses(callSiteFile, Fact.ConstInitializer.from(property))
    }
}
