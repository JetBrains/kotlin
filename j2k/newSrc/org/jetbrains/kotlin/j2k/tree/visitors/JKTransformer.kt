package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKTransformer<in D> : JKVisitor<JKElement, D> {
    override fun visitElement(element: JKElement, data: D): JKElement 
    override fun visitClass(klass: JKClass, data: D): JKClass = visitDeclaration(klass, data) as JKClass
    override fun visitStatement(statement: JKStatement, data: D): JKStatement = visitElement(statement, data) as JKStatement
    override fun visitExpression(expression: JKExpression, data: D): JKExpression = visitStatement(expression, data) as JKExpression
    override fun visitLoop(loop: JKLoop, data: D): JKLoop = visitStatement(loop, data) as JKLoop
    override fun visitDeclaration(declaration: JKDeclaration, data: D): JKDeclaration = visitElement(declaration, data) as JKDeclaration
    override fun visitBlock(block: JKBlock, data: D): JKBlock = visitElement(block, data) as JKBlock
    override fun visitCall(call: JKCall, data: D): JKCall = visitExpression(call, data) as JKCall
    override fun visitIdentifier(identifier: JKIdentifier, data: D): JKIdentifier = visitElement(identifier, data) as JKIdentifier
    override fun visitTypeIdentifier(typeIdentifier: JKTypeIdentifier, data: D): JKTypeIdentifier = visitIdentifier(typeIdentifier, data) as JKTypeIdentifier
    override fun visitNameIdentifier(nameIdentifier: JKNameIdentifier, data: D): JKNameIdentifier = visitIdentifier(nameIdentifier, data) as JKNameIdentifier
    override fun visitLiteralExpression(literalExpression: JKLiteralExpression, data: D): JKLiteralExpression = visitExpression(literalExpression, data) as JKLiteralExpression
    override fun visitModifierList(modifierList: JKModifierList, data: D): JKModifierList = visitElement(modifierList, data) as JKModifierList
    override fun visitModifier(modifier: JKModifier, data: D): JKModifier = visitElement(modifier, data) as JKModifier
    override fun visitAccessModifier(accessModifier: JKAccessModifier, data: D): JKAccessModifier = visitModifier(accessModifier, data) as JKAccessModifier
    override fun visitJavaField(javaField: JKJavaField, data: D): JKDeclaration = visitDeclaration(javaField, data) 
    override fun visitJavaMethod(javaMethod: JKJavaMethod, data: D): JKDeclaration = visitDeclaration(javaMethod, data) 
    override fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: D): JKLoop = visitLoop(javaForLoop, data) 
    override fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: D): JKExpression = visitExpression(javaAssignmentExpression, data) 
    override fun visitJavaCall(javaCall: JKJavaCall, data: D): JKCall = visitCall(javaCall, data) 
    override fun visitJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier, data: D): JKTypeIdentifier = visitTypeIdentifier(javaTypeIdentifier, data) 
    override fun visitJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression, data: D): JKLiteralExpression = visitLiteralExpression(javaStringLiteralExpression, data) 
    override fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier, data: D): JKAccessModifier = visitAccessModifier(javaAccessModifier, data) 
    override fun visitKtFun(ktFun: JKKtFun, data: D): JKDeclaration = visitDeclaration(ktFun, data) 
    override fun visitKtConstructor(ktConstructor: JKKtConstructor, data: D): JKDeclaration = visitDeclaration(ktConstructor, data) 
    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: D): JKDeclaration = visitKtConstructor(ktPrimaryConstructor, data) 
    override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: D): JKStatement = visitStatement(ktAssignmentStatement, data) 
    override fun visitKtCall(ktCall: JKKtCall, data: D): JKCall = visitCall(ktCall, data) 
}
