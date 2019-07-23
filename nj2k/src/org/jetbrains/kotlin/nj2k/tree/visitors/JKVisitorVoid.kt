package org.jetbrains.kotlin.nj2k.tree.visitors

import org.jetbrains.kotlin.nj2k.tree.*

interface JKVisitorVoid : JKVisitor<Unit, Nothing?> {
    fun visitTreeElement(treeElement: JKTreeElement)
    override fun visitTreeElement(treeElement: JKTreeElement, data: Nothing?) = visitTreeElement(treeElement)
    fun visitTreeRoot(treeRoot: JKTreeRoot) = visitTreeElement(treeRoot, null)
    override fun visitTreeRoot(treeRoot: JKTreeRoot, data: Nothing?) = visitTreeRoot(treeRoot)
    fun visitDeclaration(declaration: JKDeclaration) = visitTreeElement(declaration, null)
    override fun visitDeclaration(declaration: JKDeclaration, data: Nothing?) = visitDeclaration(declaration)
    fun visitImportStatement(importStatement: JKImportStatement) = visitTreeElement(importStatement, null)
    override fun visitImportStatement(importStatement: JKImportStatement, data: Nothing?) = visitImportStatement(importStatement)
    fun visitImportList(importList: JKImportList) = visitTreeElement(importList, null)
    override fun visitImportList(importList: JKImportList, data: Nothing?) = visitImportList(importList)
    fun visitFile(file: JKFile) = visitTreeElement(file, null)
    override fun visitFile(file: JKFile, data: Nothing?) = visitFile(file)
    fun visitClass(klass: JKClass) = visitDeclaration(klass, null)
    override fun visitClass(klass: JKClass, data: Nothing?) = visitClass(klass)
    fun visitInheritanceInfo(inheritanceInfo: JKInheritanceInfo) = visitTreeElement(inheritanceInfo, null)
    override fun visitInheritanceInfo(inheritanceInfo: JKInheritanceInfo, data: Nothing?) = visitInheritanceInfo(inheritanceInfo)
    fun visitAnnotationList(annotationList: JKAnnotationList) = visitTreeElement(annotationList, null)
    override fun visitAnnotationList(annotationList: JKAnnotationList, data: Nothing?) = visitAnnotationList(annotationList)
    fun visitAnnotation(annotation: JKAnnotation) = visitAnnotationMemberValue(annotation, null)
    override fun visitAnnotation(annotation: JKAnnotation, data: Nothing?) = visitAnnotation(annotation)
    fun visitAnnotationParameter(annotationParameter: JKAnnotationParameter) = visitTreeElement(annotationParameter, null)
    override fun visitAnnotationParameter(annotationParameter: JKAnnotationParameter, data: Nothing?) =
        visitAnnotationParameter(annotationParameter)

    fun visitAnnotationNameParameter(annotationNameParameter: JKAnnotationNameParameter) =
        visitAnnotationParameter(annotationNameParameter, null)

    override fun visitAnnotationNameParameter(annotationNameParameter: JKAnnotationNameParameter, data: Nothing?) =
        visitAnnotationNameParameter(annotationNameParameter)

    fun visitAnnotationListOwner(annotationListOwner: JKAnnotationListOwner) = visitTreeElement(annotationListOwner, null)
    override fun visitAnnotationListOwner(annotationListOwner: JKAnnotationListOwner, data: Nothing?) =
        visitAnnotationListOwner(annotationListOwner)

    fun visitMethod(method: JKMethod) = visitDeclaration(method, null)
    override fun visitMethod(method: JKMethod, data: Nothing?) = visitMethod(method)
    fun visitVariable(variable: JKVariable) = visitDeclaration(variable, null)
    override fun visitVariable(variable: JKVariable, data: Nothing?) = visitVariable(variable)
    fun visitForLoopVariable(forLoopVariable: JKForLoopVariable) = visitVariable(forLoopVariable, null)
    override fun visitForLoopVariable(forLoopVariable: JKForLoopVariable, data: Nothing?) = visitForLoopVariable(forLoopVariable)
    fun visitLocalVariable(localVariable: JKLocalVariable) = visitVariable(localVariable, null)
    override fun visitLocalVariable(localVariable: JKLocalVariable, data: Nothing?) = visitLocalVariable(localVariable)
    fun visitModifierElement(modifierElement: JKModifierElement) = visitTreeElement(modifierElement, null)
    override fun visitModifierElement(modifierElement: JKModifierElement, data: Nothing?) = visitModifierElement(modifierElement)
    fun visitMutabilityModifierElement(mutabilityModifierElement: JKMutabilityModifierElement) =
        visitModifierElement(mutabilityModifierElement, null)

    override fun visitMutabilityModifierElement(mutabilityModifierElement: JKMutabilityModifierElement, data: Nothing?) =
        visitMutabilityModifierElement(mutabilityModifierElement)

