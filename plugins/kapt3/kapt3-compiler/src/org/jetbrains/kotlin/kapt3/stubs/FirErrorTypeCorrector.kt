/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.stubs

import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.kapt3.javac.KaptTreeMaker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name.isValidIdentifier
import org.jetbrains.org.objectweb.asm.Type

class FirErrorTypeCorrector(firFile: FirFile, val treeMaker: KaptTreeMaker) {
    val aliasToFqName: Map<String, FqName>

    init {
        val aliasToFqName = mutableMapOf<String, FqName>()
        for (importDeclaration in firFile.imports) {
            val importedFqName = importDeclaration.importedFqName ?: continue
            if (importedFqName.pathSegments().any { !isValidIdentifier(it.asString()) }) continue
            if (importDeclaration.aliasName != null) {
                aliasToFqName[importDeclaration.aliasName!!.asString()] = importedFqName
            }
        }
        this.aliasToFqName = aliasToFqName
    }

    fun convertType(type: String): JCTree.JCExpression {
        val realType = aliasToFqName[type]?.asType() ?: Type.getObjectType(type.replace(".", "/"))
        return treeMaker.Type(realType)
    }

    private fun FqName.asType(): Type = Type.getObjectType(pathSegments().joinToString("/"))
}