package org.jetbrains.kotlin.nj2k.tree.visitors

import org.jetbrains.kotlin.nj2k.tree.*

interface JKVisitorWithCommentsPrinting : JKVisitorVoid {
    fun printLeftNonCodeElements(element: JKNonCodeElementsListOwner)
    fun printRightNonCodeElements(element: JKNonCodeElementsListOwner)

    override fun visitTreeElement(treeElement: JKTreeElement) {
        printLeftNonCodeElements(treeElement)
        visitTreeElementRaw(treeElement)
        printRightNonCodeElements(treeElement)
    }

    fun visitTreeElementRaw(treeElement: JKTreeElement)

    override fun visitTreeRoot(treeRoot: JKTreeRoot) {
        printLeftNonCodeElements(treeRoot)
        visitTreeRootRaw(treeRoot)
        printRightNonCodeElements(treeRoot)
    }

    fun visitTreeRootRaw(treeRoot: JKTreeRoot) = visitTreeElementRaw(treeRoot)

    override fun visitDeclaration(declaration: JKDeclaration) {
        printLeftNonCodeElements(declaration)
        visitDeclarationRaw(declaration)
        printRightNonCodeElements(declaration)
    }

    fun visitDeclarationRaw(declaration: JKDeclaration) = visitTreeElementRaw(declaration)

    override fun visitImportStatement(importStatement: JKImportStatement) {
        printLeftNonCodeElements(importStatement)
        visitImportStatementRaw(importStatement)
        printRightNonCodeElements(importStatement)
    }

    fun visitImportStatementRaw(importStatement: JKImportStatement) = visitTreeElementRaw(importStatement)

    override fun visitImportList(importList: JKImportList) {
        printLeftNonCodeElements(importList)
        visitImportListRaw(importList)
        printRightNonCodeElements(importList)
    }

    fun visitImportListRaw(importList: JKImportList) = visitTreeElementRaw(importList)

    override fun visitFile(file: JKFile) {
        printLeftNonCodeElements(file)
        visitFileRaw(file)
        printRightNonCodeElements(file)
    }

    fun visitFileRaw(file: JKFile) = visitTreeElementRaw(file)

    override fun visitClass(klass: JKClass) {
        printLeftNonCodeElements(klass)
        visitClassRaw(klass)
        printRightNonCodeElements(klass)
    }

    fun visitClassRaw(klass: JKClass) = visitDeclarationRaw(klass)

    override fun visitInheritanceInfo(inheritanceInfo: JKInheritanceInfo) {
        printLeftNonCodeElements(inheritanceInfo)
        visitInheritanceInfoRaw(inheritanceInfo)
        printRightNonCodeElements(inheritanceInfo)
    }

    fun visitInheritanceInfoRaw(inheritanceInfo: JKInheritanceInfo) = visitTreeElementRaw(inheritanceInfo)

    override fun visitAnnotationList(annotationList: JKAnnotationList) {
        printLeftNonCodeElements(annotationList)
        visitAnnotationListRaw(annotationList)
        printRightNonCodeElements(annotationList)
    }

    fun visitAnnotationListRaw(annotationList: JKAnnotationList) = visitTreeElementRaw(annotationList)

    override fun visitAnnotation(annotation: JKAnnotation) {
        printLeftNonCodeElements(annotation)
        visitAnnotationRaw(annotation)
        printRightNonCodeElements(annotation)
    }

    fun visitAnnotationRaw(annotation: JKAnnotation) = visitAnnotationMemberValueRaw(annotation)

    override fun visitAnnotationParameter(annotationParameter: JKAnnotationParameter) {
        printLeftNonCodeElements(annotationParameter)
        visitAnnotationParameterRaw(annotationParameter)
        printRightNonCodeElements(annotationParameter)
    }

    fun visitAnnotationParameterRaw(annotationParameter: JKAnnotationParameter) = visitTreeElementRaw(annotationParameter)

    override fun visitAnnotationNameParameter(annotationNameParameter: JKAnnotationNameParameter) {
        printLeftNonCodeElements(annotationNameParameter)
        visitAnnotationNameParameterRaw(annotationNameParameter)
        printRightNonCodeElements(annotationNameParameter)
    }

    fun visitAnnotationNameParameterRaw(annotationNameParameter: JKAnnotationNameParameter) =
        visitAnnotationParameterRaw(annotationNameParameter)

    override fun visitAnnotationListOwner(annotationListOwner: JKAnnotationListOwner) {
        printLeftNonCodeElements(annotationListOwner)
        visitAnnotationListOwnerRaw(annotationListOwner)
        printRightNonCodeElements(annotationListOwner)
    }

    fun visitAnnotationListOwnerRaw(annotationListOwner: JKAnnotationListOwner) = visitTreeElementRaw(annotationListOwner)

    override fun visitMethod(method: JKMethod) {
        printLeftNonCodeElements(method)
        visitMethodRaw(method)
        printRightNonCodeElements(method)
    }

    fun visitMethodRaw(method: JKMethod) = visitDeclarationRaw(method)

    override fun visitVariable(variable: JKVariable) {
        printLeftNonCodeElements(variable)
        visitVariableRaw(variable)
        printRightNonCodeElements(variable)
    }

    fun visitVariableRaw(variable: JKVariable) = visitDeclarationRaw(variable)

    override fun visitForLoopVariable(forLoopVariable: JKForLoopVariable) {
        printLeftNonCodeElements(forLoopVariable)
        visitForLoopVariableRaw(forLoopVariable)
        printRightNonCodeElements(forLoopVariable)
    }

    fun visitForLoopVariableRaw(forLoopVariable: JKForLoopVariable) = visitVariableRaw(forLoopVariable)

    override fun visitLocalVariable(localVariable: JKLocalVariable) {
        printLeftNonCodeElements(localVariable)
        visitLocalVariableRaw(localVariable)
        printRightNonCodeElements(localVariable)
    }

    fun visitLocalVariableRaw(localVariable: JKLocalVariable) = visitVariableRaw(localVariable)

    override fun visitModifierElement(modifierElement: JKModifierElement) {
        printLeftNonCodeElements(modifierElement)
        visitModifierElementRaw(modifierElement)
        printRightNonCodeElements(modifierElement)
    }

    fun visitModifierElementRaw(modifierElement: JKModifierElement) = visitTreeElementRaw(modifierElement)

    override fun visitMutabilityModifierElement(mutabilityModifierElement: JKMutabilityModifierElement) {
        printLeftNonCodeElements(mutabilityModifierElement)
        visitMutabilityModifierElementRaw(mutabilityModifierElement)
        printRightNonCodeElements(mutabilityModifierElement)
    }

    fun visitMutabilityModifierElementRaw(mutabilityModifierElement: JKMutabilityModifierElement) =
        visitModifierElementRaw(mutabilityModifierElement)

