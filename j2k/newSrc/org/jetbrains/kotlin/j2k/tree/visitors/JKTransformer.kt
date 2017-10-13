package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKTransformer<D> {
    fun transformElement(element: JKElement, data: D): JKElement 
    fun transformClass(klass: JKClass, data: D): JKElement = transformDeclaration(klass, data)
    fun transformStatement(statement: JKStatement, data: D): JKElement = transformElement(statement, data)
    fun transformExpression(expression: JKExpression, data: D): JKElement = transformStatement(expression, data)
    fun transformLoop(loop: JKLoop, data: D): JKElement = transformStatement(loop, data)
    fun transformDeclaration(declaration: JKDeclaration, data: D): JKElement = transformElement(declaration, data)
    fun transformBlock(block: JKBlock, data: D): JKElement = transformElement(block, data)
    fun transformCall(call: JKCall, data: D): JKElement = transformExpression(call, data)
    fun transformIdentifier(identifier: JKIdentifier, data: D): JKElement = transformElement(identifier, data)
    fun transformTypeIdentifier(typeIdentifier: JKTypeIdentifier, data: D): JKElement = transformIdentifier(typeIdentifier, data)
    fun transformNameIdentifier(nameIdentifier: JKNameIdentifier, data: D): JKElement = transformIdentifier(nameIdentifier, data)
    fun transformLiteralExpression(literalExpression: JKLiteralExpression, data: D): JKElement = transformExpression(literalExpression, data)
    fun transformModifierList(modifierList: JKModifierList, data: D): JKElement = transformElement(modifierList, data)
    fun transformModifier(modifier: JKModifier, data: D): JKElement = transformElement(modifier, data)
    fun transformAccessModifier(accessModifier: JKAccessModifier, data: D): JKElement = transformModifier(accessModifier, data)
}