    fun visitModalityModifierElement(modalityModifierElement: JKModalityModifierElement) =
        visitModifierElement(modalityModifierElement, null)

    override fun visitModalityModifierElement(modalityModifierElement: JKModalityModifierElement, data: Nothing?) =
        visitModalityModifierElement(modalityModifierElement)

    fun visitVisibilityModifierElement(visibilityModifierElement: JKVisibilityModifierElement) =
        visitModifierElement(visibilityModifierElement, null)

    override fun visitVisibilityModifierElement(visibilityModifierElement: JKVisibilityModifierElement, data: Nothing?) =
        visitVisibilityModifierElement(visibilityModifierElement)

    fun visitExtraModifierElement(otherModifierElement: JKOtherModifierElement) = visitModifierElement(otherModifierElement, null)
    override fun visitExtraModifierElement(otherModifierElement: JKOtherModifierElement, data: Nothing?) =
        visitExtraModifierElement(otherModifierElement)

    fun visitExtraModifiersOwner(otherModifiersOwner: JKOtherModifiersOwner) = visitModifiersListOwner(otherModifiersOwner, null)
    override fun visitExtraModifiersOwner(otherModifiersOwner: JKOtherModifiersOwner, data: Nothing?) =
        visitExtraModifiersOwner(otherModifiersOwner)

    fun visitVisibilityOwner(visibilityOwner: JKVisibilityOwner) = visitModifiersListOwner(visibilityOwner, null)
    override fun visitVisibilityOwner(visibilityOwner: JKVisibilityOwner, data: Nothing?) = visitVisibilityOwner(visibilityOwner)
    fun visitModalityOwner(modalityOwner: JKModalityOwner) = visitModifiersListOwner(modalityOwner, null)
    override fun visitModalityOwner(modalityOwner: JKModalityOwner, data: Nothing?) = visitModalityOwner(modalityOwner)
    fun visitMutabilityOwner(mutabilityOwner: JKMutabilityOwner) = visitModifiersListOwner(mutabilityOwner, null)
    override fun visitMutabilityOwner(mutabilityOwner: JKMutabilityOwner, data: Nothing?) = visitMutabilityOwner(mutabilityOwner)
    fun visitModifiersListOwner(modifiersListOwner: JKModifiersListOwner) = visitTreeElement(modifiersListOwner, null)
    override fun visitModifiersListOwner(modifiersListOwner: JKModifiersListOwner, data: Nothing?) =
        visitModifiersListOwner(modifiersListOwner)

    fun visitTypeElement(typeElement: JKTypeElement) = visitTreeElement(typeElement, null)
    override fun visitTypeElement(typeElement: JKTypeElement, data: Nothing?) = visitTypeElement(typeElement)
    fun visitStatement(statement: JKStatement) = visitTreeElement(statement, null)
    override fun visitStatement(statement: JKStatement, data: Nothing?) = visitStatement(statement)
    fun visitBlock(block: JKBlock) = visitTreeElement(block, null)
    override fun visitBlock(block: JKBlock, data: Nothing?) = visitBlock(block)
    fun visitBodyStub(bodyStub: JKBodyStub) = visitBlock(bodyStub, null)
    override fun visitBodyStub(bodyStub: JKBodyStub, data: Nothing?) = visitBodyStub(bodyStub)
    fun visitIdentifier(identifier: JKIdentifier) = visitTreeElement(identifier, null)
    override fun visitIdentifier(identifier: JKIdentifier, data: Nothing?) = visitIdentifier(identifier)
    fun visitNameIdentifier(nameIdentifier: JKNameIdentifier) = visitIdentifier(nameIdentifier, null)
    override fun visitNameIdentifier(nameIdentifier: JKNameIdentifier, data: Nothing?) = visitNameIdentifier(nameIdentifier)
    fun visitExpression(expression: JKExpression) = visitTreeElement(expression, null)
    override fun visitExpression(expression: JKExpression, data: Nothing?) = visitExpression(expression)
    fun visitMethodReferenceExpression(methodReferenceExpression: JKMethodReferenceExpression) =
        visitExpression(methodReferenceExpression, null)

    override fun visitMethodReferenceExpression(methodReferenceExpression: JKMethodReferenceExpression, data: Nothing?) =
        visitMethodReferenceExpression(methodReferenceExpression)

    fun visitExpressionStatement(expressionStatement: JKExpressionStatement) = visitStatement(expressionStatement, null)
    override fun visitExpressionStatement(expressionStatement: JKExpressionStatement, data: Nothing?) =
        visitExpressionStatement(expressionStatement)

    fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement) = visitStatement(declarationStatement, null)
    override fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement, data: Nothing?) =
        visitDeclarationStatement(declarationStatement)

    fun visitOperatorExpression(operatorExpression: JKOperatorExpression) = visitExpression(operatorExpression, null)
    override fun visitOperatorExpression(operatorExpression: JKOperatorExpression, data: Nothing?) =
        visitOperatorExpression(operatorExpression)

    fun visitBinaryExpression(binaryExpression: JKBinaryExpression) = visitOperatorExpression(binaryExpression, null)
    override fun visitBinaryExpression(binaryExpression: JKBinaryExpression, data: Nothing?) = visitBinaryExpression(binaryExpression)
    fun visitUnaryExpression(unaryExpression: JKUnaryExpression) = visitOperatorExpression(unaryExpression, null)
    override fun visitUnaryExpression(unaryExpression: JKUnaryExpression, data: Nothing?) = visitUnaryExpression(unaryExpression)
    fun visitPrefixExpression(prefixExpression: JKPrefixExpression) = visitUnaryExpression(prefixExpression, null)
    override fun visitPrefixExpression(prefixExpression: JKPrefixExpression, data: Nothing?) = visitPrefixExpression(prefixExpression)
    fun visitPostfixExpression(postfixExpression: JKPostfixExpression) = visitUnaryExpression(postfixExpression, null)
    override fun visitPostfixExpression(postfixExpression: JKPostfixExpression, data: Nothing?) = visitPostfixExpression(postfixExpression)
    fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression) = visitExpression(qualifiedExpression, null)
    override fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression, data: Nothing?) =
        visitQualifiedExpression(qualifiedExpression)

    fun visitTypeArgumentList(typeArgumentList: JKTypeArgumentList) = visitTreeElement(typeArgumentList, null)
    override fun visitTypeArgumentList(typeArgumentList: JKTypeArgumentList, data: Nothing?) = visitTypeArgumentList(typeArgumentList)
    fun visitTypeArgumentListOwner(typeArgumentListOwner: JKTypeArgumentListOwner) = visitTreeElement(typeArgumentListOwner, null)
    override fun visitTypeArgumentListOwner(typeArgumentListOwner: JKTypeArgumentListOwner, data: Nothing?) =
        visitTypeArgumentListOwner(typeArgumentListOwner)

    fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression) = visitExpression(methodCallExpression, null)
    override fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression, data: Nothing?) =
        visitMethodCallExpression(methodCallExpression)

    fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression) = visitAssignableExpression(fieldAccessExpression, null)
    override fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression, data: Nothing?) =
        visitFieldAccessExpression(fieldAccessExpression)

    fun visitPackageAccessExpression(packageAccessExpression: JKPackageAccessExpression) =
        visitAssignableExpression(packageAccessExpression, null)

    override fun visitPackageAccessExpression(packageAccessExpression: JKPackageAccessExpression, data: Nothing?) =
        visitPackageAccessExpression(packageAccessExpression)

    fun visitClassAccessExpression(classAccessExpression: JKClassAccessExpression) = visitExpression(classAccessExpression, null)
    override fun visitClassAccessExpression(classAccessExpression: JKClassAccessExpression, data: Nothing?) =
        visitClassAccessExpression(classAccessExpression)

    fun visitArrayAccessExpression(arrayAccessExpression: JKArrayAccessExpression) = visitAssignableExpression(arrayAccessExpression, null)
    override fun visitArrayAccessExpression(arrayAccessExpression: JKArrayAccessExpression, data: Nothing?) =
        visitArrayAccessExpression(arrayAccessExpression)

    fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression) = visitExpression(parenthesizedExpression, null)
    override fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression, data: Nothing?) =
        visitParenthesizedExpression(parenthesizedExpression)

    fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression) = visitExpression(typeCastExpression, null)
    override fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression, data: Nothing?) =
        visitTypeCastExpression(typeCastExpression)

    fun visitExpressionList(expressionList: JKExpressionList) = visitTreeElement(expressionList, null)
    override fun visitExpressionList(expressionList: JKExpressionList, data: Nothing?) = visitExpressionList(expressionList)
    fun visitArgument(argument: JKArgument) = visitTreeElement(argument, null)
    override fun visitArgument(argument: JKArgument, data: Nothing?) = visitArgument(argument)
    fun visitNamedArgument(namedArgument: JKNamedArgument) = visitArgument(namedArgument, null)
    override fun visitNamedArgument(namedArgument: JKNamedArgument, data: Nothing?) = visitNamedArgument(namedArgument)
    fun visitArgumentList(argumentList: JKArgumentList) = visitTreeElement(argumentList, null)
    override fun visitArgumentList(argumentList: JKArgumentList, data: Nothing?) = visitArgumentList(argumentList)
    fun visitLiteralExpression(literalExpression: JKLiteralExpression) = visitExpression(literalExpression, null)
    override fun visitLiteralExpression(literalExpression: JKLiteralExpression, data: Nothing?) = visitLiteralExpression(literalExpression)
    fun visitParameter(parameter: JKParameter) = visitVariable(parameter, null)
    override fun visitParameter(parameter: JKParameter, data: Nothing?) = visitParameter(parameter)
    fun visitStringLiteralExpression(stringLiteralExpression: JKStringLiteralExpression) =
        visitLiteralExpression(stringLiteralExpression, null)

    override fun visitStringLiteralExpression(stringLiteralExpression: JKStringLiteralExpression, data: Nothing?) =
        visitStringLiteralExpression(stringLiteralExpression)

    fun visitStubExpression(stubExpression: JKStubExpression) = visitExpression(stubExpression, null)
    override fun visitStubExpression(stubExpression: JKStubExpression, data: Nothing?) = visitStubExpression(stubExpression)
    fun visitLoopStatement(loopStatement: JKLoopStatement) = visitStatement(loopStatement, null)
    override fun visitLoopStatement(loopStatement: JKLoopStatement, data: Nothing?) = visitLoopStatement(loopStatement)
    fun visitBlockStatement(blockStatement: JKBlockStatement) = visitStatement(blockStatement, null)
    override fun visitBlockStatement(blockStatement: JKBlockStatement, data: Nothing?) = visitBlockStatement(blockStatement)
    fun visitBlockStatementWithoutBrackets(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets) =
        visitStatement(blockStatementWithoutBrackets, null)

    override fun visitBlockStatementWithoutBrackets(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets, data: Nothing?) =
        visitBlockStatementWithoutBrackets(blockStatementWithoutBrackets)

    fun visitThisExpression(thisExpression: JKThisExpression) = visitExpression(thisExpression, null)
    override fun visitThisExpression(thisExpression: JKThisExpression, data: Nothing?) = visitThisExpression(thisExpression)
    fun visitSuperExpression(superExpression: JKSuperExpression) = visitExpression(superExpression, null)
    override fun visitSuperExpression(superExpression: JKSuperExpression, data: Nothing?) = visitSuperExpression(superExpression)
    fun visitWhileStatement(whileStatement: JKWhileStatement) = visitLoopStatement(whileStatement, null)
    override fun visitWhileStatement(whileStatement: JKWhileStatement, data: Nothing?) = visitWhileStatement(whileStatement)
    fun visitDoWhileStatement(doWhileStatement: JKDoWhileStatement) = visitLoopStatement(doWhileStatement, null)
    override fun visitDoWhileStatement(doWhileStatement: JKDoWhileStatement, data: Nothing?) = visitDoWhileStatement(doWhileStatement)
    fun visitBreakStatement(breakStatement: JKBreakStatement) = visitStatement(breakStatement, null)
    override fun visitBreakStatement(breakStatement: JKBreakStatement, data: Nothing?) = visitBreakStatement(breakStatement)
    fun visitBreakWithLabelStatement(breakWithLabelStatement: JKBreakWithLabelStatement) =
        visitBreakStatement(breakWithLabelStatement, null)

    override fun visitBreakWithLabelStatement(breakWithLabelStatement: JKBreakWithLabelStatement, data: Nothing?) =
        visitBreakWithLabelStatement(breakWithLabelStatement)

    fun visitIfStatement(ifStatement: JKIfStatement) = visitStatement(ifStatement, null)
    override fun visitIfStatement(ifStatement: JKIfStatement, data: Nothing?) = visitIfStatement(ifStatement)
    fun visitIfElseStatement(ifElseStatement: JKIfElseStatement) = visitIfStatement(ifElseStatement, null)
    override fun visitIfElseStatement(ifElseStatement: JKIfElseStatement, data: Nothing?) = visitIfElseStatement(ifElseStatement)
    fun visitIfElseExpression(ifElseExpression: JKIfElseExpression) = visitExpression(ifElseExpression, null)
    override fun visitIfElseExpression(ifElseExpression: JKIfElseExpression, data: Nothing?) = visitIfElseExpression(ifElseExpression)
    fun visitAssignableExpression(assignableExpression: JKAssignableExpression) = visitExpression(assignableExpression, null)
    override fun visitAssignableExpression(assignableExpression: JKAssignableExpression, data: Nothing?) =
        visitAssignableExpression(assignableExpression)

    fun visitLambdaExpression(lambdaExpression: JKLambdaExpression) = visitExpression(lambdaExpression, null)
    override fun visitLambdaExpression(lambdaExpression: JKLambdaExpression, data: Nothing?) = visitLambdaExpression(lambdaExpression)
    fun visitDelegationConstructorCall(delegationConstructorCall: JKDelegationConstructorCall) =
        visitMethodCallExpression(delegationConstructorCall, null)

    override fun visitDelegationConstructorCall(delegationConstructorCall: JKDelegationConstructorCall, data: Nothing?) =
        visitDelegationConstructorCall(delegationConstructorCall)

    fun visitLabel(label: JKLabel) = visitTreeElement(label, null)
    override fun visitLabel(label: JKLabel, data: Nothing?) = visitLabel(label)
    fun visitLabelEmpty(labelEmpty: JKLabelEmpty) = visitLabel(labelEmpty, null)
    override fun visitLabelEmpty(labelEmpty: JKLabelEmpty, data: Nothing?) = visitLabelEmpty(labelEmpty)
    fun visitLabelText(labelText: JKLabelText) = visitLabel(labelText, null)
    override fun visitLabelText(labelText: JKLabelText, data: Nothing?) = visitLabelText(labelText)
    fun visitContinueStatement(continueStatement: JKContinueStatement) = visitStatement(continueStatement, null)
    override fun visitContinueStatement(continueStatement: JKContinueStatement, data: Nothing?) = visitContinueStatement(continueStatement)
    fun visitLabeledStatement(labeledStatement: JKLabeledStatement) = visitExpression(labeledStatement, null)
    override fun visitLabeledStatement(labeledStatement: JKLabeledStatement, data: Nothing?) = visitLabeledStatement(labeledStatement)
    fun visitEmptyStatement(emptyStatement: JKEmptyStatement) = visitStatement(emptyStatement, null)
    override fun visitEmptyStatement(emptyStatement: JKEmptyStatement, data: Nothing?) = visitEmptyStatement(emptyStatement)
    fun visitTypeParameterList(typeParameterList: JKTypeParameterList) = visitTreeElement(typeParameterList, null)
    override fun visitTypeParameterList(typeParameterList: JKTypeParameterList, data: Nothing?) = visitTypeParameterList(typeParameterList)
    fun visitTypeParameter(typeParameter: JKTypeParameter) = visitTreeElement(typeParameter, null)
    override fun visitTypeParameter(typeParameter: JKTypeParameter, data: Nothing?) = visitTypeParameter(typeParameter)
    fun visitTypeParameterListOwner(typeParameterListOwner: JKTypeParameterListOwner) = visitTreeElement(typeParameterListOwner, null)
    override fun visitTypeParameterListOwner(typeParameterListOwner: JKTypeParameterListOwner, data: Nothing?) =
        visitTypeParameterListOwner(typeParameterListOwner)

    fun visitEnumConstant(enumConstant: JKEnumConstant) = visitVariable(enumConstant, null)
    override fun visitEnumConstant(enumConstant: JKEnumConstant, data: Nothing?) = visitEnumConstant(enumConstant)
    fun visitForInStatement(forInStatement: JKForInStatement) = visitStatement(forInStatement, null)
    override fun visitForInStatement(forInStatement: JKForInStatement, data: Nothing?) = visitForInStatement(forInStatement)
    fun visitPackageDeclaration(packageDeclaration: JKPackageDeclaration) = visitDeclaration(packageDeclaration, null)
    override fun visitPackageDeclaration(packageDeclaration: JKPackageDeclaration, data: Nothing?) =
        visitPackageDeclaration(packageDeclaration)

    fun visitClassLiteralExpression(classLiteralExpression: JKClassLiteralExpression) = visitExpression(classLiteralExpression, null)
    override fun visitClassLiteralExpression(classLiteralExpression: JKClassLiteralExpression, data: Nothing?) =
        visitClassLiteralExpression(classLiteralExpression)

    fun visitAnnotationMemberValue(annotationMemberValue: JKAnnotationMemberValue) = visitTreeElement(annotationMemberValue, null)
    override fun visitAnnotationMemberValue(annotationMemberValue: JKAnnotationMemberValue, data: Nothing?) =
        visitAnnotationMemberValue(annotationMemberValue)

    fun visitField(field: JKField) = visitVariable(field, null)
    override fun visitField(field: JKField, data: Nothing?) = visitField(field)
    fun visitJavaField(javaField: JKJavaField) = visitField(javaField, null)
    override fun visitJavaField(javaField: JKJavaField, data: Nothing?) = visitJavaField(javaField)
    fun visitJavaMethod(javaMethod: JKJavaMethod) = visitMethod(javaMethod, null)
    override fun visitJavaMethod(javaMethod: JKJavaMethod, data: Nothing?) = visitJavaMethod(javaMethod)
    fun visitJavaMethodCallExpression(javaMethodCallExpression: JKJavaMethodCallExpression) =
        visitMethodCallExpression(javaMethodCallExpression, null)

    override fun visitJavaMethodCallExpression(javaMethodCallExpression: JKJavaMethodCallExpression, data: Nothing?) =
        visitJavaMethodCallExpression(javaMethodCallExpression)

    fun visitClassBody(classBody: JKClassBody) = visitTreeElement(classBody, null)
    override fun visitClassBody(classBody: JKClassBody, data: Nothing?) = visitClassBody(classBody)
    fun visitEmptyClassBody(emptyClassBody: JKEmptyClassBody) = visitClassBody(emptyClassBody, null)
    override fun visitEmptyClassBody(emptyClassBody: JKEmptyClassBody, data: Nothing?) = visitEmptyClassBody(emptyClassBody)
    fun visitJavaNewExpression(javaNewExpression: JKJavaNewExpression) = visitExpression(javaNewExpression, null)
    override fun visitJavaNewExpression(javaNewExpression: JKJavaNewExpression, data: Nothing?) = visitJavaNewExpression(javaNewExpression)
    fun visitJavaDefaultNewExpression(javaDefaultNewExpression: JKJavaDefaultNewExpression) =
        visitExpression(javaDefaultNewExpression, null)

    override fun visitJavaDefaultNewExpression(javaDefaultNewExpression: JKJavaDefaultNewExpression, data: Nothing?) =
        visitJavaDefaultNewExpression(javaDefaultNewExpression)

    fun visitJavaNewEmptyArray(javaNewEmptyArray: JKJavaNewEmptyArray) = visitExpression(javaNewEmptyArray, null)
    override fun visitJavaNewEmptyArray(javaNewEmptyArray: JKJavaNewEmptyArray, data: Nothing?) = visitJavaNewEmptyArray(javaNewEmptyArray)
    fun visitJavaNewArray(javaNewArray: JKJavaNewArray) = visitExpression(javaNewArray, null)
    override fun visitJavaNewArray(javaNewArray: JKJavaNewArray, data: Nothing?) = visitJavaNewArray(javaNewArray)
    fun visitJavaLiteralExpression(javaLiteralExpression: JKJavaLiteralExpression) = visitLiteralExpression(javaLiteralExpression, null)
    override fun visitJavaLiteralExpression(javaLiteralExpression: JKJavaLiteralExpression, data: Nothing?) =
        visitJavaLiteralExpression(javaLiteralExpression)

    fun visitReturnStatement(returnStatement: JKReturnStatement) = visitStatement(returnStatement, null)
    override fun visitReturnStatement(returnStatement: JKReturnStatement, data: Nothing?) = visitReturnStatement(returnStatement)
    fun visitJavaAssertStatement(javaAssertStatement: JKJavaAssertStatement) = visitStatement(javaAssertStatement, null)
    override fun visitJavaAssertStatement(javaAssertStatement: JKJavaAssertStatement, data: Nothing?) =
        visitJavaAssertStatement(javaAssertStatement)

    fun visitJavaForLoopStatement(javaForLoopStatement: JKJavaForLoopStatement) = visitLoopStatement(javaForLoopStatement, null)
    override fun visitJavaForLoopStatement(javaForLoopStatement: JKJavaForLoopStatement, data: Nothing?) =
        visitJavaForLoopStatement(javaForLoopStatement)

    fun visitJavaPolyadicExpression(javaPolyadicExpression: JKJavaPolyadicExpression) = visitExpression(javaPolyadicExpression, null)
    override fun visitJavaPolyadicExpression(javaPolyadicExpression: JKJavaPolyadicExpression, data: Nothing?) =
        visitJavaPolyadicExpression(javaPolyadicExpression)

    fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression) =
        visitExpression(javaAssignmentExpression, null)

    override fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: Nothing?) =
        visitJavaAssignmentExpression(javaAssignmentExpression)

    fun visitJavaThrowStatement(javaThrowStatement: JKJavaThrowStatement) = visitStatement(javaThrowStatement, null)
    override fun visitJavaThrowStatement(javaThrowStatement: JKJavaThrowStatement, data: Nothing?) =
        visitJavaThrowStatement(javaThrowStatement)

    fun visitJavaTryStatement(javaTryStatement: JKJavaTryStatement) = visitStatement(javaTryStatement, null)
    override fun visitJavaTryStatement(javaTryStatement: JKJavaTryStatement, data: Nothing?) = visitJavaTryStatement(javaTryStatement)
    fun visitJavaTryCatchSection(javaTryCatchSection: JKJavaTryCatchSection) = visitTreeElement(javaTryCatchSection, null)
    override fun visitJavaTryCatchSection(javaTryCatchSection: JKJavaTryCatchSection, data: Nothing?) =
        visitJavaTryCatchSection(javaTryCatchSection)

    fun visitJavaSwitchStatement(javaSwitchStatement: JKJavaSwitchStatement) = visitStatement(javaSwitchStatement, null)
    override fun visitJavaSwitchStatement(javaSwitchStatement: JKJavaSwitchStatement, data: Nothing?) =
        visitJavaSwitchStatement(javaSwitchStatement)

    fun visitJavaSwitchCase(javaSwitchCase: JKJavaSwitchCase) = visitTreeElement(javaSwitchCase, null)
    override fun visitJavaSwitchCase(javaSwitchCase: JKJavaSwitchCase, data: Nothing?) = visitJavaSwitchCase(javaSwitchCase)
    fun visitJavaDefaultSwitchCase(javaDefaultSwitchCase: JKJavaDefaultSwitchCase) = visitJavaSwitchCase(javaDefaultSwitchCase, null)
    override fun visitJavaDefaultSwitchCase(javaDefaultSwitchCase: JKJavaDefaultSwitchCase, data: Nothing?) =
        visitJavaDefaultSwitchCase(javaDefaultSwitchCase)

    fun visitJavaLabelSwitchCase(javaLabelSwitchCase: JKJavaLabelSwitchCase) = visitJavaSwitchCase(javaLabelSwitchCase, null)
    override fun visitJavaLabelSwitchCase(javaLabelSwitchCase: JKJavaLabelSwitchCase, data: Nothing?) =
        visitJavaLabelSwitchCase(javaLabelSwitchCase)

    fun visitJavaContinueStatement(javaContinueStatement: JKJavaContinueStatement) = visitStatement(javaContinueStatement, null)
    override fun visitJavaContinueStatement(javaContinueStatement: JKJavaContinueStatement, data: Nothing?) =
        visitJavaContinueStatement(javaContinueStatement)

    fun visitJavaSynchronizedStatement(javaSynchronizedStatement: JKJavaSynchronizedStatement) =
        visitStatement(javaSynchronizedStatement, null)

    override fun visitJavaSynchronizedStatement(javaSynchronizedStatement: JKJavaSynchronizedStatement, data: Nothing?) =
        visitJavaSynchronizedStatement(javaSynchronizedStatement)

    fun visitJavaAnnotationMethod(javaAnnotationMethod: JKJavaAnnotationMethod) = visitMethod(javaAnnotationMethod, null)
    override fun visitJavaAnnotationMethod(javaAnnotationMethod: JKJavaAnnotationMethod, data: Nothing?) =
        visitJavaAnnotationMethod(javaAnnotationMethod)

    fun visitJavaStaticInitDeclaration(javaStaticInitDeclaration: JKJavaStaticInitDeclaration) =
        visitDeclaration(javaStaticInitDeclaration, null)

    override fun visitJavaStaticInitDeclaration(javaStaticInitDeclaration: JKJavaStaticInitDeclaration, data: Nothing?) =
        visitJavaStaticInitDeclaration(javaStaticInitDeclaration)

    fun visitKtGetterOrSetter(ktGetterOrSetter: JKKtGetterOrSetter) = visitTreeElement(ktGetterOrSetter, null)
    override fun visitKtGetterOrSetter(ktGetterOrSetter: JKKtGetterOrSetter, data: Nothing?) = visitKtGetterOrSetter(ktGetterOrSetter)
    fun visitKtEmptyGetterOrSetter(ktEmptyGetterOrSetter: JKKtEmptyGetterOrSetter) = visitKtGetterOrSetter(ktEmptyGetterOrSetter, null)
    override fun visitKtEmptyGetterOrSetter(ktEmptyGetterOrSetter: JKKtEmptyGetterOrSetter, data: Nothing?) =
        visitKtEmptyGetterOrSetter(ktEmptyGetterOrSetter)

    fun visitKtProperty(ktProperty: JKKtProperty) = visitField(ktProperty, null)
    override fun visitKtProperty(ktProperty: JKKtProperty, data: Nothing?) = visitKtProperty(ktProperty)
    fun visitKtFunction(ktFunction: JKKtFunction) = visitMethod(ktFunction, null)
    override fun visitKtFunction(ktFunction: JKKtFunction, data: Nothing?) = visitKtFunction(ktFunction)
    fun visitKtConstructor(ktConstructor: JKKtConstructor) = visitMethod(ktConstructor, null)
    override fun visitKtConstructor(ktConstructor: JKKtConstructor, data: Nothing?) = visitKtConstructor(ktConstructor)
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) = visitKtConstructor(ktPrimaryConstructor, null)
    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: Nothing?) =
        visitKtPrimaryConstructor(ktPrimaryConstructor)

    fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement) = visitStatement(ktAssignmentStatement, null)
    override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: Nothing?) =
        visitKtAssignmentStatement(ktAssignmentStatement)

    fun visitKtCall(ktCall: JKKtCall) = visitMethodCallExpression(ktCall, null)
    override fun visitKtCall(ktCall: JKKtCall, data: Nothing?) = visitKtCall(ktCall)
    fun visitKtMethodCallExpression(ktMethodCallExpression: JKKtMethodCallExpression) =
        visitMethodCallExpression(ktMethodCallExpression, null)

    override fun visitKtMethodCallExpression(ktMethodCallExpression: JKKtMethodCallExpression, data: Nothing?) =
        visitKtMethodCallExpression(ktMethodCallExpression)

    fun visitKtAlsoCallExpression(ktAlsoCallExpression: JKKtAlsoCallExpression) = visitKtMethodCallExpression(ktAlsoCallExpression, null)
    override fun visitKtAlsoCallExpression(ktAlsoCallExpression: JKKtAlsoCallExpression, data: Nothing?) =
        visitKtAlsoCallExpression(ktAlsoCallExpression)

    fun visitKtLiteralExpression(ktLiteralExpression: JKKtLiteralExpression) = visitLiteralExpression(ktLiteralExpression, null)
    override fun visitKtLiteralExpression(ktLiteralExpression: JKKtLiteralExpression, data: Nothing?) =
        visitKtLiteralExpression(ktLiteralExpression)

    fun visitKtWhenStatement(ktWhenStatement: JKKtWhenStatement) = visitStatement(ktWhenStatement, null)
    override fun visitKtWhenStatement(ktWhenStatement: JKKtWhenStatement, data: Nothing?) = visitKtWhenStatement(ktWhenStatement)
    fun visitKtWhenCase(ktWhenCase: JKKtWhenCase) = visitTreeElement(ktWhenCase, null)
    override fun visitKtWhenCase(ktWhenCase: JKKtWhenCase, data: Nothing?) = visitKtWhenCase(ktWhenCase)
    fun visitKtWhenLabel(ktWhenLabel: JKKtWhenLabel) = visitTreeElement(ktWhenLabel, null)
    override fun visitKtWhenLabel(ktWhenLabel: JKKtWhenLabel, data: Nothing?) = visitKtWhenLabel(ktWhenLabel)
    fun visitKtElseWhenLabel(ktElseWhenLabel: JKKtElseWhenLabel) = visitKtWhenLabel(ktElseWhenLabel, null)
    override fun visitKtElseWhenLabel(ktElseWhenLabel: JKKtElseWhenLabel, data: Nothing?) = visitKtElseWhenLabel(ktElseWhenLabel)
    fun visitKtValueWhenLabel(ktValueWhenLabel: JKKtValueWhenLabel) = visitKtWhenLabel(ktValueWhenLabel, null)
    override fun visitKtValueWhenLabel(ktValueWhenLabel: JKKtValueWhenLabel, data: Nothing?) = visitKtValueWhenLabel(ktValueWhenLabel)
    fun visitKtIsExpression(ktIsExpression: JKKtIsExpression) = visitExpression(ktIsExpression, null)
    override fun visitKtIsExpression(ktIsExpression: JKKtIsExpression, data: Nothing?) = visitKtIsExpression(ktIsExpression)
    fun visitKtInitDeclaration(ktInitDeclaration: JKKtInitDeclaration) = visitDeclaration(ktInitDeclaration, null)
    override fun visitKtInitDeclaration(ktInitDeclaration: JKKtInitDeclaration, data: Nothing?) = visitKtInitDeclaration(ktInitDeclaration)
    fun visitKtConvertedFromForLoopSyntheticWhileStatement(ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement) =
        visitStatement(ktConvertedFromForLoopSyntheticWhileStatement, null)

    override fun visitKtConvertedFromForLoopSyntheticWhileStatement(
        ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement,
        data: Nothing?
    ) = visitKtConvertedFromForLoopSyntheticWhileStatement(ktConvertedFromForLoopSyntheticWhileStatement)

    fun visitKtThrowExpression(ktThrowExpression: JKKtThrowExpression) = visitExpression(ktThrowExpression, null)
    override fun visitKtThrowExpression(ktThrowExpression: JKKtThrowExpression, data: Nothing?) = visitKtThrowExpression(ktThrowExpression)
    fun visitKtTryExpression(ktTryExpression: JKKtTryExpression) = visitExpression(ktTryExpression, null)
    override fun visitKtTryExpression(ktTryExpression: JKKtTryExpression, data: Nothing?) = visitKtTryExpression(ktTryExpression)
    fun visitKtTryCatchSection(ktTryCatchSection: JKKtTryCatchSection) = visitTreeElement(ktTryCatchSection, null)
    override fun visitKtTryCatchSection(ktTryCatchSection: JKKtTryCatchSection, data: Nothing?) = visitKtTryCatchSection(ktTryCatchSection)
    fun visitKtAnnotationArrayInitializerExpression(ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression) =
        visitExpression(ktAnnotationArrayInitializerExpression, null)

    override fun visitKtAnnotationArrayInitializerExpression(
        ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression,
        data: Nothing?
    ) = visitKtAnnotationArrayInitializerExpression(ktAnnotationArrayInitializerExpression)
}