    override fun visitModalityModifierElement(modalityModifierElement: JKModalityModifierElement) {
        printLeftNonCodeElements(modalityModifierElement)
        visitModalityModifierElementRaw(modalityModifierElement)
        printRightNonCodeElements(modalityModifierElement)
    }

    fun visitModalityModifierElementRaw(modalityModifierElement: JKModalityModifierElement) =
        visitModifierElementRaw(modalityModifierElement)

    override fun visitVisibilityModifierElement(visibilityModifierElement: JKVisibilityModifierElement) {
        printLeftNonCodeElements(visibilityModifierElement)
        visitVisibilityModifierElementRaw(visibilityModifierElement)
        printRightNonCodeElements(visibilityModifierElement)
    }

    fun visitVisibilityModifierElementRaw(visibilityModifierElement: JKVisibilityModifierElement) =
        visitModifierElementRaw(visibilityModifierElement)

    override fun visitExtraModifierElement(otherModifierElement: JKOtherModifierElement) {
        printLeftNonCodeElements(otherModifierElement)
        visitExtraModifierElementRaw(otherModifierElement)
        printRightNonCodeElements(otherModifierElement)
    }

    fun visitExtraModifierElementRaw(otherModifierElement: JKOtherModifierElement) = visitModifierElementRaw(otherModifierElement)

    override fun visitExtraModifiersOwner(otherModifiersOwner: JKOtherModifiersOwner) {
        printLeftNonCodeElements(otherModifiersOwner)
        visitExtraModifiersOwnerRaw(otherModifiersOwner)
        printRightNonCodeElements(otherModifiersOwner)
    }

    fun visitExtraModifiersOwnerRaw(otherModifiersOwner: JKOtherModifiersOwner) = visitModifiersListOwnerRaw(otherModifiersOwner)

    override fun visitVisibilityOwner(visibilityOwner: JKVisibilityOwner) {
        printLeftNonCodeElements(visibilityOwner)
        visitVisibilityOwnerRaw(visibilityOwner)
        printRightNonCodeElements(visibilityOwner)
    }

    fun visitVisibilityOwnerRaw(visibilityOwner: JKVisibilityOwner) = visitModifiersListOwnerRaw(visibilityOwner)

    override fun visitModalityOwner(modalityOwner: JKModalityOwner) {
        printLeftNonCodeElements(modalityOwner)
        visitModalityOwnerRaw(modalityOwner)
        printRightNonCodeElements(modalityOwner)
    }

    fun visitModalityOwnerRaw(modalityOwner: JKModalityOwner) = visitModifiersListOwnerRaw(modalityOwner)

    override fun visitMutabilityOwner(mutabilityOwner: JKMutabilityOwner) {
        printLeftNonCodeElements(mutabilityOwner)
        visitMutabilityOwnerRaw(mutabilityOwner)
        printRightNonCodeElements(mutabilityOwner)
    }

    fun visitMutabilityOwnerRaw(mutabilityOwner: JKMutabilityOwner) = visitModifiersListOwnerRaw(mutabilityOwner)

    override fun visitModifiersListOwner(modifiersListOwner: JKModifiersListOwner) {
        printLeftNonCodeElements(modifiersListOwner)
        visitModifiersListOwnerRaw(modifiersListOwner)
        printRightNonCodeElements(modifiersListOwner)
    }

    fun visitModifiersListOwnerRaw(modifiersListOwner: JKModifiersListOwner) = visitTreeElementRaw(modifiersListOwner)

    override fun visitTypeElement(typeElement: JKTypeElement) {
        printLeftNonCodeElements(typeElement)
        visitTypeElementRaw(typeElement)
        printRightNonCodeElements(typeElement)
    }

    fun visitTypeElementRaw(typeElement: JKTypeElement) = visitTreeElementRaw(typeElement)

    override fun visitStatement(statement: JKStatement) {
        printLeftNonCodeElements(statement)
        visitStatementRaw(statement)
        printRightNonCodeElements(statement)
    }

    fun visitStatementRaw(statement: JKStatement) = visitTreeElementRaw(statement)

    override fun visitBlock(block: JKBlock) {
        printLeftNonCodeElements(block)
        visitBlockRaw(block)
        printRightNonCodeElements(block)
    }

    fun visitBlockRaw(block: JKBlock) = visitTreeElementRaw(block)

    override fun visitBodyStub(bodyStub: JKBodyStub) {
        printLeftNonCodeElements(bodyStub)
        visitBodyStubRaw(bodyStub)
        printRightNonCodeElements(bodyStub)
    }

    fun visitBodyStubRaw(bodyStub: JKBodyStub) = visitBlockRaw(bodyStub)

    override fun visitIdentifier(identifier: JKIdentifier) {
        printLeftNonCodeElements(identifier)
        visitIdentifierRaw(identifier)
        printRightNonCodeElements(identifier)
    }

    fun visitIdentifierRaw(identifier: JKIdentifier) = visitTreeElementRaw(identifier)

    override fun visitNameIdentifier(nameIdentifier: JKNameIdentifier) {
        printLeftNonCodeElements(nameIdentifier)
        visitNameIdentifierRaw(nameIdentifier)
        printRightNonCodeElements(nameIdentifier)
    }

    fun visitNameIdentifierRaw(nameIdentifier: JKNameIdentifier) = visitIdentifierRaw(nameIdentifier)

    override fun visitExpression(expression: JKExpression) {
        printLeftNonCodeElements(expression)
        visitExpressionRaw(expression)
        printRightNonCodeElements(expression)
    }

    fun visitExpressionRaw(expression: JKExpression) = visitTreeElementRaw(expression)

    override fun visitMethodReferenceExpression(methodReferenceExpression: JKMethodReferenceExpression) {
        printLeftNonCodeElements(methodReferenceExpression)
        visitMethodReferenceExpressionRaw(methodReferenceExpression)
        printRightNonCodeElements(methodReferenceExpression)
    }

    fun visitMethodReferenceExpressionRaw(methodReferenceExpression: JKMethodReferenceExpression) =
        visitExpressionRaw(methodReferenceExpression)

    override fun visitExpressionStatement(expressionStatement: JKExpressionStatement) {
        printLeftNonCodeElements(expressionStatement)
        visitExpressionStatementRaw(expressionStatement)
        printRightNonCodeElements(expressionStatement)
    }

    fun visitExpressionStatementRaw(expressionStatement: JKExpressionStatement) = visitStatementRaw(expressionStatement)

