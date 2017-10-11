package org.jetbrains.kotlin.j2k.tree

interface JKVisitorVoid : JKVisitor<Unit, Nothing?> {
    fun visitElement(element: JKElement)
    override fun visitElement(element: JKElement, data: Nothing?) = visitElement(element)
    fun visitClass(klass: JKClass)
    override fun visitClass(klass: JKClass, data: Nothing?) = visitClass(klass)
    fun visitMember(member: JKMember)
    override fun visitMember(member: JKMember, data: Nothing?) = visitMember(member)
    fun visitExpression(expression: JKExpression)
    override fun visitExpression(expression: JKExpression, data: Nothing?) = visitExpression(expression)
    fun visitStatement(statement: JKStatement)
    override fun visitStatement(statement: JKStatement, data: Nothing?) = visitStatement(statement)
    fun visitLoop(loop: JKLoop)
    override fun visitLoop(loop: JKLoop, data: Nothing?) = visitLoop(loop)
    fun visitDeclaration(declaration: JKDeclaration)
    override fun visitDeclaration(declaration: JKDeclaration, data: Nothing?) = visitDeclaration(declaration)
}
