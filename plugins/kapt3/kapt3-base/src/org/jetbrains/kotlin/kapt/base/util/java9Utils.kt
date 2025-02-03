/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.util

import com.sun.tools.javac.main.Option
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Options
import com.sun.tools.javac.util.List as JavacList
import org.jetbrains.kotlin.kapt.base.plus

private fun getJavaVersion(): Int =
    System.getProperty("java.specification.version")?.substringAfter('.')?.toIntOrNull() ?: 6

fun isJava9OrLater() = getJavaVersion() >= 9
fun isJava11OrLater() = getJavaVersion() >= 11
fun isJava17OrLater() = getJavaVersion() >= 17

fun Options.putJavacOption(jdk8Name: String, jdk9Name: String, value: String) {
    val option = if (isJava9OrLater()) {
        Option.valueOf(jdk9Name)
    } else {
        Option.valueOf(jdk8Name)
    }

    put(option, value)
}

@Suppress("FunctionName")
fun TreeMaker.TopLevelJava9Aware(packageClause: JCTree.JCExpression?, declarations: JavacList<JCTree>): JCTree.JCCompilationUnit {
    @Suppress("SpellCheckingInspection")
    return if (isJava9OrLater()) {
        val topLevelMethod = TreeMaker::class.java.declaredMethods.single { it.name == "TopLevel" }
        val packageDecl: JCTree? = packageClause?.let {
            val packageDeclMethod = TreeMaker::class.java.methods.single { it.name == "PackageDecl" }
            packageDeclMethod.invoke(this, JavacList.nil<JCTree>(), packageClause) as JCTree
        }
        val allDeclarations = if (packageDecl != null) JavacList.of(packageDecl) + declarations else declarations
        topLevelMethod.invoke(this, allDeclarations) as JCTree.JCCompilationUnit
    } else {
        val topLevelMethod = TreeMaker::class.java.declaredMethods.single { it.name == "TopLevel" }
        topLevelMethod.invoke(this, JavacList.nil<JCTree.JCAnnotation>(), packageClause, declarations) as JCTree.JCCompilationUnit
    }
}

// The cast is not useless on JDK 21
@Suppress("USELESS_CAST")
fun JCTree.JCCompilationUnit.getPackageNameJava9Aware(): JCTree? {
    return if (isJava9OrLater()) {
        JCTree.JCCompilationUnit::class.java.getDeclaredMethod("getPackageName").invoke(this) as JCTree?
    } else {
        this.packageName as JCTree?
    }
}