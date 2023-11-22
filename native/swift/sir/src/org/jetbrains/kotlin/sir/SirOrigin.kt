/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SirOrigin

interface SirDeclarationOrigin : SirOrigin

class SirBuiltinDeclarationOrigin : SirDeclarationOrigin

interface SirFunctionOrigin : SirDeclarationOrigin

class DummyFunctionOrigin : SirFunctionOrigin

interface KotlinSirOrigin : SirOrigin

interface KotlinDeclarationSirOrigin : SirOrigin {
    val fqName: List<String>
}

interface KotlinFunctionSirOrigin : KotlinDeclarationSirOrigin, SirFunctionOrigin {
    val returnType: SirType

    val parameters: List<SirParameter>
}

interface SyntheticSirOrigin : SirOrigin

interface SyntheticFunctionSirOrigin : SirFunctionOrigin