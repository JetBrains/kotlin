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
import java.lang.reflect.Method

private val javaVersion: Int =
    System.getProperty("java.specification.version")?.substringAfter('.')?.toIntOrNull() ?: 6

fun isJava9OrLater() = javaVersion >= 9
fun isJava11OrLater() = javaVersion >= 11
fun isJava17OrLater() = javaVersion >= 17

private val topLevelMethod: Method by lazy {
    TreeMaker::class.java.declaredMethods.single { it.name == "TopLevel" }
}

private val packageDeclMethod: Method by lazy {
    TreeMaker::class.java.methods.single { it.name == "PackageDecl" }
}

private val getPackageNameMethod: Method by lazy {
    JCTree.JCCompilationUnit::class.java.getDeclaredMethod("getPackageName")
}

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
    return if (isJava9OrLater()) {
        val packageDecl: JCTree? = packageClause?.let {
            packageDeclMethod.invoke(this, JavacList.nil<JCTree>(), packageClause) as JCTree
        }
        val allDeclarations = if (packageDecl != null) JavacList.of(packageDecl) + declarations else declarations
        topLevelMethod.invoke(this, allDeclarations) as JCTree.JCCompilationUnit
    } else {
        topLevelMethod.invoke(this, JavacList.nil<JCTree.JCAnnotation>(), packageClause, declarations) as JCTree.JCCompilationUnit
    }
}

// The cast is not useless on JDK 21
@Suppress("USELESS_CAST")
fun JCTree.JCCompilationUnit.getPackageNameJava9Aware(): JCTree? {
    return if (isJava9OrLater()) {
        getPackageNameMethod.invoke(this) as JCTree?
    } else {
        this.packageName as JCTree?
    }
}
