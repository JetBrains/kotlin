/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

/**
 * A marker interface that denotes Swift IR elements.
 */
interface SirElement {
}

class SirModule : SirElement {
    val declarations: MutableList<SirDeclaration> =
        mutableListOf()
}

interface SirDeclaration

class SirForeignFunction(val fqName: List<String>) : SirDeclaration
