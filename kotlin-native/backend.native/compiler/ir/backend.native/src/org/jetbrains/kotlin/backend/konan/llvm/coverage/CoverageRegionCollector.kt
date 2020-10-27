/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.llvm.coverage

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Collect all regions in the module.
 * @param fileFilter filters files that should be processed.
 */
internal class CoverageRegionCollector(private val fileFilter: (IrFile) -> Boolean) {

    fun collectFunctionRegions(irModuleFragment: IrModuleFragment): List<FileRegionInfo> =
            irModuleFragment.files
                    .filter(fileFilter)
                    .map { file ->
                        val collector = FunctionsCollector(file)
                        collector.visitFile(file)
                        FileRegionInfo(file, collector.functionRegions)
                    }

    private inner class FunctionsCollector(val file: IrFile) : IrElementVisitorVoid {

        val functionRegions = mutableListOf<FunctionRegions>()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFunction(declaration: IrFunction) {
            if (!declaration.isInline && !declaration.isExternal && !declaration.isGeneratedByCompiler) {
                val regionsCollector = IrFunctionRegionsCollector(fileFilter, file)
                declaration.acceptVoid(regionsCollector)
                if (regionsCollector.regions.isNotEmpty()) {
                    functionRegions += FunctionRegions(declaration, regionsCollector.regions)
                }
            }
            // TODO: Decide how to work with local functions. Should they be process separately?
            declaration.acceptChildrenVoid(this)
        }
    }
}

// User doesn't bother about compiler-generated declarations.
// So lets filter them.
private val IrDeclaration.isGeneratedByCompiler: Boolean
    get() {
        return origin != IrDeclarationOrigin.DEFINED || nameForIrSerialization.asString() == "Konan_start"
    }

/**
 * Very similar to [org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor] but instead of bitcode generation we collect regions.
 * [fileFilter]: specify which files should be processed by code coverage. Here it is required
 * for checking calls to inline functions from other files.
 * TODO: for now it is very inaccurate.
 */
private class IrFunctionRegionsCollector(
        val fileFilter: (IrFile) -> Boolean,
        val irFile: IrFile
) : IrElementVisitorVoid {

    private data class StatementContext(val current: IrStatement, val next: IrStatement?)

    val regions = mutableMapOf<IrElement, Region>()

    private val irFileStack = mutableListOf(irFile)

    private val regionStack = mutableListOf<Region>()

    private val irStatementsStack = mutableListOf<StatementContext>()

    private val currentFile: IrFile
        get() = irFileStack.last()

    private val currentRegion: Region
        get() = regionStack.last()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.body?.let { body ->
            visitInRegionContext(recordRegion(body) ?: return) {
                body.acceptVoid(this)
            }
        }
    }

    override fun visitConstructor(declaration: IrConstructor) {
        val statements = declaration.body?.statements ?: return
        visitInStatementContext(statements) { statement ->
            if (statement is IrDelegatingConstructorCall && !declaration.isPrimary
                    || statement !is IrDelegatingConstructorCall && statement !is IrReturn) {
                recordRegion(statement)
                statement.acceptVoid(this)
            }
        }
    }

    override fun visitBody(body: IrBody) = visitInStatementContext(body.statements)

    override fun visitContainerExpression(expression: IrContainerExpression) {
        val statements = expression.statements
        when (expression) {
            is IrReturnableBlock -> {
                val file = expression.sourceFileSymbol?.owner
                if (file != null && file != currentFile && fileFilter(file)) {
                    recordRegion(expression)
                    visitInFileContext(file) {
                        visitInStatementContext(statements)
                    }
                }
            }
            else -> visitInStatementContext(statements)
        }
    }

    // TODO: The following implementation produces correct region mapping, but something goes wrong later
    // override fun visitFieldAccess(expression: IrFieldAccessExpression) {
    //     expression.receiver?.let { recordRegion(it) }
    //     expression.acceptChildrenVoid(this)
    // }

    override fun visitWhen(expression: IrWhen) {
        val branches = expression.branches
        branches.forEach {
            val condition = it.condition
            val result = it.result

            if (condition is IrConst<*> && condition.value == true && condition.endOffset == result.endOffset) {
                // Probably an 'else' branch.
                // Note: can't rely on [IrElseBranch], because IR deserializer doesn't emit it.
                recordRegion(result)
            } else {
                recordRegion(condition)
                recordRegion(result, condition.endOffset, result.endOffset)
                condition.acceptVoid(this)
            }
            result.acceptVoid(this)
        }
    }

    override fun visitLoop(loop: IrLoop) {
        val condition = loop.condition
        recordRegion(condition)
        condition.acceptVoid(this)

        val body = loop.body ?: return
        when (loop) {
            is IrWhileLoop -> recordRegion(body, condition.endOffset, body.endOffset)
            is IrDoWhileLoop -> recordRegion(body, body.startOffset, condition.startOffset)
        }
        body.acceptVoid(this)
    }

    override fun visitBreakContinue(jump: IrBreakContinue) {
        val (current, next) = irStatementsStack.lastOrNull() ?: return
        recordRegion(next ?: return, current.endOffset, jump.loop.endOffset)
    }

    override fun visitReturn(expression: IrReturn) {
        irStatementsStack.subList(0, irStatementsStack.lastIndex)
                .filter { (current, next) -> next != null && current.endOffset > expression.endOffset }
                .forEach { (current, next) -> recordRegion(next!!, expression.endOffset, current.endOffset) }
        val next = irStatementsStack.lastOrNull()?.next ?: return
        val nextRegion = recordRegion(next, expression.endOffset, currentRegion.endOffset) ?: return
        regionStack.pop()
        regionStack.push(nextRegion)
    }

    private fun visitInFileContext(file: IrFile, visit: () -> Unit) {
        irFileStack.push(file)
        visit()
        irFileStack.pop()
    }

    private fun visitInRegionContext(region: Region, visit: () -> Unit) {
        regionStack.push(region)
        visit()
        regionStack.pop()
    }

    private fun visitInStatementContext(
            statements: List<IrStatement>,
            visit: (IrStatement) -> Unit = { statement -> statement.acceptVoid(this) }
    ) {
        for (i in 0..statements.lastIndex) {
            val current = statements[i]
            if (!current.hasValidOffsets()) {
                continue
            }
            val nextInContext = irStatementsStack.lastOrNull()?.next
            val next = if (i < statements.lastIndex && statements[i + 1].hasValidOffsets()) statements[i + 1] else nextInContext
            irStatementsStack.push(StatementContext(current, next))
            visit(current)
            irStatementsStack.pop()
        }
    }

    private fun recordRegion(irElement: IrElement, kind: RegionKind = RegionKind.Code)
            = Region.fromIr(irElement, currentFile, kind)?.also { regions[irElement] = it }

    private fun recordRegion(irElement: IrElement, startOffset: Int, endOffset: Int, kind: RegionKind = RegionKind.Code)
            = Region.fromOffset(startOffset, endOffset, currentFile, kind)?.also { regions[irElement] = it }

    private fun IrElement.hasValidOffsets() = startOffset != UNDEFINED_OFFSET && endOffset != UNDEFINED_OFFSET
            && startOffset != endOffset
}