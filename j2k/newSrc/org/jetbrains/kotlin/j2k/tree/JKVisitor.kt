package org.jetbrains.kotlin.j2k.tree

interface JKVisitor<R, D>  {
    fun visitElement(element: JKElement, data: D): R 
    fun visitClass(klass: JKClass, data: D): R = visitDeclaration(klass, data)
    fun visitStatement(statement: JKStatement, data: D): R = visitElement(statement, data)
    fun visitExpression(expression: JKExpression, data: D): R = visitStatement(expression, data)
    fun visitLoop(loop: JKLoop, data: D): R = visitStatement(loop, data)
    fun visitDeclaration(declaration: JKDeclaration, data: D): R = visitElement(declaration, data)
    fun visitBlock(block: JKBlock, data: D): R = visitElement(block, data)
    fun visitCall(call: JKCall, data: D): R = visitExpression(call, data)
    fun visitIdentifier(identifier: JKIdentifier, data: D): R = visitElement(identifier, data)
    fun visitTypeIdentifier(typeIdentifier: JKTypeIdentifier, data: D): R = visitIdentifier(typeIdentifier, data)
    fun visitNameIdentifier(nameIdentifier: JKNameIdentifier, data: D): R = visitIdentifier(nameIdentifier, data)
    fun visitLiteralExpression(literalExpression: JKLiteralExpression, data: D): R = visitExpression(literalExpression, data)
    fun visitModifierList(modifierList: JKModifierList, data: D): R = visitElement(modifierList, data)
    fun visitModifier(modifier: JKModifier, data: D): R = visitElement(modifier, data)
    fun visitAccessModifier(accessModifier: JKAccessModifier, data: D): R = visitModifier(accessModifier, data)
}
