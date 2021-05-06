/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.skia

import org.jetbrains.kotlin.native.interop.gen.Classifier
import org.jetbrains.kotlin.native.interop.gen.StubIrContext
import org.jetbrains.kotlin.native.interop.gen.StubsBuildingContextImpl
import org.jetbrains.kotlin.native.interop.indexer.StructDecl
import org.jetbrains.kotlin.native.interop.indexer.StructDef

class SkiaStubsBuildingContextImpl(stubIrContext: StubIrContext) : StubsBuildingContextImpl(stubIrContext) {
    override val declarationMapper = SkiaDeclarationMapperImpl()

    inner class SkiaDeclarationMapperImpl : DeclarationMapperImpl() {
        override fun getKotlinClassForManaged(structDecl: StructDecl): Classifier {
            assert(structDecl.isSkiaSharedPointer)
            val struct = structDecl.stripSkiaSharedPointer
            val structArgument = nativeIndex.structs.singleOrNull {
                it.spelling == struct && it.def != null
            } ?: error("Expected to find a single template arg struct by name: ${struct}")
            return getKotlinClassForPointed(structArgument)
        }
    }
}