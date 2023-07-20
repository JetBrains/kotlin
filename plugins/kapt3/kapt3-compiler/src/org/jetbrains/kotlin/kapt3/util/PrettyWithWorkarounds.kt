/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.util

import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.Pretty
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.kapt3.base.util.isJava11OrLater
import java.io.StringWriter
import java.io.Writer

class PrettyWithWorkarounds(private val context: Context, val out: Writer, sourceOutput: Boolean) : Pretty(out, sourceOutput) {
    companion object {
        private const val ENUM = Flags.ENUM.toLong()
    }

    override fun print(s: Any) {
        out.write(s.toString())
    }

    override fun visitVarDef(tree: JCTree.JCVariableDecl) {
        if ((tree.mods.flags and ENUM) != 0L) {
            // Pretty does not print annotations for enum values for some reason
            printExpr(TreeMaker.instance(context).Modifiers(0, tree.mods.annotations))

            if (isJava11OrLater()) {
                // Print enums fully, there is an issue when using Pretty in JDK 11.
                // See https://youtrack.jetbrains.com/issue/KT-33052.
                print("/*public static final*/ ${tree.name}")
                tree.init?.let { print(" /* = $it */") }
                return
            }
        }
        super.visitVarDef(tree)
    }
}

fun JCTree.prettyPrint(context: Context): String {
    return StringWriter().apply { PrettyWithWorkarounds(context, this, false).printStat(this@prettyPrint) }.toString()
}