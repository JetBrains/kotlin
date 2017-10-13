package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKTransformerVoid : JKTransformer<Nothing?> {
    fun transformElement(element: JKElement) : JKElement
    override fun transformElement(element: JKElement, data: Nothing?) = transformElement(element)
    fun transformClass(klass: JKClass) = transformDeclaration(klass, null)
    override fun transformClass(klass: JKClass, data: Nothing?) = transformClass(klass)
    fun transformStatement(statement: JKStatement) = transformElement(statement, null)
    override fun transformStatement(statement: JKStatement, data: Nothing?) = transformStatement(statement)
    fun transformExpression(expression: JKExpression) = transformStatement(expression, null)
    override fun transformExpression(expression: JKExpression, data: Nothing?) = transformExpression(expression)
    fun transformLoop(loop: JKLoop) = transformStatement(loop, null)
    override fun transformLoop(loop: JKLoop, data: Nothing?) = transformLoop(loop)
    fun transformDeclaration(declaration: JKDeclaration) = transformElement(declaration, null)
    override fun transformDeclaration(declaration: JKDeclaration, data: Nothing?) = transformDeclaration(declaration)
    fun transformBlock(block: JKBlock) = transformElement(block, null)
    override fun transformBlock(block: JKBlock, data: Nothing?) = transformBlock(block)
    fun transformCall(call: JKCall) = transformExpression(call, null)
    override fun transformCall(call: JKCall, data: Nothing?) = transformCall(call)
    fun transformIdentifier(identifier: JKIdentifier) = transformElement(identifier, null)
    override fun transformIdentifier(identifier: JKIdentifier, data: Nothing?) = transformIdentifier(identifier)
    fun transformTypeIdentifier(typeIdentifier: JKTypeIdentifier) = transformIdentifier(typeIdentifier, null)
    override fun transformTypeIdentifier(typeIdentifier: JKTypeIdentifier, data: Nothing?) = transformTypeIdentifier(typeIdentifier)
    fun transformNameIdentifier(nameIdentifier: JKNameIdentifier) = transformIdentifier(nameIdentifier, null)
    override fun transformNameIdentifier(nameIdentifier: JKNameIdentifier, data: Nothing?) = transformNameIdentifier(nameIdentifier)
    fun transformLiteralExpression(literalExpression: JKLiteralExpression) = transformExpression(literalExpression, null)
    override fun transformLiteralExpression(literalExpression: JKLiteralExpression, data: Nothing?) = transformLiteralExpression(literalExpression)
    fun transformModifierList(modifierList: JKModifierList) = transformElement(modifierList, null)
    override fun transformModifierList(modifierList: JKModifierList, data: Nothing?) = transformModifierList(modifierList)
    fun transformModifier(modifier: JKModifier) = transformElement(modifier, null)
    override fun transformModifier(modifier: JKModifier, data: Nothing?) = transformModifier(modifier)
    fun transformAccessModifier(accessModifier: JKAccessModifier) = transformModifier(accessModifier, null)
    override fun transformAccessModifier(accessModifier: JKAccessModifier, data: Nothing?) = transformAccessModifier(accessModifier)
}
