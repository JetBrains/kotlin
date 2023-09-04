/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.InventNamesForLocalClasses
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass

// TODO: consider replacing '$' by another delimeter that can't be used in class name specified with backticks (``)
internal class NativeInventNamesForLocalClasses(val generationState: NativeGenerationState) : InventNamesForLocalClasses(
        allowTopLevelCallables = true,
        generateNamesForRegeneratedObjects = true
) {
    override fun computeTopLevelClassName(clazz: IrClass): String = clazz.name.asString()
    override fun sanitizeNameIfNeeded(name: String) = name

    override fun putLocalClassName(declaration: IrAttributeContainer, localClassName: String) {
        generationState.putLocalClassName(declaration, localClassName)
    }
}
