package org.jetbrains.kotlin.j2k.tree

interface JKVisitor<R, D>  {
    fun visitElement(element: JKElement, data: D): R 
    fun visitClass(klass: JKClass, data: D): R = visitDeclaration(klass, data)
    fun visitExpression(expression: JKExpression, data: D): R = visitElement(expression, data)
    fun visitStatement(statement: JKStatement, data: D): R = visitElement(statement, data)
    fun visitLoop(loop: JKLoop, data: D): R = visitStatement(loop, data)
    fun visitDeclaration(declaration: JKDeclaration, data: D): R = visitElement(declaration, data)
    fun visitBlock(block: JKBlock, data: D): R = visitElement(block, data)
}
