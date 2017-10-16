package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKTransformerVoid : JKTransformer<Nothing?> {
    fun visitElement(element: JKElement): JKElement 
    override fun visitElement(element: JKElement, data: Nothing?): JKElement = visitElement(element)
    fun visitClass(klass: JKClass): JKClass = visitDeclaration(klass) as JKClass
    override fun visitClass(klass: JKClass, data: Nothing?): JKClass = visitClass(klass)
    fun visitStatement(statement: JKStatement): JKStatement = visitElement(statement) as JKStatement
    override fun visitStatement(statement: JKStatement, data: Nothing?): JKStatement = visitStatement(statement)
    fun visitExpression(expression: JKExpression): JKExpression = visitStatement(expression) as JKExpression
    override fun visitExpression(expression: JKExpression, data: Nothing?): JKExpression = visitExpression(expression)
    fun visitLoop(loop: JKLoop): JKLoop = visitStatement(loop) as JKLoop
    override fun visitLoop(loop: JKLoop, data: Nothing?): JKLoop = visitLoop(loop)
    fun visitDeclaration(declaration: JKDeclaration): JKDeclaration = visitElement(declaration) as JKDeclaration
    override fun visitDeclaration(declaration: JKDeclaration, data: Nothing?): JKDeclaration = visitDeclaration(declaration)
    fun visitBlock(block: JKBlock): JKBlock = visitElement(block) as JKBlock
    override fun visitBlock(block: JKBlock, data: Nothing?): JKBlock = visitBlock(block)
    fun visitCall(call: JKCall): JKCall = visitExpression(call) as JKCall
    override fun visitCall(call: JKCall, data: Nothing?): JKCall = visitCall(call)
    fun visitIdentifier(identifier: JKIdentifier): JKIdentifier = visitElement(identifier) as JKIdentifier
    override fun visitIdentifier(identifier: JKIdentifier, data: Nothing?): JKIdentifier = visitIdentifier(identifier)
    fun visitTypeIdentifier(typeIdentifier: JKTypeIdentifier): JKTypeIdentifier = visitIdentifier(typeIdentifier) as JKTypeIdentifier
    override fun visitTypeIdentifier(typeIdentifier: JKTypeIdentifier, data: Nothing?): JKTypeIdentifier = visitTypeIdentifier(typeIdentifier)
    fun visitNameIdentifier(nameIdentifier: JKNameIdentifier): JKNameIdentifier = visitIdentifier(nameIdentifier) as JKNameIdentifier
    override fun visitNameIdentifier(nameIdentifier: JKNameIdentifier, data: Nothing?): JKNameIdentifier = visitNameIdentifier(nameIdentifier)
    fun visitLiteralExpression(literalExpression: JKLiteralExpression): JKLiteralExpression = visitExpression(literalExpression) as JKLiteralExpression
    override fun visitLiteralExpression(literalExpression: JKLiteralExpression, data: Nothing?): JKLiteralExpression = visitLiteralExpression(literalExpression)
    fun visitModifierList(modifierList: JKModifierList): JKModifierList = visitElement(modifierList) as JKModifierList
    override fun visitModifierList(modifierList: JKModifierList, data: Nothing?): JKModifierList = visitModifierList(modifierList)
    fun visitModifier(modifier: JKModifier): JKModifier = visitElement(modifier) as JKModifier
    override fun visitModifier(modifier: JKModifier, data: Nothing?): JKModifier = visitModifier(modifier)
    fun visitAccessModifier(accessModifier: JKAccessModifier): JKAccessModifier = visitModifier(accessModifier) as JKAccessModifier
    override fun visitAccessModifier(accessModifier: JKAccessModifier, data: Nothing?): JKAccessModifier = visitAccessModifier(accessModifier)
    fun visitJavaField(javaField: JKJavaField): JKDeclaration = visitDeclaration(javaField) 
    override fun visitJavaField(javaField: JKJavaField, data: Nothing?): JKDeclaration = visitJavaField(javaField)
    fun visitJavaMethod(javaMethod: JKJavaMethod): JKDeclaration = visitDeclaration(javaMethod) 
    override fun visitJavaMethod(javaMethod: JKJavaMethod, data: Nothing?): JKDeclaration = visitJavaMethod(javaMethod)
    fun visitJavaForLoop(javaForLoop: JKJavaForLoop): JKLoop = visitLoop(javaForLoop) 
    override fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: Nothing?): JKLoop = visitJavaForLoop(javaForLoop)
    fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression): JKExpression = visitExpression(javaAssignmentExpression) 
    override fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: Nothing?): JKExpression = visitJavaAssignmentExpression(javaAssignmentExpression)
    fun visitJavaCall(javaCall: JKJavaCall): JKCall = visitCall(javaCall) 
    override fun visitJavaCall(javaCall: JKJavaCall, data: Nothing?): JKCall = visitJavaCall(javaCall)
    fun visitJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier): JKTypeIdentifier = visitTypeIdentifier(javaTypeIdentifier) 
    override fun visitJavaTypeIdentifier(javaTypeIdentifier: JKJavaTypeIdentifier, data: Nothing?): JKTypeIdentifier = visitJavaTypeIdentifier(javaTypeIdentifier)
    fun visitJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression): JKLiteralExpression = visitLiteralExpression(javaStringLiteralExpression) 
    override fun visitJavaStringLiteralExpression(javaStringLiteralExpression: JKJavaStringLiteralExpression, data: Nothing?): JKLiteralExpression = visitJavaStringLiteralExpression(javaStringLiteralExpression)
    fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier): JKAccessModifier = visitAccessModifier(javaAccessModifier) 
    override fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier, data: Nothing?): JKAccessModifier = visitJavaAccessModifier(javaAccessModifier)
    fun visitKtFun(ktFun: JKKtFun): JKDeclaration = visitDeclaration(ktFun) 
    override fun visitKtFun(ktFun: JKKtFun, data: Nothing?): JKDeclaration = visitKtFun(ktFun)
    fun visitKtConstructor(ktConstructor: JKKtConstructor): JKDeclaration = visitDeclaration(ktConstructor) 
    override fun visitKtConstructor(ktConstructor: JKKtConstructor, data: Nothing?): JKDeclaration = visitKtConstructor(ktConstructor)
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor): JKDeclaration = visitKtConstructor(ktPrimaryConstructor) 
    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: Nothing?): JKDeclaration = visitKtPrimaryConstructor(ktPrimaryConstructor)
    fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement): JKStatement = visitStatement(ktAssignmentStatement) 
    override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: Nothing?): JKStatement = visitKtAssignmentStatement(ktAssignmentStatement)
    fun visitKtCall(ktCall: JKKtCall): JKCall = visitCall(ktCall) 
    override fun visitKtCall(ktCall: JKKtCall, data: Nothing?): JKCall = visitKtCall(ktCall)
}
