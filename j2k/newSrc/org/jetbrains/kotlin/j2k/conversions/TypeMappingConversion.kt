/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKClassType
import org.jetbrains.kotlin.j2k.tree.JKJavaPrimitiveType
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.JKType

class TypeMappingConversion : MatchBasedConversion() {
    override fun onElementChanged(new: JKTreeElement, old: JKTreeElement) {
        somethingChanged = true
    }

    override fun runConversion(treeRoot: JKTreeElement): Boolean {
        val root = applyToElement(treeRoot)
        assert(root === treeRoot)
        return somethingChanged
    }

    var somethingChanged = false


    fun applyToElement(element: JKTreeElement): JKTreeElement {
        return when (element) {
            is JKJavaPrimitiveType -> mapPrimitiveType(element)
            is JKClassType -> mapClassType(element)
            else -> applyRecursive(element, this::applyToElement)
        }
    }

    fun mapClassType(type: JKClassType): JKType {
//        if (type.classReference?.target) {
//
//        }
        return type
    }

    fun mapPrimitiveType(type: JKJavaPrimitiveType): JKType {
        return type
    }
}