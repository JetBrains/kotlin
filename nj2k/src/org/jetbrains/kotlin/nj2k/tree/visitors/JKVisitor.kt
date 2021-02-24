package org.jetbrains.kotlin.nj2k.tree.visitors

import org.jetbrains.kotlin.nj2k.tree.*

abstract class JKVisitor {
    abstract fun visitTreeElement(treeElement: JKTreeElement)
    open fun visitDeclaration(declaration: JKDeclaration) = visitTreeElement(declaration)
    open fun visitClass(klass: JKClass) = visitDeclaration(klass)
    open fun visitVariable(variable: JKVariable) = visitDeclaration(variable)
    open fun visitLocalVariable(localVariable: JKLocalVariable) = visitVariable(localVariable)
    open fun visitForLoopVariable(forLoopVariable: JKForLoopVariable) = visitVariable(forLoopVariable)
    open fun visitParameter(parameter: JKParameter) = visitVariable(parameter)
    open fun visitEnumConstant(enumConstant: JKEnumConstant) = visitVariable(enumConstant)
    open fun visitTypeParameter(typeParameter: JKTypeParameter) = visitDeclaration(typeParameter)
    open fun visitMethod(method: JKMethod) = visitDeclaration(method)
    open fun visitMethodImpl(methodImpl: JKMethodImpl) = visitMethod(methodImpl)
    open fun visitConstructor(constructor: JKConstructor) = visitMethod(constructor)
    open fun visitConstructorImpl(constructorImpl: JKConstructorImpl) = visitConstructor(constructorImpl)
    open fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) = visitConstructor(ktPrimaryConstructor)
    open fun visitField(field: JKField) = visitVariable(field)
    open fun visitKtInitDeclaration(ktInitDeclaration: JKKtInitDeclaration) = visitDeclaration(ktInitDeclaration)
    open fun visitJavaStaticInitDeclaration(javaStaticInitDeclaration: JKJavaStaticInitDeclaration) =
        visitDeclaration(javaStaticInitDeclaration)

    open fun visitTreeRoot(treeRoot: JKTreeRoot) = visitTreeElement(treeRoot)
    open fun visitFile(file: JKFile) = visitTreeElement(file)
    open fun visitTypeElement(typeElement: JKTypeElement) = visitTreeElement(typeElement)
    open fun visitBlock(block: JKBlock) = visitTreeElement(block)
    open fun visitInheritanceInfo(inheritanceInfo: JKInheritanceInfo) = visitTreeElement(inheritanceInfo)
    open fun visitPackageDeclaration(packageDeclaration: JKPackageDeclaration) = visitTreeElement(packageDeclaration)
    open fun visitLabel(label: JKLabel) = visitTreeElement(label)
    open fun visitLabelEmpty(labelEmpty: JKLabelEmpty) = visitLabel(labelEmpty)
    open fun visitLabelText(labelText: JKLabelText) = visitLabel(labelText)
    open fun visitImportStatement(importStatement: JKImportStatement) = visitTreeElement(importStatement)
    open fun visitImportList(importList: JKImportList) = visitTreeElement(importList)
    open fun visitAnnotationParameter(annotationParameter: JKAnnotationParameter) = visitTreeElement(annotationParameter)
    open fun visitAnnotationParameterImpl(annotationParameterImpl: JKAnnotationParameterImpl) =
        visitAnnotationParameter(annotationParameterImpl)

    open fun visitAnnotationNameParameter(annotationNameParameter: JKAnnotationNameParameter) =
        visitAnnotationParameter(annotationNameParameter)

    open fun visitArgument(argument: JKArgument) = visitTreeElement(argument)
    open fun visitNamedArgument(namedArgument: JKNamedArgument) = visitArgument(namedArgument)
    open fun visitArgumentImpl(argumentImpl: JKArgumentImpl) = visitArgument(argumentImpl)
    open fun visitArgumentList(argumentList: JKArgumentList) = visitTreeElement(argumentList)
    open fun visitTypeParameterList(typeParameterList: JKTypeParameterList) = visitTreeElement(typeParameterList)
    open fun visitAnnotationList(annotationList: JKAnnotationList) = visitTreeElement(annotationList)
    open fun visitAnnotation(annotation: JKAnnotation) = visitTreeElement(annotation)
    open fun visitTypeArgumentList(typeArgumentList: JKTypeArgumentList) = visitTreeElement(typeArgumentList)
    open fun visitNameIdentifier(nameIdentifier: JKNameIdentifier) = visitTreeElement(nameIdentifier)
    open fun visitBlockImpl(blockImpl: JKBlockImpl) = visitBlock(blockImpl)
    open fun visitKtWhenCase(ktWhenCase: JKKtWhenCase) = visitTreeElement(ktWhenCase)
    open fun visitKtWhenLabel(ktWhenLabel: JKKtWhenLabel) = visitTreeElement(ktWhenLabel)
    open fun visitKtElseWhenLabel(ktElseWhenLabel: JKKtElseWhenLabel) = visitKtWhenLabel(ktElseWhenLabel)
    open fun visitKtValueWhenLabel(ktValueWhenLabel: JKKtValueWhenLabel) = visitKtWhenLabel(ktValueWhenLabel)
    open fun visitClassBody(classBody: JKClassBody) = visitTreeElement(classBody)
    open fun visitJavaTryCatchSection(javaTryCatchSection: JKJavaTryCatchSection) = visitStatement(javaTryCatchSection)
    open fun visitJavaSwitchCase(javaSwitchCase: JKJavaSwitchCase) = visitTreeElement(javaSwitchCase)
    open fun visitJavaDefaultSwitchCase(javaDefaultSwitchCase: JKJavaDefaultSwitchCase) = visitJavaSwitchCase(javaDefaultSwitchCase)
    open fun visitJavaLabelSwitchCase(javaLabelSwitchCase: JKJavaLabelSwitchCase) = visitJavaSwitchCase(javaLabelSwitchCase)
    open fun visitExpression(expression: JKExpression) = visitTreeElement(expression)
    open fun visitOperatorExpression(operatorExpression: JKOperatorExpression) = visitExpression(operatorExpression)
    open fun visitBinaryExpression(binaryExpression: JKBinaryExpression) = visitOperatorExpression(binaryExpression)
    open fun visitUnaryExpression(unaryExpression: JKUnaryExpression) = visitOperatorExpression(unaryExpression)
    open fun visitPrefixExpression(prefixExpression: JKPrefixExpression) = visitUnaryExpression(prefixExpression)
    open fun visitPostfixExpression(postfixExpression: JKPostfixExpression) = visitUnaryExpression(postfixExpression)
    open fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression) = visitExpression(qualifiedExpression)
    open fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression) = visitExpression(parenthesizedExpression)
    open fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression) = visitExpression(typeCastExpression)
    open fun visitLiteralExpression(literalExpression: JKLiteralExpression) = visitExpression(literalExpression)
    open fun visitStubExpression(stubExpression: JKStubExpression) = visitExpression(stubExpression)
    open fun visitThisExpression(thisExpression: JKThisExpression) = visitExpression(thisExpression)
    open fun visitSuperExpression(superExpression: JKSuperExpression) = visitExpression(superExpression)
    open fun visitIfElseExpression(ifElseExpression: JKIfElseExpression) = visitExpression(ifElseExpression)
    open fun visitLambdaExpression(lambdaExpression: JKLambdaExpression) = visitExpression(lambdaExpression)
    open fun visitCallExpression(callExpression: JKCallExpression) = visitExpression(callExpression)
    open fun visitDelegationConstructorCall(delegationConstructorCall: JKDelegationConstructorCall) =
        visitCallExpression(delegationConstructorCall)

    open fun visitCallExpressionImpl(callExpressionImpl: JKCallExpressionImpl) = visitCallExpression(callExpressionImpl)
    open fun visitNewExpression(newExpression: JKNewExpression) = visitExpression(newExpression)
    open fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression) = visitExpression(fieldAccessExpression)
    open fun visitPackageAccessExpression(packageAccessExpression: JKPackageAccessExpression) = visitExpression(packageAccessExpression)
    open fun visitClassAccessExpression(classAccessExpression: JKClassAccessExpression) = visitExpression(classAccessExpression)
    open fun visitMethodAccessExpression(methodAccessExpression: JKMethodAccessExpression) = visitExpression(methodAccessExpression)
    open fun visitTypeQualifierExpression(typeQualifierExpression: JKTypeQualifierExpression) = visitExpression(typeQualifierExpression)
    open fun visitMethodReferenceExpression(methodReferenceExpression: JKMethodReferenceExpression) =
        visitExpression(methodReferenceExpression)

    open fun visitLabeledExpression(labeledExpression: JKLabeledExpression) = visitExpression(labeledExpression)
    open fun visitClassLiteralExpression(classLiteralExpression: JKClassLiteralExpression) = visitExpression(classLiteralExpression)
    open fun visitKtAssignmentChainLink(ktAssignmentChainLink: JKKtAssignmentChainLink) = visitExpression(ktAssignmentChainLink)
    open fun visitAssignmentChainAlsoLink(assignmentChainAlsoLink: JKAssignmentChainAlsoLink) =
        visitKtAssignmentChainLink(assignmentChainAlsoLink)

    open fun visitAssignmentChainLetLink(assignmentChainLetLink: JKAssignmentChainLetLink) =
        visitKtAssignmentChainLink(assignmentChainLetLink)

    open fun visitIsExpression(isExpression: JKIsExpression) = visitExpression(isExpression)
    open fun visitKtThrowExpression(ktThrowExpression: JKKtThrowExpression) = visitExpression(ktThrowExpression)
    open fun visitKtItExpression(ktItExpression: JKKtItExpression) = visitExpression(ktItExpression)
    open fun visitKtAnnotationArrayInitializerExpression(ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression) =
        visitExpression(ktAnnotationArrayInitializerExpression)

    open fun visitKtTryExpression(ktTryExpression: JKKtTryExpression) = visitExpression(ktTryExpression)
    open fun visitKtTryCatchSection(ktTryCatchSection: JKKtTryCatchSection) = visitTreeElement(ktTryCatchSection)
    open fun visitJavaNewEmptyArray(javaNewEmptyArray: JKJavaNewEmptyArray) = visitExpression(javaNewEmptyArray)
    open fun visitJavaNewArray(javaNewArray: JKJavaNewArray) = visitExpression(javaNewArray)
    open fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression) = visitExpression(javaAssignmentExpression)
    open fun visitModifierElement(modifierElement: JKModifierElement) = visitTreeElement(modifierElement)
    open fun visitMutabilityModifierElement(mutabilityModifierElement: JKMutabilityModifierElement) =
        visitModifierElement(mutabilityModifierElement)

    open fun visitModalityModifierElement(modalityModifierElement: JKModalityModifierElement) =
        visitModifierElement(modalityModifierElement)

    open fun visitVisibilityModifierElement(visibilityModifierElement: JKVisibilityModifierElement) =
        visitModifierElement(visibilityModifierElement)

    open fun visitOtherModifierElement(otherModifierElement: JKOtherModifierElement) = visitModifierElement(otherModifierElement)
    open fun visitStatement(statement: JKStatement) = visitTreeElement(statement)
    open fun visitEmptyStatement(emptyStatement: JKEmptyStatement) = visitStatement(emptyStatement)
    open fun visitLoopStatement(loopStatement: JKLoopStatement) = visitStatement(loopStatement)
    open fun visitWhileStatement(whileStatement: JKWhileStatement) = visitLoopStatement(whileStatement)
    open fun visitDoWhileStatement(doWhileStatement: JKDoWhileStatement) = visitLoopStatement(doWhileStatement)
    open fun visitForInStatement(forInStatement: JKForInStatement) = visitStatement(forInStatement)
    open fun visitIfElseStatement(ifElseStatement: JKIfElseStatement) = visitStatement(ifElseStatement)
    open fun visitBreakStatement(breakStatement: JKBreakStatement) = visitStatement(breakStatement)
    open fun visitContinueStatement(continueStatement: JKContinueStatement) = visitStatement(continueStatement)
    open fun visitBlockStatement(blockStatement: JKBlockStatement) = visitStatement(blockStatement)
    open fun visitBlockStatementWithoutBrackets(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets) =
        visitStatement(blockStatementWithoutBrackets)

    open fun visitExpressionStatement(expressionStatement: JKExpressionStatement) = visitStatement(expressionStatement)
    open fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement) = visitStatement(declarationStatement)
    open fun visitKtWhenStatement(ktWhenStatement: JKKtWhenStatement) = visitStatement(ktWhenStatement)
    open fun visitKtConvertedFromForLoopSyntheticWhileStatement(ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement) =
        visitStatement(ktConvertedFromForLoopSyntheticWhileStatement)

    open fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement) = visitStatement(ktAssignmentStatement)
    open fun visitReturnStatement(returnStatement: JKReturnStatement) = visitStatement(returnStatement)
    open fun visitJavaSwitchStatement(javaSwitchStatement: JKJavaSwitchStatement) = visitStatement(javaSwitchStatement)
    open fun visitJavaThrowStatement(javaThrowStatement: JKJavaThrowStatement) = visitStatement(javaThrowStatement)
    open fun visitJavaTryStatement(javaTryStatement: JKJavaTryStatement) = visitStatement(javaTryStatement)
    open fun visitJavaSynchronizedStatement(javaSynchronizedStatement: JKJavaSynchronizedStatement) = visitStatement(javaSynchronizedStatement)
    open fun visitJavaAssertStatement(javaAssertStatement: JKJavaAssertStatement) = visitStatement(javaAssertStatement)
    open fun visitJavaForLoopStatement(javaForLoopStatement: JKJavaForLoopStatement) = visitLoopStatement(javaForLoopStatement)
    open fun visitJavaAnnotationMethod(javaAnnotationMethod: JKJavaAnnotationMethod) = visitMethod(javaAnnotationMethod)
}
