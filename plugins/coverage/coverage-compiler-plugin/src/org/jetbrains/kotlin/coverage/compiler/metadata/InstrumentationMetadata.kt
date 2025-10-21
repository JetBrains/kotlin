/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage.compiler.metadata

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile


internal class ModuleIM {
    private val _files = mutableListOf<FileIM>()
    val files: List<FileIM> = _files

    fun addFile(path: String, packageName: String): FileIM {
        val file = FileIM(_files.size, path, packageName)
        _files.add(file)
        return file
    }
}

internal class FileIM(
    val number: Int,
    val path: String,
    val packageName: String,
) : DeclarationContainerIM() {
    override val parent: DeclarationContainerIM? = null
}

internal sealed class DeclarationContainerIM {
    abstract val parent: DeclarationContainerIM?

    private val _declarations = mutableListOf<DeclarationIM>()
    val declarations: List<DeclarationIM> = _declarations

    open fun addFunction(
        name: String,
        params: List<String>,
        returnType: String,
        range: PositionRange,
    ): FunctionIM {
        return FunctionIM(name, params, returnType, range, this).also { functionIM -> _declarations.add(functionIM) }
    }

    open fun addClass(
        name: String,
        isCompanion: Boolean,
        range: PositionRange,
    ): ClassIM {
        return ClassIM(name, isCompanion, range, this).also { _declarations.add(it) }
    }

    open fun addProperty(
        name: String,
        isVar: Boolean,
        isConst: Boolean,
        range: PositionRange,
    ): PropertyIM {
        return PropertyIM(name, isVar, isConst, range, this).also { _declarations.add(it) }
    }
}

internal sealed class DeclarationIM : DeclarationContainerIM() {
    abstract val range: PositionRange
}

internal class ClassIM(
    val name: String,
    val isCompanion: Boolean,
    override val range: PositionRange,
    override val parent: DeclarationContainerIM,
) : DeclarationIM() {
    val isLocal: Boolean get() = parent is FunctionIM
}

internal class PropertyIM(
    val name: String,
    val isVar: Boolean,
    val isConst: Boolean,
    override val range: PositionRange,
    override val parent: DeclarationContainerIM,
) : DeclarationIM() {

    // TODO add initializer, getter and setter

    override fun addProperty(
        name: String,
        isVar: Boolean,
        isConst: Boolean,
        range: PositionRange,
    ): PropertyIM {
        throw UnsupportedOperationException("Property cannot have classes")
    }

    override fun addClass(
        name: String,
        isCompanion: Boolean,
        range: PositionRange,
    ): ClassIM {
        throw UnsupportedOperationException("Property cannot have properties")
    }
}

internal class FunctionIM(
    val name: String,
    val params: List<String>,
    val returnType: String,
    override val range: PositionRange,
    override val parent: DeclarationContainerIM,
) : DeclarationIM() {
    val isLocal: Boolean get() = parent is FunctionIM
    var body: BodyIM? = null
}

internal sealed interface BodyIM


internal class Position(val line: Int, val column: Int) {
    override fun toString(): String {
        return "$line:$column"
    }
}

internal class PositionRange(val start: Position, val end: Position) {
    override fun toString(): String {
        return "${start.line}:${start.column}-${end.line}:${end.column}"
    }
}

internal fun IrFile.positionRange(element: IrElement): PositionRange {
    return PositionRange(position(element.startOffset), position(element.endOffset))
}

internal fun IrFile.position(offset: Int): Position {
    return Position(
        fileEntry.getLineNumber(offset),
        fileEntry.getColumnNumber(offset)
    )
}