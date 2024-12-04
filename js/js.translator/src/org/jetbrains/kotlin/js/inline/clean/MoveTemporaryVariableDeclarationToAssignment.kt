/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import java.util.LinkedHashSet

/**
 * Moving a declaration of the temporary variable without an initializer to the closest assignment.
 *
 * Basic example:
 *      var $tmp;
 *      $tmp = <expr>;
 * Transformed to:
 *      var $tmp = <expr>;
 *
 * The optimization algorithm does the following:
 * 1) Collects all temporary variables whose declarations do not have an initializer.
 * 2) Collects the JsBlock set of all blocks where the temporary variables are assigned and used.
 * 3) Ignores the temporary variable if it is used between the declaration and the first assignment.
 * 4) Represents the collected JsBlocks as a tree.
 * 5) Searches for the Lowest Common Ancestor (LCA) among all JsBlocks where the temporary variables are assigned or used.
 * 6) If the LCA is found in the set of JsBlocks where the temporary variable is assigned, we can move the declaration to the assignment.
 */
class MoveTemporaryVariableDeclarationToAssignment(private val body: JsBlock) {
    private var hasChanges = false

    private val varUsedInBlocks = hashMapOf<JsName, HashSet<JsBlock>>()
    private val varAssignedInBlocks = hashMapOf<JsName, HashSet<JsBlock>>()

    private val blockParents = hashMapOf<JsBlock, JsBlock>()

    private val varWithoutInitDeclarations = hashSetOf<JsName>()
    private val varBeforeAssignmentUsages = hashSetOf<JsName>()

    private val removedVarDeclarations = hashSetOf<JsName>()

    fun apply(): Boolean {
        analyze()
        perform()

        require(removedVarDeclarations.isEmpty())

        return hasChanges
    }

    private fun analyze() {
        val visitor = object : RecursiveJsVisitor() {
            private val blockStack = mutableListOf<JsBlock>()

            private val currentBlock
                get() = blockStack.last()

            override fun visitBlock(x: JsBlock) {
                if (blockStack.isNotEmpty()) {
                    blockParents[x] = currentBlock
                }
                blockStack += x
                super.visitBlock(x)
                blockStack.removeLast()
            }

            override fun visit(x: JsVars.JsVar) {
                if (x.initExpression == null && x.synthetic) {
                    varWithoutInitDeclarations += x.name
                }
                super.visit(x)
            }

            override fun visitVars(x: JsVars) {
                if (x.synthetic) {
                    for (variable in x) {
                        if (variable.initExpression == null) {
                            varWithoutInitDeclarations += variable.name
                        }
                    }
                }
                super.visitVars(x)
            }

            override fun visitNameRef(nameRef: JsNameRef) {
                val name = nameRef.name
                if (name != null && name in varWithoutInitDeclarations) {
                    varUsedInBlocks.getOrPut(name) { hashSetOf() } += currentBlock

                    val assignments = varAssignedInBlocks[name] ?: emptySet()
                    val hasAssignment = blockStack.any { it in assignments }
                    if (!hasAssignment) {
                        varBeforeAssignmentUsages += name
                    }
                }
                super.visitNameRef(nameRef)
            }

            override fun visitExpressionStatement(x: JsExpressionStatement) {
                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    val (name, _) = assignment
                    if (name in varWithoutInitDeclarations) {
                        varAssignedInBlocks.getOrPut(name) { hashSetOf() } += currentBlock
                    }
                }
                super.visitExpressionStatement(x)
            }
        }

        visitor.accept(body)
    }

    private fun removeNonParentBlocks(block: JsBlock, parents: LinkedHashSet<JsBlock>) {
        var blockParent: JsBlock? = block
        while (blockParent != null) {
            if (blockParent !in parents) {
                blockParent = blockParents[blockParent]
                continue
            }
            val parentIter = parents.iterator()
            while (parentIter.hasNext()) {
                if (parentIter.next() != blockParent) {
                    parentIter.remove()
                } else {
                    return
                }
            }
        }
    }

    private fun calculateLCA(blocks: Set<JsBlock>): JsBlock {
        require(blocks.isNotEmpty())

        val parents = LinkedHashSet<JsBlock>()
        var firstBlockParent: JsBlock? = blocks.first()
        while (firstBlockParent != null) {
            parents += firstBlockParent
            firstBlockParent = blockParents[firstBlockParent]
        }

        for (block in blocks.asSequence().drop(1)) {
            removeNonParentBlocks(block, parents)
        }

        require(parents.isNotEmpty())
        return parents.first()
    }

    private fun perform() {
        val visitor = object : JsVisitorWithContextImpl() {
            private fun canRemoveDeclarationWithoutInit(name: JsName): Boolean {
                if (name !in varWithoutInitDeclarations || name in varBeforeAssignmentUsages) {
                    return false
                }

                val assignedInBlocks = varAssignedInBlocks[name] ?: return false
                val usedInBlocks = varUsedInBlocks[name] ?: emptySet()

                val lcaBlock = calculateLCA(usedInBlocks + assignedInBlocks)
                return lcaBlock in assignedInBlocks
            }

            override fun endVisit(x: JsVars.JsVar, ctx: JsContext<*>) {
                if (canRemoveDeclarationWithoutInit(x.name)) {
                    removedVarDeclarations += x.name
                    ctx.removeMe()
                    hasChanges = true
                } else {
                    super.endVisit(x, ctx)
                }
            }

            override fun endVisit(x: JsVars, ctx: JsContext<*>) {
                if (x.isEmpty) {
                    ctx.removeMe()
                    hasChanges = true
                } else {
                    super.endVisit(x, ctx)
                }
            }

            override fun endVisit(x: JsExpressionStatement, ctx: JsContext<JsNode>) {
                val assignment = JsAstUtils.decomposeAssignmentToVariable(x.expression)
                if (assignment != null) {
                    val (name, initExpr) = assignment
                    if (removedVarDeclarations.remove(name)) {
                        val varDeclarationWithInit = JsVars.JsVar(name, initExpr).apply { synthetic = true }
                        val vars = JsVars(varDeclarationWithInit).apply { synthetic = true }
                        ctx.replaceMe(vars)
                    }
                    accept(initExpr)
                } else {
                    super.endVisit(x, ctx)
                }
            }
        }

        visitor.accept(body)
    }
}
