/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.coverage.compiler.metadata

import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import java.io.File

internal fun ModuleIM.writeToFile(file: File) {
    file.parentFile.mkdirs()
    file.bufferedWriter().use { out ->
        out.writeModule(this)
    }
}

private fun Appendable.writeModule(moduleIM: ModuleIM) {
    moduleIM.files.forEach { fileIM ->
        appendLine(fileIM.path)
        appendLine(fileIM.packageName)
        writeDeclarations(SINGLE_INDENT, fileIM)
    }
}

private fun Appendable.writeDeclarations(indent: String, containerIM: DeclarationContainerIM) {
    containerIM.declarations.forEach { declarationIM ->
        when (declarationIM) {
            is ClassIM -> writeClass(indent, declarationIM)
            is FunctionIM -> writeFunction(indent, declarationIM)
            is PropertyIM -> writeProperty(indent, declarationIM)
            else -> error("Unknown declaration type: ${declarationIM::class.simpleName}")
        }
    }
}

private fun Appendable.writeProperty(indent: String, propertyIM: PropertyIM) {
    append(indent)
    if (propertyIM.isConst) append("const ")
    if (propertyIM.isVar) append("var ") else append("val ")
    appendLine(propertyIM.name)
    appendLine("$indent[${propertyIM.range}]")
    writeDeclarations("$indent    ", propertyIM)
}

private fun Appendable.writeClass(indent: String, classIM: ClassIM) {
    append(indent)
    if (classIM.isCompanion) append("companion object ") else append("class ")
    appendLine(classIM.name)
    appendLine("$indent[${classIM.range}]")
    writeDeclarations("$indent    ", classIM)
}

private fun Appendable.writeFunction(indent: String, functionIM: FunctionIM) {
    appendLine("${indent}fun ${functionIM.name}(${functionIM.params}): ${functionIM.returnType}")
    val nextIndent = indent + SINGLE_INDENT
    appendLine("${nextIndent}range: ${functionIM.range}")
    writeBody(nextIndent, functionIM.body)
    writeDeclarations(nextIndent, functionIM)
}


private fun Appendable.writeBody(indent: String, body: BodyIM?) {
    append("${indent}body: ")
    if (body != null && body is LineBranchBodyIM) {
        body.lines.forEach { line ->
            append("${line.pointId}=${line.lineNumber}:${line.columnStart}[")

            append("];")
        }
    } else {
        append("null")
    }

    appendLine()
}


const val SINGLE_INDENT = "    "
