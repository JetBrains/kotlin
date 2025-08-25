/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.support

import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirEnum
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirVariable

val SirClass.superClassDeclaration: SirClass?
    get() = superClass?.typeDeclaration as? SirClass

fun List<SirDeclaration>.classNamed(className: String): SirClass {
    return filterIsInstance<SirClass>()
        .firstOrNull { it.name == className }
        ?: error("Class $className not found")
}

fun List<SirDeclaration>.enumNamed(enumName: String): SirEnum {
    return filterIsInstance<SirEnum>()
        .firstOrNull { it.name == enumName }
        ?: error("Class $enumName not found")
}

fun List<SirDeclaration>.protocolNamed(protocolName: String): SirProtocol {
    return filterIsInstance<SirProtocol>()
        .firstOrNull { it.name == protocolName }
        ?: error("Protocol $protocolName not found")
}

fun List<SirDeclaration>.variableNamed(variableName: String): SirVariable {
    return filterIsInstance<SirVariable>()
        .firstOrNull { it.name == variableName }
        ?: error("Variable $variableName not found")
}

fun List<SirDeclaration>.functionsNamed(functionName: String): List<SirFunction> {
    return filterIsInstance<SirFunction>()
        .filter { it.name == functionName }
}
