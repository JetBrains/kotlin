/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.util

import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.JCAnnotation
import com.sun.tools.javac.tree.JCTree.JCClassDecl
import com.sun.tools.javac.tree.JCTree.JCTypeParameter
import com.sun.tools.javac.tree.JCTree.JCVariableDecl
import com.sun.tools.javac.tree.Pretty
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.List as JavacList
import org.jetbrains.kotlin.kapt.base.util.isJava11OrLater
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.METADATA_FQ_NAME
import java.io.StringWriter
import java.io.Writer

private class PrettyWithWorkarounds(
    private val context: Context, private val out: Writer, sourceOutput: Boolean,
    private val renderMetadata: ((Pretty, JCAnnotation) -> String)?,
) : Pretty(out, sourceOutput) {
    companion object {
        private const val ENUM = Flags.ENUM.toLong()

        // Flags.RECORD, absent from the JDK 11 API kapt is compiled against
        private const val RECORD = 1L shl 61
    }

    // Pretty prints a record as a class with its components as fields, so the record header is printed here
    private var pendingRecord: JCClassDecl? = null
    private var pendingRecordComponents: List<JCVariableDecl> = emptyList()

    override fun print(s: Any) {
        val record = pendingRecord
        // JDK 21+ prints "class " and the name separately, JDK 17 in one call
        val text = s.toString()
        out.write(if (record != null && (text == "class " || text == "class ${record.name}")) "record" + text.removePrefix("class") else text)
    }

    override fun visitClassDef(tree: JCClassDecl) {
        if ((tree.mods.flags and RECORD) == 0L) return super.visitClassDef(tree)

        val defs = tree.defs
        val [components, members] = defs.partition { it is JCVariableDecl && (it.mods.flags and RECORD) != 0L }
        pendingRecord = tree
        @Suppress("UNCHECKED_CAST")
        pendingRecordComponents = components as List<JCVariableDecl>
        tree.defs = JavacList.from(members)
        try {
            super.visitClassDef(tree)
        } finally {
            tree.defs = defs
            pendingRecord = null
        }
    }

    override fun printTypeParameters(trees: JavacList<JCTypeParameter>) {
        super.printTypeParameters(trees)
        if (trees !== pendingRecord?.typarams) return
        pendingRecord = null

        print("(")
        pendingRecordComponents.forEachIndexed { index, component ->
            if (index > 0) print(", ")
            printExpr(TreeMaker.instance(context).Modifiers(0, component.mods.annotations))
            val type = component.vartype
            if ((component.mods.flags and Flags.VARARGS) != 0L && type is JCTree.JCArrayTypeTree) {
                printExpr(type.elemtype)
                print("...")
            } else {
                printExpr(type)
            }
            print(" ")
            print(component.name)
        }
        print(")")
    }

    override fun visitVarDef(tree: JCVariableDecl) {
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

    override fun visitAnnotation(tree: JCAnnotation) {
        if (renderMetadata != null && tree.annotationType.toString() == METADATA_FQ_NAME.asString()) {
            print(renderMetadata.invoke(this, tree))
        }
        print("@")
        printExpr(tree.annotationType)
        print("(")
        printExprs(tree.args)
        print(")")
    }
}

fun JCTree.prettyPrint(context: Context, renderMetadata: ((Pretty, JCAnnotation) -> String)? = null): String {
    return StringWriter().apply { PrettyWithWorkarounds(context, this, false, renderMetadata).printStat(this@prettyPrint) }.toString()
}
