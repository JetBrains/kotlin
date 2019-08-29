/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3.base.util

import com.sun.tools.javac.main.Option
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Options
import com.sun.tools.javac.util.List as JavacList
import org.jetbrains.kotlin.kapt3.base.plus

fun isJava9OrLater(): Boolean = !System.getProperty("java.version").startsWith("1.")
fun isJava11OrLater(): Boolean {
    val majorVersion = System.getProperty("java.version").substringBefore(".", "")
    if (majorVersion.isEmpty()) return false

    return try {
        majorVersion.toInt() >= 11
    } catch (ignored: Throwable) {
        false
    }
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
        TopLevel(JavacList.nil(), packageClause, declarations)
    }
}

fun JCTree.JCCompilationUnit.getPackageNameJava9Aware(): JCTree? {
    return if (isJava9OrLater()) {
        JCTree.JCCompilationUnit::class.java.getDeclaredMethod("getPackageName").invoke(this) as JCTree?
    } else {
        this.packageName
    }
}