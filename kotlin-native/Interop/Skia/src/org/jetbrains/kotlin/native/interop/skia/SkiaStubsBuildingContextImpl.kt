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

    override fun isCppClass(spelling: String): Boolean {
        val decl = nativeIndex.structs.firstOrNull { it.spelling == spelling } ?: return false
        return decl.def?.kind == StructDef.Kind.CLASS
    }

    override fun managedWrapperClassifier(cppClassifier: Classifier): Classifier? {
        if (cppClassifier.pkg != "org.jetbrains.skiko.skia.native") return null
        // TODO: it'd be nice to check inheritance from SkiaRefCnt or CPlusPlusClass,
        // but unable to do that at StubType level.
        if (!(cppClassifier.topLevelName.startsWith("Sk") || cppClassifier.topLevelName.startsWith("Gr"))) return null
        if (cppClassifier.topLevelName == "SkString") return null
        // TODO: We only managed C++ classes, not structs for now.
        if (!isCppClass(cppClassifier.topLevelName)) return null

        return Classifier.topLevel(cppClassifier.pkg, cppClassifier.topLevelName.drop(2))
    }
}