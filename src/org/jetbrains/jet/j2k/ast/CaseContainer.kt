package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.util.AstUtil
import java.util.LinkedList
import java.util.List

public open class CaseContainer(val caseStatement: List<Statement>, statements: List<Statement>): Statement() {
    private val myBlock: Block

    {
        val newStatements: List<Statement> = statements.filterNot { it is BreakStatement || it is ContinueStatement }
        myBlock = Block(newStatements, false)
    }

    public override fun toKotlin(): String {
        return AstUtil.joinNodes(caseStatement, ", ") + " -> " + myBlock.toKotlin()
    }
}
