package org.jetbrains.kotlin.j2k.tree

interface JKVisitorVoid : JKVisitor<Unit, Nothing?> {
    fun visitElement(element: JKElement) 
    override fun visitElement(element: JKElement, data: Nothing?) = visitElement(element)
    fun visitClass(klass: JKClass) = visitDeclaration(klass, null)
    override fun visitClass(klass: JKClass, data: Nothing?) = visitClass(klass)
    fun visitExpression(expression: JKExpression) = visitElement(expression, null)
    override fun visitExpression(expression: JKExpression, data: Nothing?) = visitExpression(expression)
    fun visitStatement(statement: JKStatement) = visitElement(statement, null)
    override fun visitStatement(statement: JKStatement, data: Nothing?) = visitStatement(statement)
    fun visitLoop(loop: JKLoop) = visitStatement(loop, null)
    override fun visitLoop(loop: JKLoop, data: Nothing?) = visitLoop(loop)
    fun visitDeclaration(declaration: JKDeclaration) = visitElement(declaration, null)
    override fun visitDeclaration(declaration: JKDeclaration, data: Nothing?) = visitDeclaration(declaration)
    fun visitBlock(block: JKBlock) = visitElement(block, null)
    override fun visitBlock(block: JKBlock, data: Nothing?) = visitBlock(block)
}
