/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.List

private fun getJavaVersion(): Int =
    System.getProperty("java.specification.version")?.substringAfter('.')?.toIntOrNull() ?: 6

fun isJava9OrLater() = getJavaVersion() >= 9


@Suppress("FunctionName")
fun TreeMaker.TopLevelJava9Aware(packageClause: JCTree.JCExpression?, declarations: List<JCTree>): JCTree.JCCompilationUnit {
    @Suppress("SpellCheckingInspection")
    return if (isJava9OrLater()) {
        val topLevelMethod = TreeMaker::class.java.declaredMethods.single { it.name == "TopLevel" }
        val packageDecl: JCTree? = packageClause?.let {
            val packageDeclMethod = TreeMaker::class.java.methods.single { it.name == "PackageDecl" }
            packageDeclMethod.invoke(this, List.nil<JCTree>(), packageClause) as JCTree
        }
        val allDeclarations = if (packageDecl != null) List.of(packageDecl) + declarations else declarations
        topLevelMethod.invoke(this, allDeclarations) as JCTree.JCCompilationUnit
    } else {
        TopLevel(List.nil(), packageClause, declarations)
    }
}

fun JCTree.JCCompilationUnit.getPackageNameJava9Aware(): JCTree? {
    return if (isJava9OrLater()) {
        JCTree.JCCompilationUnit::class.java.getDeclaredMethod("getPackageName").invoke(this) as JCTree?
    } else {
        this.packageName
    }
}
