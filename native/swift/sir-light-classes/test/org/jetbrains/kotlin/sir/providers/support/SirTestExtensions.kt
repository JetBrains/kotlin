/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.support

import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirVariable

val SirClass.superClassDeclaration: SirClass?
    get() = (superClass as? SirNominalType)?.typeDeclaration as? SirClass

fun List<SirDeclaration>.classNamed(className: String): SirClass? {
    return filterIsInstance<SirClass>()
        .firstOrNull { it.name == className }
}

fun List<SirDeclaration>.variableNamed(propertyName: String): SirVariable? {
    return filterIsInstance<SirVariable>()
        .firstOrNull { it.name == propertyName }
}
