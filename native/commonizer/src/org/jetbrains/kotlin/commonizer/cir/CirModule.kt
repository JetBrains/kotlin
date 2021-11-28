/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

interface CirModule : CirDeclaration, CirHasName {
    companion object {
        @Suppress("NOTHING_TO_INLINE")
        inline fun create(name: CirName): CirModule = CirModuleImpl(name)
    }
}

data class CirModuleImpl(
    override val name: CirName
) : CirModule
