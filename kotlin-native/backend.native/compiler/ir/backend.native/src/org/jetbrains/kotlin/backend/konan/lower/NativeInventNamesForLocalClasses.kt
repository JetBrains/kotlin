/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.InventNamesForLocalClasses
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.name.Name

internal var IrClass.hasSyntheticNameToBeHiddenInReflection by irFlag(followAttributeOwner = true)

// TODO: consider replacing '$' by another delimeter that can't be used in class name specified with backticks (``)
internal class NativeInventNamesForLocalClasses(val generationState: NativeGenerationState) : InventNamesForLocalClasses() {
    override fun computeTopLevelClassName(clazz: IrClass): String = clazz.name.asString()
    override fun sanitizeNameIfNeeded(name: String) = name

    override fun customizeNameInventorData(clazz: IrClass, data: NameBuilder): NameBuilder {
        if (!clazz.isAnonymousObject) return data
        val customEnclosingName = (clazz.parent as? IrFile)?.packagePartClassName ?: return data
        return NameBuilder(currentName = customEnclosingName, isLocal = true, processingInlinedFunction = data.processingInlinedFunction)
    }

    override fun putLocalClassName(declaration: IrElement, localClassName: String) {
        if (declaration is IrClass) {
            if (declaration.isAnonymousObject) {
                declaration.hasSyntheticNameToBeHiddenInReflection = true
            }
            declaration.name = Name.identifier(localClassName)
        }
    }
}
