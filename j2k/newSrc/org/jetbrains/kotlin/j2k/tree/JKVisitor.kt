package org.jetbrains.kotlin.j2k.tree

interface JKVisitor<R, D>  {
    fun visitElement(element: JKElement, data: D): R
    fun visitClass(klass: JKClass, data: D): R
    fun visitMember(member: JKMember, data: D): R
    fun visitExpression(expression: JKExpression, data: D): R
    fun visitStatement(statement: JKStatement, data: D): R
    fun visitLoop(loop: JKLoop, data: D): R
    fun visitDeclaration(declaration: JKDeclaration, data: D): R
}
