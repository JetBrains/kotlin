/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKJavaMethodImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKTypeElementImpl

class JavaStandartMethodsConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaMethodImpl) return recurse(element)
        if (fixToStringMethod(element)) {
            return recurse(element)
        }
        return recurse(element)
    }

    private fun fixToStringMethod(method: JKJavaMethodImpl): Boolean {
        if (method.name.value != "toString") return false
        if (method.parameters.isNotEmpty()) return false
        val type = (method.returnType.type as? JKClassType)
            ?.takeIf { it.classReference.name == "String" }
            ?.updateNullability(Nullability.NotNull) ?: return false
        method.returnType = JKTypeElementImpl(type)
        return true
    }
}