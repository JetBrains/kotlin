package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKVisitor<out R, in D> {
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
    fun visitJavaField(javaField: JKJavaField, data: D): R = visitDeclaration(javaField, data)
    fun visitJavaMethod(javaMethod: JKJavaMethod, data: D): R = visitDeclaration(javaMethod, data)
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: D): R = visitLoop(javaForLoop, data)
    fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: D): R = visitExpression(javaAssignmentExpression, data)
    fun visitJavaCall(javaCall: JKJavaCall, data: D): R = visitCall(javaCall, data)
    fun visitJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier, data: D): R = visitTypeIdentifier(javaTypeIdentifier, data)
    fun visitJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression, data: D): R = visitLiteralExpression(javaStringLiteralExpression, data)
    fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier, data: D): R = visitAccessModifier(javaAccessModifier, data)
    fun visitKtFun(ktFun: JKKtFun, data: D): R = visitDeclaration(ktFun, data)
    fun visitKtConstructor(ktConstructor: JKKtConstructor, data: D): R = visitDeclaration(ktConstructor, data)
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: D): R = visitKtConstructor(ktPrimaryConstructor, data)
    fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: D): R = visitStatement(ktAssignmentStatement, data)
    fun visitKtCall(ktCall: JKKtCall, data: D): R = visitCall(ktCall, data)
    fun visitKtProperty(ktProperty: JKKtProperty, data: D): R = visitDeclaration(ktProperty, data)
}