    override fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement) {
        printLeftNonCodeElements(declarationStatement)
        visitDeclarationStatementRaw(declarationStatement)
        printRightNonCodeElements(declarationStatement)
    }

    fun visitDeclarationStatementRaw(declarationStatement: JKDeclarationStatement) = visitStatementRaw(declarationStatement)

    override fun visitOperatorExpression(operatorExpression: JKOperatorExpression) {
        printLeftNonCodeElements(operatorExpression)
        visitOperatorExpressionRaw(operatorExpression)
        printRightNonCodeElements(operatorExpression)
    }

    fun visitOperatorExpressionRaw(operatorExpression: JKOperatorExpression) = visitExpressionRaw(operatorExpression)

    override fun visitBinaryExpression(binaryExpression: JKBinaryExpression) {
        printLeftNonCodeElements(binaryExpression)
        visitBinaryExpressionRaw(binaryExpression)
        printRightNonCodeElements(binaryExpression)
    }

    fun visitBinaryExpressionRaw(binaryExpression: JKBinaryExpression) = visitOperatorExpressionRaw(binaryExpression)

    override fun visitUnaryExpression(unaryExpression: JKUnaryExpression) {
        printLeftNonCodeElements(unaryExpression)
        visitUnaryExpressionRaw(unaryExpression)
        printRightNonCodeElements(unaryExpression)
    }

    fun visitUnaryExpressionRaw(unaryExpression: JKUnaryExpression) = visitOperatorExpressionRaw(unaryExpression)

    override fun visitPrefixExpression(prefixExpression: JKPrefixExpression) {
        printLeftNonCodeElements(prefixExpression)
        visitPrefixExpressionRaw(prefixExpression)
        printRightNonCodeElements(prefixExpression)
    }

    fun visitPrefixExpressionRaw(prefixExpression: JKPrefixExpression) = visitUnaryExpressionRaw(prefixExpression)

    override fun visitPostfixExpression(postfixExpression: JKPostfixExpression) {
        printLeftNonCodeElements(postfixExpression)
        visitPostfixExpressionRaw(postfixExpression)
        printRightNonCodeElements(postfixExpression)
    }

    fun visitPostfixExpressionRaw(postfixExpression: JKPostfixExpression) = visitUnaryExpressionRaw(postfixExpression)

    override fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression) {
        printLeftNonCodeElements(qualifiedExpression)
        visitQualifiedExpressionRaw(qualifiedExpression)
        printRightNonCodeElements(qualifiedExpression)
    }

    fun visitQualifiedExpressionRaw(qualifiedExpression: JKQualifiedExpression) = visitExpressionRaw(qualifiedExpression)

    override fun visitTypeArgumentList(typeArgumentList: JKTypeArgumentList) {
        printLeftNonCodeElements(typeArgumentList)
        visitTypeArgumentListRaw(typeArgumentList)
        printRightNonCodeElements(typeArgumentList)
    }

    fun visitTypeArgumentListRaw(typeArgumentList: JKTypeArgumentList) = visitTreeElementRaw(typeArgumentList)

    override fun visitTypeArgumentListOwner(typeArgumentListOwner: JKTypeArgumentListOwner) {
        printLeftNonCodeElements(typeArgumentListOwner)
        visitTypeArgumentListOwnerRaw(typeArgumentListOwner)
        printRightNonCodeElements(typeArgumentListOwner)
    }

    fun visitTypeArgumentListOwnerRaw(typeArgumentListOwner: JKTypeArgumentListOwner) = visitTreeElementRaw(typeArgumentListOwner)

    override fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression) {
        printLeftNonCodeElements(methodCallExpression)
        visitMethodCallExpressionRaw(methodCallExpression)
        printRightNonCodeElements(methodCallExpression)
    }

    fun visitMethodCallExpressionRaw(methodCallExpression: JKMethodCallExpression) = visitExpressionRaw(methodCallExpression)

    override fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression) {
        printLeftNonCodeElements(fieldAccessExpression)
        visitFieldAccessExpressionRaw(fieldAccessExpression)
        printRightNonCodeElements(fieldAccessExpression)
    }

    fun visitFieldAccessExpressionRaw(fieldAccessExpression: JKFieldAccessExpression) = visitAssignableExpressionRaw(fieldAccessExpression)

    override fun visitPackageAccessExpression(packageAccessExpression: JKPackageAccessExpression) {
        printLeftNonCodeElements(packageAccessExpression)
        visitPackageAccessExpressionRaw(packageAccessExpression)
        printRightNonCodeElements(packageAccessExpression)
    }

    fun visitPackageAccessExpressionRaw(packageAccessExpression: JKPackageAccessExpression) =
        visitAssignableExpressionRaw(packageAccessExpression)

    override fun visitClassAccessExpression(classAccessExpression: JKClassAccessExpression) {
        printLeftNonCodeElements(classAccessExpression)
        visitClassAccessExpressionRaw(classAccessExpression)
        printRightNonCodeElements(classAccessExpression)
    }

    fun visitClassAccessExpressionRaw(classAccessExpression: JKClassAccessExpression) = visitExpressionRaw(classAccessExpression)

    override fun visitArrayAccessExpression(arrayAccessExpression: JKArrayAccessExpression) {
        printLeftNonCodeElements(arrayAccessExpression)
        visitArrayAccessExpressionRaw(arrayAccessExpression)
        printRightNonCodeElements(arrayAccessExpression)
    }

    fun visitArrayAccessExpressionRaw(arrayAccessExpression: JKArrayAccessExpression) = visitAssignableExpressionRaw(arrayAccessExpression)

    override fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression) {
        printLeftNonCodeElements(parenthesizedExpression)
        visitParenthesizedExpressionRaw(parenthesizedExpression)
        printRightNonCodeElements(parenthesizedExpression)
    }

    fun visitParenthesizedExpressionRaw(parenthesizedExpression: JKParenthesizedExpression) = visitExpressionRaw(parenthesizedExpression)

    override fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression) {
        printLeftNonCodeElements(typeCastExpression)
        visitTypeCastExpressionRaw(typeCastExpression)
        printRightNonCodeElements(typeCastExpression)
    }

    fun visitTypeCastExpressionRaw(typeCastExpression: JKTypeCastExpression) = visitExpressionRaw(typeCastExpression)

    override fun visitExpressionList(expressionList: JKExpressionList) {
        printLeftNonCodeElements(expressionList)
        visitExpressionListRaw(expressionList)
        printRightNonCodeElements(expressionList)
    }

    fun visitExpressionListRaw(expressionList: JKExpressionList) = visitTreeElementRaw(expressionList)

    override fun visitArgument(argument: JKArgument) {
        printLeftNonCodeElements(argument)
        visitArgumentRaw(argument)
        printRightNonCodeElements(argument)
    }

    fun visitArgumentRaw(argument: JKArgument) = visitTreeElementRaw(argument)

    override fun visitNamedArgument(namedArgument: JKNamedArgument) {
        printLeftNonCodeElements(namedArgument)
        visitNamedArgumentRaw(namedArgument)
        printRightNonCodeElements(namedArgument)
    }

    fun visitNamedArgumentRaw(namedArgument: JKNamedArgument) = visitArgumentRaw(namedArgument)

    override fun visitArgumentList(argumentList: JKArgumentList) {
        printLeftNonCodeElements(argumentList)
        visitArgumentListRaw(argumentList)
        printRightNonCodeElements(argumentList)
    }

    fun visitArgumentListRaw(argumentList: JKArgumentList) = visitTreeElementRaw(argumentList)

    override fun visitLiteralExpression(literalExpression: JKLiteralExpression) {
        printLeftNonCodeElements(literalExpression)
        visitLiteralExpressionRaw(literalExpression)
        printRightNonCodeElements(literalExpression)
    }

    fun visitLiteralExpressionRaw(literalExpression: JKLiteralExpression) = visitExpressionRaw(literalExpression)

    override fun visitParameter(parameter: JKParameter) {
        printLeftNonCodeElements(parameter)
        visitParameterRaw(parameter)
        printRightNonCodeElements(parameter)
    }

    fun visitParameterRaw(parameter: JKParameter) = visitVariableRaw(parameter)

    override fun visitStringLiteralExpression(stringLiteralExpression: JKStringLiteralExpression) {
        printLeftNonCodeElements(stringLiteralExpression)
        visitStringLiteralExpressionRaw(stringLiteralExpression)
        printRightNonCodeElements(stringLiteralExpression)
    }

    fun visitStringLiteralExpressionRaw(stringLiteralExpression: JKStringLiteralExpression) =
        visitLiteralExpressionRaw(stringLiteralExpression)

    override fun visitStubExpression(stubExpression: JKStubExpression) {
        printLeftNonCodeElements(stubExpression)
        visitStubExpressionRaw(stubExpression)
        printRightNonCodeElements(stubExpression)
    }

    fun visitStubExpressionRaw(stubExpression: JKStubExpression) = visitExpressionRaw(stubExpression)

    override fun visitLoopStatement(loopStatement: JKLoopStatement) {
        printLeftNonCodeElements(loopStatement)
        visitLoopStatementRaw(loopStatement)
        printRightNonCodeElements(loopStatement)
    }

    fun visitLoopStatementRaw(loopStatement: JKLoopStatement) = visitStatementRaw(loopStatement)

    override fun visitBlockStatement(blockStatement: JKBlockStatement) {
        printLeftNonCodeElements(blockStatement)
        visitBlockStatementRaw(blockStatement)
        printRightNonCodeElements(blockStatement)
    }

    fun visitBlockStatementRaw(blockStatement: JKBlockStatement) = visitStatementRaw(blockStatement)

    override fun visitBlockStatementWithoutBrackets(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets) {
        printLeftNonCodeElements(blockStatementWithoutBrackets)
        visitBlockStatementWithoutBracketsRaw(blockStatementWithoutBrackets)
        printRightNonCodeElements(blockStatementWithoutBrackets)
    }

    fun visitBlockStatementWithoutBracketsRaw(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets) =
        visitStatementRaw(blockStatementWithoutBrackets)

    override fun visitThisExpression(thisExpression: JKThisExpression) {
        printLeftNonCodeElements(thisExpression)
        visitThisExpressionRaw(thisExpression)
        printRightNonCodeElements(thisExpression)
    }

    fun visitThisExpressionRaw(thisExpression: JKThisExpression) = visitExpressionRaw(thisExpression)

    override fun visitSuperExpression(superExpression: JKSuperExpression) {
        printLeftNonCodeElements(superExpression)
        visitSuperExpressionRaw(superExpression)
        printRightNonCodeElements(superExpression)
    }

    fun visitSuperExpressionRaw(superExpression: JKSuperExpression) = visitExpressionRaw(superExpression)

    override fun visitWhileStatement(whileStatement: JKWhileStatement) {
        printLeftNonCodeElements(whileStatement)
        visitWhileStatementRaw(whileStatement)
        printRightNonCodeElements(whileStatement)
    }

    fun visitWhileStatementRaw(whileStatement: JKWhileStatement) = visitLoopStatementRaw(whileStatement)

    override fun visitDoWhileStatement(doWhileStatement: JKDoWhileStatement) {
        printLeftNonCodeElements(doWhileStatement)
        visitDoWhileStatementRaw(doWhileStatement)
        printRightNonCodeElements(doWhileStatement)
    }

    fun visitDoWhileStatementRaw(doWhileStatement: JKDoWhileStatement) = visitLoopStatementRaw(doWhileStatement)

    override fun visitBreakStatement(breakStatement: JKBreakStatement) {
        printLeftNonCodeElements(breakStatement)
        visitBreakStatementRaw(breakStatement)
        printRightNonCodeElements(breakStatement)
    }

    fun visitBreakStatementRaw(breakStatement: JKBreakStatement) = visitStatementRaw(breakStatement)

    override fun visitBreakWithLabelStatement(breakWithLabelStatement: JKBreakWithLabelStatement) {
        printLeftNonCodeElements(breakWithLabelStatement)
        visitBreakWithLabelStatementRaw(breakWithLabelStatement)
        printRightNonCodeElements(breakWithLabelStatement)
    }

    fun visitBreakWithLabelStatementRaw(breakWithLabelStatement: JKBreakWithLabelStatement) =
        visitBreakStatementRaw(breakWithLabelStatement)

    override fun visitIfStatement(ifStatement: JKIfStatement) {
        printLeftNonCodeElements(ifStatement)
        visitIfStatementRaw(ifStatement)
        printRightNonCodeElements(ifStatement)
    }

    fun visitIfStatementRaw(ifStatement: JKIfStatement) = visitStatementRaw(ifStatement)

    override fun visitIfElseStatement(ifElseStatement: JKIfElseStatement) {
        printLeftNonCodeElements(ifElseStatement)
        visitIfElseStatementRaw(ifElseStatement)
        printRightNonCodeElements(ifElseStatement)
    }

    fun visitIfElseStatementRaw(ifElseStatement: JKIfElseStatement) = visitIfStatementRaw(ifElseStatement)

    override fun visitIfElseExpression(ifElseExpression: JKIfElseExpression) {
        printLeftNonCodeElements(ifElseExpression)
        visitIfElseExpressionRaw(ifElseExpression)
        printRightNonCodeElements(ifElseExpression)
    }

    fun visitIfElseExpressionRaw(ifElseExpression: JKIfElseExpression) = visitExpressionRaw(ifElseExpression)

    override fun visitAssignableExpression(assignableExpression: JKAssignableExpression) {
        printLeftNonCodeElements(assignableExpression)
        visitAssignableExpressionRaw(assignableExpression)
        printRightNonCodeElements(assignableExpression)
    }

    fun visitAssignableExpressionRaw(assignableExpression: JKAssignableExpression) = visitExpressionRaw(assignableExpression)

    override fun visitLambdaExpression(lambdaExpression: JKLambdaExpression) {
        printLeftNonCodeElements(lambdaExpression)
        visitLambdaExpressionRaw(lambdaExpression)
        printRightNonCodeElements(lambdaExpression)
    }

    fun visitLambdaExpressionRaw(lambdaExpression: JKLambdaExpression) = visitExpressionRaw(lambdaExpression)

    override fun visitDelegationConstructorCall(delegationConstructorCall: JKDelegationConstructorCall) {
        printLeftNonCodeElements(delegationConstructorCall)
        visitDelegationConstructorCallRaw(delegationConstructorCall)
        printRightNonCodeElements(delegationConstructorCall)
    }

    fun visitDelegationConstructorCallRaw(delegationConstructorCall: JKDelegationConstructorCall) =
        visitMethodCallExpressionRaw(delegationConstructorCall)

    override fun visitLabel(label: JKLabel) {
        printLeftNonCodeElements(label)
        visitLabelRaw(label)
        printRightNonCodeElements(label)
    }

    fun visitLabelRaw(label: JKLabel) = visitTreeElementRaw(label)

    override fun visitLabelEmpty(labelEmpty: JKLabelEmpty) {
        printLeftNonCodeElements(labelEmpty)
        visitLabelEmptyRaw(labelEmpty)
        printRightNonCodeElements(labelEmpty)
    }

    fun visitLabelEmptyRaw(labelEmpty: JKLabelEmpty) = visitLabelRaw(labelEmpty)

    override fun visitLabelText(labelText: JKLabelText) {
        printLeftNonCodeElements(labelText)
        visitLabelTextRaw(labelText)
        printRightNonCodeElements(labelText)
    }

    fun visitLabelTextRaw(labelText: JKLabelText) = visitLabelRaw(labelText)

    override fun visitContinueStatement(continueStatement: JKContinueStatement) {
        printLeftNonCodeElements(continueStatement)
        visitContinueStatementRaw(continueStatement)
        printRightNonCodeElements(continueStatement)
    }

    fun visitContinueStatementRaw(continueStatement: JKContinueStatement) = visitStatementRaw(continueStatement)

    override fun visitLabeledStatement(labeledStatement: JKLabeledStatement) {
        printLeftNonCodeElements(labeledStatement)
        visitLabeledStatementRaw(labeledStatement)
        printRightNonCodeElements(labeledStatement)
    }

    fun visitLabeledStatementRaw(labeledStatement: JKLabeledStatement) = visitExpressionRaw(labeledStatement)

    override fun visitEmptyStatement(emptyStatement: JKEmptyStatement) {
        printLeftNonCodeElements(emptyStatement)
        visitEmptyStatementRaw(emptyStatement)
        printRightNonCodeElements(emptyStatement)
    }

    fun visitEmptyStatementRaw(emptyStatement: JKEmptyStatement) = visitStatementRaw(emptyStatement)

    override fun visitTypeParameterList(typeParameterList: JKTypeParameterList) {
        printLeftNonCodeElements(typeParameterList)
        visitTypeParameterListRaw(typeParameterList)
        printRightNonCodeElements(typeParameterList)
    }

    fun visitTypeParameterListRaw(typeParameterList: JKTypeParameterList) = visitTreeElementRaw(typeParameterList)

    override fun visitTypeParameter(typeParameter: JKTypeParameter) {
        printLeftNonCodeElements(typeParameter)
        visitTypeParameterRaw(typeParameter)
        printRightNonCodeElements(typeParameter)
    }

    fun visitTypeParameterRaw(typeParameter: JKTypeParameter) = visitTreeElementRaw(typeParameter)

    override fun visitTypeParameterListOwner(typeParameterListOwner: JKTypeParameterListOwner) {
        printLeftNonCodeElements(typeParameterListOwner)
        visitTypeParameterListOwnerRaw(typeParameterListOwner)
        printRightNonCodeElements(typeParameterListOwner)
    }

    fun visitTypeParameterListOwnerRaw(typeParameterListOwner: JKTypeParameterListOwner) = visitTreeElementRaw(typeParameterListOwner)

    override fun visitEnumConstant(enumConstant: JKEnumConstant) {
        printLeftNonCodeElements(enumConstant)
        visitEnumConstantRaw(enumConstant)
        printRightNonCodeElements(enumConstant)
    }

    fun visitEnumConstantRaw(enumConstant: JKEnumConstant) = visitVariableRaw(enumConstant)

    override fun visitForInStatement(forInStatement: JKForInStatement) {
        printLeftNonCodeElements(forInStatement)
        visitForInStatementRaw(forInStatement)
        printRightNonCodeElements(forInStatement)
    }

    fun visitForInStatementRaw(forInStatement: JKForInStatement) = visitStatementRaw(forInStatement)

    override fun visitPackageDeclaration(packageDeclaration: JKPackageDeclaration) {
        printLeftNonCodeElements(packageDeclaration)
        visitPackageDeclarationRaw(packageDeclaration)
        printRightNonCodeElements(packageDeclaration)
    }

    fun visitPackageDeclarationRaw(packageDeclaration: JKPackageDeclaration) = visitDeclarationRaw(packageDeclaration)

    override fun visitClassLiteralExpression(classLiteralExpression: JKClassLiteralExpression) {
        printLeftNonCodeElements(classLiteralExpression)
        visitClassLiteralExpressionRaw(classLiteralExpression)
        printRightNonCodeElements(classLiteralExpression)
    }

    fun visitClassLiteralExpressionRaw(classLiteralExpression: JKClassLiteralExpression) = visitExpressionRaw(classLiteralExpression)

    override fun visitAnnotationMemberValue(annotationMemberValue: JKAnnotationMemberValue) {
        printLeftNonCodeElements(annotationMemberValue)
        visitAnnotationMemberValueRaw(annotationMemberValue)
        printRightNonCodeElements(annotationMemberValue)
    }

    fun visitAnnotationMemberValueRaw(annotationMemberValue: JKAnnotationMemberValue) = visitTreeElementRaw(annotationMemberValue)

    override fun visitField(field: JKField) {
        printLeftNonCodeElements(field)
        visitFieldRaw(field)
        printRightNonCodeElements(field)
    }

    fun visitFieldRaw(field: JKField) = visitVariableRaw(field)

    override fun visitJavaField(javaField: JKJavaField) {
        printLeftNonCodeElements(javaField)
        visitJavaFieldRaw(javaField)
        printRightNonCodeElements(javaField)
    }

    fun visitJavaFieldRaw(javaField: JKJavaField) = visitFieldRaw(javaField)

    override fun visitJavaMethod(javaMethod: JKJavaMethod) {
        printLeftNonCodeElements(javaMethod)
        visitJavaMethodRaw(javaMethod)
        printRightNonCodeElements(javaMethod)
    }

    fun visitJavaMethodRaw(javaMethod: JKJavaMethod) = visitMethodRaw(javaMethod)

    override fun visitJavaMethodCallExpression(javaMethodCallExpression: JKJavaMethodCallExpression) {
        printLeftNonCodeElements(javaMethodCallExpression)
        visitJavaMethodCallExpressionRaw(javaMethodCallExpression)
        printRightNonCodeElements(javaMethodCallExpression)
    }

    fun visitJavaMethodCallExpressionRaw(javaMethodCallExpression: JKJavaMethodCallExpression) =
        visitMethodCallExpressionRaw(javaMethodCallExpression)

    override fun visitClassBody(classBody: JKClassBody) {
        printLeftNonCodeElements(classBody)
        visitClassBodyRaw(classBody)
        printRightNonCodeElements(classBody)
    }

    fun visitClassBodyRaw(classBody: JKClassBody) = visitTreeElementRaw(classBody)

    override fun visitEmptyClassBody(emptyClassBody: JKEmptyClassBody) {
        printLeftNonCodeElements(emptyClassBody)
        visitEmptyClassBodyRaw(emptyClassBody)
        printRightNonCodeElements(emptyClassBody)
    }

    fun visitEmptyClassBodyRaw(emptyClassBody: JKEmptyClassBody) = visitClassBodyRaw(emptyClassBody)

    override fun visitJavaNewExpression(javaNewExpression: JKJavaNewExpression) {
        printLeftNonCodeElements(javaNewExpression)
        visitJavaNewExpressionRaw(javaNewExpression)
        printRightNonCodeElements(javaNewExpression)
    }

    fun visitJavaNewExpressionRaw(javaNewExpression: JKJavaNewExpression) = visitExpressionRaw(javaNewExpression)

    override fun visitJavaDefaultNewExpression(javaDefaultNewExpression: JKJavaDefaultNewExpression) {
        printLeftNonCodeElements(javaDefaultNewExpression)
        visitJavaDefaultNewExpressionRaw(javaDefaultNewExpression)
        printRightNonCodeElements(javaDefaultNewExpression)
    }

    fun visitJavaDefaultNewExpressionRaw(javaDefaultNewExpression: JKJavaDefaultNewExpression) =
        visitExpressionRaw(javaDefaultNewExpression)

    override fun visitJavaNewEmptyArray(javaNewEmptyArray: JKJavaNewEmptyArray) {
        printLeftNonCodeElements(javaNewEmptyArray)
        visitJavaNewEmptyArrayRaw(javaNewEmptyArray)
        printRightNonCodeElements(javaNewEmptyArray)
    }

    fun visitJavaNewEmptyArrayRaw(javaNewEmptyArray: JKJavaNewEmptyArray) = visitExpressionRaw(javaNewEmptyArray)

    override fun visitJavaNewArray(javaNewArray: JKJavaNewArray) {
        printLeftNonCodeElements(javaNewArray)
        visitJavaNewArrayRaw(javaNewArray)
        printRightNonCodeElements(javaNewArray)
    }

    fun visitJavaNewArrayRaw(javaNewArray: JKJavaNewArray) = visitExpressionRaw(javaNewArray)

    override fun visitJavaLiteralExpression(javaLiteralExpression: JKJavaLiteralExpression) {
        printLeftNonCodeElements(javaLiteralExpression)
        visitJavaLiteralExpressionRaw(javaLiteralExpression)
        printRightNonCodeElements(javaLiteralExpression)
    }

    fun visitJavaLiteralExpressionRaw(javaLiteralExpression: JKJavaLiteralExpression) = visitLiteralExpressionRaw(javaLiteralExpression)

    override fun visitReturnStatement(returnStatement: JKReturnStatement) {
        printLeftNonCodeElements(returnStatement)
        visitReturnStatementRaw(returnStatement)
        printRightNonCodeElements(returnStatement)
    }

    fun visitReturnStatementRaw(returnStatement: JKReturnStatement) = visitStatementRaw(returnStatement)

    override fun visitJavaAssertStatement(javaAssertStatement: JKJavaAssertStatement) {
        printLeftNonCodeElements(javaAssertStatement)
        visitJavaAssertStatementRaw(javaAssertStatement)
        printRightNonCodeElements(javaAssertStatement)
    }

    fun visitJavaAssertStatementRaw(javaAssertStatement: JKJavaAssertStatement) = visitStatementRaw(javaAssertStatement)

    override fun visitJavaForLoopStatement(javaForLoopStatement: JKJavaForLoopStatement) {
        printLeftNonCodeElements(javaForLoopStatement)
        visitJavaForLoopStatementRaw(javaForLoopStatement)
        printRightNonCodeElements(javaForLoopStatement)
    }

    fun visitJavaForLoopStatementRaw(javaForLoopStatement: JKJavaForLoopStatement) = visitLoopStatementRaw(javaForLoopStatement)

    override fun visitJavaPolyadicExpression(javaPolyadicExpression: JKJavaPolyadicExpression) {
        printLeftNonCodeElements(javaPolyadicExpression)
        visitJavaPolyadicExpressionRaw(javaPolyadicExpression)
        printRightNonCodeElements(javaPolyadicExpression)
    }

    fun visitJavaPolyadicExpressionRaw(javaPolyadicExpression: JKJavaPolyadicExpression) = visitExpressionRaw(javaPolyadicExpression)

    override fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression) {
        printLeftNonCodeElements(javaAssignmentExpression)
        visitJavaAssignmentExpressionRaw(javaAssignmentExpression)
        printRightNonCodeElements(javaAssignmentExpression)
    }

    fun visitJavaAssignmentExpressionRaw(javaAssignmentExpression: JKJavaAssignmentExpression) =
        visitExpressionRaw(javaAssignmentExpression)

    override fun visitJavaThrowStatement(javaThrowStatement: JKJavaThrowStatement) {
        printLeftNonCodeElements(javaThrowStatement)
        visitJavaThrowStatementRaw(javaThrowStatement)
        printRightNonCodeElements(javaThrowStatement)
    }

    fun visitJavaThrowStatementRaw(javaThrowStatement: JKJavaThrowStatement) = visitStatementRaw(javaThrowStatement)

    override fun visitJavaTryStatement(javaTryStatement: JKJavaTryStatement) {
        printLeftNonCodeElements(javaTryStatement)
        visitJavaTryStatementRaw(javaTryStatement)
        printRightNonCodeElements(javaTryStatement)
    }

    fun visitJavaTryStatementRaw(javaTryStatement: JKJavaTryStatement) = visitStatementRaw(javaTryStatement)

    override fun visitJavaTryCatchSection(javaTryCatchSection: JKJavaTryCatchSection) {
        printLeftNonCodeElements(javaTryCatchSection)
        visitJavaTryCatchSectionRaw(javaTryCatchSection)
        printRightNonCodeElements(javaTryCatchSection)
    }

    fun visitJavaTryCatchSectionRaw(javaTryCatchSection: JKJavaTryCatchSection) = visitTreeElementRaw(javaTryCatchSection)

    override fun visitJavaSwitchStatement(javaSwitchStatement: JKJavaSwitchStatement) {
        printLeftNonCodeElements(javaSwitchStatement)
        visitJavaSwitchStatementRaw(javaSwitchStatement)
        printRightNonCodeElements(javaSwitchStatement)
    }

    fun visitJavaSwitchStatementRaw(javaSwitchStatement: JKJavaSwitchStatement) = visitStatementRaw(javaSwitchStatement)

    override fun visitJavaSwitchCase(javaSwitchCase: JKJavaSwitchCase) {
        printLeftNonCodeElements(javaSwitchCase)
        visitJavaSwitchCaseRaw(javaSwitchCase)
        printRightNonCodeElements(javaSwitchCase)
    }

    fun visitJavaSwitchCaseRaw(javaSwitchCase: JKJavaSwitchCase) = visitTreeElementRaw(javaSwitchCase)

    override fun visitJavaDefaultSwitchCase(javaDefaultSwitchCase: JKJavaDefaultSwitchCase) {
        printLeftNonCodeElements(javaDefaultSwitchCase)
        visitJavaDefaultSwitchCaseRaw(javaDefaultSwitchCase)
        printRightNonCodeElements(javaDefaultSwitchCase)
    }

    fun visitJavaDefaultSwitchCaseRaw(javaDefaultSwitchCase: JKJavaDefaultSwitchCase) = visitJavaSwitchCaseRaw(javaDefaultSwitchCase)

    override fun visitJavaLabelSwitchCase(javaLabelSwitchCase: JKJavaLabelSwitchCase) {
        printLeftNonCodeElements(javaLabelSwitchCase)
        visitJavaLabelSwitchCaseRaw(javaLabelSwitchCase)
        printRightNonCodeElements(javaLabelSwitchCase)
    }

    fun visitJavaLabelSwitchCaseRaw(javaLabelSwitchCase: JKJavaLabelSwitchCase) = visitJavaSwitchCaseRaw(javaLabelSwitchCase)

    override fun visitJavaContinueStatement(javaContinueStatement: JKJavaContinueStatement) {
        printLeftNonCodeElements(javaContinueStatement)
        visitJavaContinueStatementRaw(javaContinueStatement)
        printRightNonCodeElements(javaContinueStatement)
    }

    fun visitJavaContinueStatementRaw(javaContinueStatement: JKJavaContinueStatement) = visitStatementRaw(javaContinueStatement)

    override fun visitJavaSynchronizedStatement(javaSynchronizedStatement: JKJavaSynchronizedStatement) {
        printLeftNonCodeElements(javaSynchronizedStatement)
        visitJavaSynchronizedStatementRaw(javaSynchronizedStatement)
        printRightNonCodeElements(javaSynchronizedStatement)
    }

    fun visitJavaSynchronizedStatementRaw(javaSynchronizedStatement: JKJavaSynchronizedStatement) =
        visitStatementRaw(javaSynchronizedStatement)

    override fun visitJavaAnnotationMethod(javaAnnotationMethod: JKJavaAnnotationMethod) {
        printLeftNonCodeElements(javaAnnotationMethod)
        visitJavaAnnotationMethodRaw(javaAnnotationMethod)
        printRightNonCodeElements(javaAnnotationMethod)
    }

    fun visitJavaAnnotationMethodRaw(javaAnnotationMethod: JKJavaAnnotationMethod) = visitMethodRaw(javaAnnotationMethod)

    override fun visitJavaStaticInitDeclaration(javaStaticInitDeclaration: JKJavaStaticInitDeclaration) {
        printLeftNonCodeElements(javaStaticInitDeclaration)
        visitJavaStaticInitDeclarationRaw(javaStaticInitDeclaration)
        printRightNonCodeElements(javaStaticInitDeclaration)
    }

    fun visitJavaStaticInitDeclarationRaw(javaStaticInitDeclaration: JKJavaStaticInitDeclaration) =
        visitDeclarationRaw(javaStaticInitDeclaration)

    override fun visitKtGetterOrSetter(ktGetterOrSetter: JKKtGetterOrSetter) {
        printLeftNonCodeElements(ktGetterOrSetter)
        visitKtGetterOrSetterRaw(ktGetterOrSetter)
        printRightNonCodeElements(ktGetterOrSetter)
    }

    fun visitKtGetterOrSetterRaw(ktGetterOrSetter: JKKtGetterOrSetter) = visitTreeElementRaw(ktGetterOrSetter)

    override fun visitKtEmptyGetterOrSetter(ktEmptyGetterOrSetter: JKKtEmptyGetterOrSetter) {
        printLeftNonCodeElements(ktEmptyGetterOrSetter)
        visitKtEmptyGetterOrSetterRaw(ktEmptyGetterOrSetter)
        printRightNonCodeElements(ktEmptyGetterOrSetter)
    }

    fun visitKtEmptyGetterOrSetterRaw(ktEmptyGetterOrSetter: JKKtEmptyGetterOrSetter) = visitKtGetterOrSetterRaw(ktEmptyGetterOrSetter)

    override fun visitKtProperty(ktProperty: JKKtProperty) {
        printLeftNonCodeElements(ktProperty)
        visitKtPropertyRaw(ktProperty)
        printRightNonCodeElements(ktProperty)
    }

    fun visitKtPropertyRaw(ktProperty: JKKtProperty) = visitFieldRaw(ktProperty)

    override fun visitKtFunction(ktFunction: JKKtFunction) {
        printLeftNonCodeElements(ktFunction)
        visitKtFunctionRaw(ktFunction)
        printRightNonCodeElements(ktFunction)
    }

    fun visitKtFunctionRaw(ktFunction: JKKtFunction) = visitMethodRaw(ktFunction)

    override fun visitKtConstructor(ktConstructor: JKKtConstructor) {
        printLeftNonCodeElements(ktConstructor)
        visitKtConstructorRaw(ktConstructor)
        printRightNonCodeElements(ktConstructor)
    }

    fun visitKtConstructorRaw(ktConstructor: JKKtConstructor) = visitMethodRaw(ktConstructor)

    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) {
        printLeftNonCodeElements(ktPrimaryConstructor)
        visitKtPrimaryConstructorRaw(ktPrimaryConstructor)
        printRightNonCodeElements(ktPrimaryConstructor)
    }

    fun visitKtPrimaryConstructorRaw(ktPrimaryConstructor: JKKtPrimaryConstructor) = visitKtConstructorRaw(ktPrimaryConstructor)

    override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement) {
        printLeftNonCodeElements(ktAssignmentStatement)
        visitKtAssignmentStatementRaw(ktAssignmentStatement)
        printRightNonCodeElements(ktAssignmentStatement)
    }

    fun visitKtAssignmentStatementRaw(ktAssignmentStatement: JKKtAssignmentStatement) = visitStatementRaw(ktAssignmentStatement)

    override fun visitKtCall(ktCall: JKKtCall) {
        printLeftNonCodeElements(ktCall)
        visitKtCallRaw(ktCall)
        printRightNonCodeElements(ktCall)
    }

    fun visitKtCallRaw(ktCall: JKKtCall) = visitMethodCallExpressionRaw(ktCall)

    override fun visitKtMethodCallExpression(ktMethodCallExpression: JKKtMethodCallExpression) {
        printLeftNonCodeElements(ktMethodCallExpression)
        visitKtMethodCallExpressionRaw(ktMethodCallExpression)
        printRightNonCodeElements(ktMethodCallExpression)
    }

    fun visitKtMethodCallExpressionRaw(ktMethodCallExpression: JKKtMethodCallExpression) =
        visitMethodCallExpressionRaw(ktMethodCallExpression)

    override fun visitKtAlsoCallExpression(ktAlsoCallExpression: JKKtAlsoCallExpression) {
        printLeftNonCodeElements(ktAlsoCallExpression)
        visitKtAlsoCallExpressionRaw(ktAlsoCallExpression)
        printRightNonCodeElements(ktAlsoCallExpression)
    }

    fun visitKtAlsoCallExpressionRaw(ktAlsoCallExpression: JKKtAlsoCallExpression) = visitKtMethodCallExpressionRaw(ktAlsoCallExpression)

    override fun visitKtLiteralExpression(ktLiteralExpression: JKKtLiteralExpression) {
        printLeftNonCodeElements(ktLiteralExpression)
        visitKtLiteralExpressionRaw(ktLiteralExpression)
        printRightNonCodeElements(ktLiteralExpression)
    }

    fun visitKtLiteralExpressionRaw(ktLiteralExpression: JKKtLiteralExpression) = visitLiteralExpressionRaw(ktLiteralExpression)

    override fun visitKtWhenStatement(ktWhenStatement: JKKtWhenStatement) {
        printLeftNonCodeElements(ktWhenStatement)
        visitKtWhenStatementRaw(ktWhenStatement)
        printRightNonCodeElements(ktWhenStatement)
    }

    fun visitKtWhenStatementRaw(ktWhenStatement: JKKtWhenStatement) = visitStatementRaw(ktWhenStatement)

    override fun visitKtWhenCase(ktWhenCase: JKKtWhenCase) {
        printLeftNonCodeElements(ktWhenCase)
        visitKtWhenCaseRaw(ktWhenCase)
        printRightNonCodeElements(ktWhenCase)
    }

    fun visitKtWhenCaseRaw(ktWhenCase: JKKtWhenCase) = visitTreeElementRaw(ktWhenCase)

    override fun visitKtWhenLabel(ktWhenLabel: JKKtWhenLabel) {
        printLeftNonCodeElements(ktWhenLabel)
        visitKtWhenLabelRaw(ktWhenLabel)
        printRightNonCodeElements(ktWhenLabel)
    }

    fun visitKtWhenLabelRaw(ktWhenLabel: JKKtWhenLabel) = visitTreeElementRaw(ktWhenLabel)

    override fun visitKtElseWhenLabel(ktElseWhenLabel: JKKtElseWhenLabel) {
        printLeftNonCodeElements(ktElseWhenLabel)
        visitKtElseWhenLabelRaw(ktElseWhenLabel)
        printRightNonCodeElements(ktElseWhenLabel)
    }

    fun visitKtElseWhenLabelRaw(ktElseWhenLabel: JKKtElseWhenLabel) = visitKtWhenLabelRaw(ktElseWhenLabel)

    override fun visitKtValueWhenLabel(ktValueWhenLabel: JKKtValueWhenLabel) {
        printLeftNonCodeElements(ktValueWhenLabel)
        visitKtValueWhenLabelRaw(ktValueWhenLabel)
        printRightNonCodeElements(ktValueWhenLabel)
    }

    fun visitKtValueWhenLabelRaw(ktValueWhenLabel: JKKtValueWhenLabel) = visitKtWhenLabelRaw(ktValueWhenLabel)

    override fun visitKtIsExpression(ktIsExpression: JKKtIsExpression) {
        printLeftNonCodeElements(ktIsExpression)
        visitKtIsExpressionRaw(ktIsExpression)
        printRightNonCodeElements(ktIsExpression)
    }

    fun visitKtIsExpressionRaw(ktIsExpression: JKKtIsExpression) = visitExpressionRaw(ktIsExpression)

    override fun visitKtInitDeclaration(ktInitDeclaration: JKKtInitDeclaration) {
        printLeftNonCodeElements(ktInitDeclaration)
        visitKtInitDeclarationRaw(ktInitDeclaration)
        printRightNonCodeElements(ktInitDeclaration)
    }

    fun visitKtInitDeclarationRaw(ktInitDeclaration: JKKtInitDeclaration) = visitDeclarationRaw(ktInitDeclaration)

    override fun visitKtConvertedFromForLoopSyntheticWhileStatement(ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement) {
        printLeftNonCodeElements(ktConvertedFromForLoopSyntheticWhileStatement)
        visitKtConvertedFromForLoopSyntheticWhileStatementRaw(ktConvertedFromForLoopSyntheticWhileStatement)
        printRightNonCodeElements(ktConvertedFromForLoopSyntheticWhileStatement)
    }

    fun visitKtConvertedFromForLoopSyntheticWhileStatementRaw(ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement) =
        visitStatementRaw(ktConvertedFromForLoopSyntheticWhileStatement)

    override fun visitKtThrowExpression(ktThrowExpression: JKKtThrowExpression) {
        printLeftNonCodeElements(ktThrowExpression)
        visitKtThrowExpressionRaw(ktThrowExpression)
        printRightNonCodeElements(ktThrowExpression)
    }

    fun visitKtThrowExpressionRaw(ktThrowExpression: JKKtThrowExpression) = visitExpressionRaw(ktThrowExpression)

    override fun visitKtTryExpression(ktTryExpression: JKKtTryExpression) {
        printLeftNonCodeElements(ktTryExpression)
        visitKtTryExpressionRaw(ktTryExpression)
        printRightNonCodeElements(ktTryExpression)
    }

    fun visitKtTryExpressionRaw(ktTryExpression: JKKtTryExpression) = visitExpressionRaw(ktTryExpression)

    override fun visitKtTryCatchSection(ktTryCatchSection: JKKtTryCatchSection) {
        printLeftNonCodeElements(ktTryCatchSection)
        visitKtTryCatchSectionRaw(ktTryCatchSection)
        printRightNonCodeElements(ktTryCatchSection)
    }

    fun visitKtTryCatchSectionRaw(ktTryCatchSection: JKKtTryCatchSection) = visitTreeElementRaw(ktTryCatchSection)

    override fun visitKtAnnotationArrayInitializerExpression(ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression) {
        printLeftNonCodeElements(ktAnnotationArrayInitializerExpression)
        visitKtAnnotationArrayInitializerExpressionRaw(ktAnnotationArrayInitializerExpression)
        printRightNonCodeElements(ktAnnotationArrayInitializerExpression)
    }

    fun visitKtAnnotationArrayInitializerExpressionRaw(ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression) =
        visitExpressionRaw(ktAnnotationArrayInitializerExpression)
}
