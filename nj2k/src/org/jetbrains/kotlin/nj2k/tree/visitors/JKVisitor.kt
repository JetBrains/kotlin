package org.jetbrains.kotlin.nj2k.tree.visitors

import org.jetbrains.kotlin.nj2k.tree.*

interface JKVisitor<out R, in D> {
    fun visitTreeElement(treeElement: JKTreeElement, data: D): R
    fun visitTreeRoot(treeRoot: JKTreeRoot, data: D): R = visitTreeElement(treeRoot, data)
    fun visitDeclaration(declaration: JKDeclaration, data: D): R = visitTreeElement(declaration, data)
    fun visitImportStatement(importStatement: JKImportStatement, data: D): R = visitTreeElement(importStatement, data)
    fun visitImportList(importList: JKImportList, data: D): R = visitTreeElement(importList, data)
    fun visitFile(file: JKFile, data: D): R = visitTreeElement(file, data)
    fun visitClass(klass: JKClass, data: D): R = visitDeclaration(klass, data)
    fun visitInheritanceInfo(inheritanceInfo: JKInheritanceInfo, data: D): R = visitTreeElement(inheritanceInfo, data)
    fun visitAnnotationList(annotationList: JKAnnotationList, data: D): R = visitTreeElement(annotationList, data)
    fun visitAnnotation(annotation: JKAnnotation, data: D): R = visitAnnotationMemberValue(annotation, data)
    fun visitAnnotationParameter(annotationParameter: JKAnnotationParameter, data: D): R = visitTreeElement(annotationParameter, data)
    fun visitAnnotationNameParameter(annotationNameParameter: JKAnnotationNameParameter, data: D): R =
        visitAnnotationParameter(annotationNameParameter, data)

    fun visitAnnotationListOwner(annotationListOwner: JKAnnotationListOwner, data: D): R = visitTreeElement(annotationListOwner, data)
    fun visitMethod(method: JKMethod, data: D): R = visitDeclaration(method, data)
    fun visitVariable(variable: JKVariable, data: D): R = visitDeclaration(variable, data)
    fun visitForLoopVariable(forLoopVariable: JKForLoopVariable, data: D): R = visitVariable(forLoopVariable, data)
    fun visitLocalVariable(localVariable: JKLocalVariable, data: D): R = visitVariable(localVariable, data)
    fun visitModifierElement(modifierElement: JKModifierElement, data: D): R = visitTreeElement(modifierElement, data)
    fun visitMutabilityModifierElement(mutabilityModifierElement: JKMutabilityModifierElement, data: D): R =
        visitModifierElement(mutabilityModifierElement, data)

    fun visitModalityModifierElement(modalityModifierElement: JKModalityModifierElement, data: D): R =
        visitModifierElement(modalityModifierElement, data)

    fun visitVisibilityModifierElement(visibilityModifierElement: JKVisibilityModifierElement, data: D): R =
        visitModifierElement(visibilityModifierElement, data)

    fun visitExtraModifierElement(otherModifierElement: JKOtherModifierElement, data: D): R =
        visitModifierElement(otherModifierElement, data)

    fun visitExtraModifiersOwner(otherModifiersOwner: JKOtherModifiersOwner, data: D): R =
        visitModifiersListOwner(otherModifiersOwner, data)

    fun visitVisibilityOwner(visibilityOwner: JKVisibilityOwner, data: D): R = visitModifiersListOwner(visibilityOwner, data)
    fun visitModalityOwner(modalityOwner: JKModalityOwner, data: D): R = visitModifiersListOwner(modalityOwner, data)
    fun visitMutabilityOwner(mutabilityOwner: JKMutabilityOwner, data: D): R = visitModifiersListOwner(mutabilityOwner, data)
    fun visitModifiersListOwner(modifiersListOwner: JKModifiersListOwner, data: D): R = visitTreeElement(modifiersListOwner, data)
    fun visitTypeElement(typeElement: JKTypeElement, data: D): R = visitTreeElement(typeElement, data)
    fun visitStatement(statement: JKStatement, data: D): R = visitTreeElement(statement, data)
    fun visitBlock(block: JKBlock, data: D): R = visitTreeElement(block, data)
    fun visitBodyStub(bodyStub: JKBodyStub, data: D): R = visitBlock(bodyStub, data)
    fun visitIdentifier(identifier: JKIdentifier, data: D): R = visitTreeElement(identifier, data)
    fun visitNameIdentifier(nameIdentifier: JKNameIdentifier, data: D): R = visitIdentifier(nameIdentifier, data)
    fun visitExpression(expression: JKExpression, data: D): R = visitTreeElement(expression, data)
    fun visitMethodReferenceExpression(methodReferenceExpression: JKMethodReferenceExpression, data: D): R =
        visitExpression(methodReferenceExpression, data)

    fun visitExpressionStatement(expressionStatement: JKExpressionStatement, data: D): R = visitStatement(expressionStatement, data)
    fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement, data: D): R = visitStatement(declarationStatement, data)
    fun visitOperatorExpression(operatorExpression: JKOperatorExpression, data: D): R = visitExpression(operatorExpression, data)
    fun visitBinaryExpression(binaryExpression: JKBinaryExpression, data: D): R = visitOperatorExpression(binaryExpression, data)
    fun visitUnaryExpression(unaryExpression: JKUnaryExpression, data: D): R = visitOperatorExpression(unaryExpression, data)
    fun visitPrefixExpression(prefixExpression: JKPrefixExpression, data: D): R = visitUnaryExpression(prefixExpression, data)
    fun visitPostfixExpression(postfixExpression: JKPostfixExpression, data: D): R = visitUnaryExpression(postfixExpression, data)
    fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression, data: D): R = visitExpression(qualifiedExpression, data)
    fun visitTypeArgumentList(typeArgumentList: JKTypeArgumentList, data: D): R = visitTreeElement(typeArgumentList, data)
    fun visitTypeArgumentListOwner(typeArgumentListOwner: JKTypeArgumentListOwner, data: D): R =
        visitTreeElement(typeArgumentListOwner, data)

    fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression, data: D): R = visitExpression(methodCallExpression, data)
    fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression, data: D): R =
        visitAssignableExpression(fieldAccessExpression, data)

    fun visitPackageAccessExpression(packageAccessExpression: JKPackageAccessExpression, data: D): R =
        visitAssignableExpression(packageAccessExpression, data)

    fun visitClassAccessExpression(classAccessExpression: JKClassAccessExpression, data: D): R =
        visitExpression(classAccessExpression, data)

    fun visitArrayAccessExpression(arrayAccessExpression: JKArrayAccessExpression, data: D): R =
        visitAssignableExpression(arrayAccessExpression, data)

    fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression, data: D): R =
        visitExpression(parenthesizedExpression, data)

    fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression, data: D): R = visitExpression(typeCastExpression, data)
    fun visitExpressionList(expressionList: JKExpressionList, data: D): R = visitTreeElement(expressionList, data)
    fun visitArgument(argument: JKArgument, data: D): R = visitTreeElement(argument, data)
    fun visitNamedArgument(namedArgument: JKNamedArgument, data: D): R = visitArgument(namedArgument, data)
    fun visitArgumentList(argumentList: JKArgumentList, data: D): R = visitTreeElement(argumentList, data)
    fun visitLiteralExpression(literalExpression: JKLiteralExpression, data: D): R = visitExpression(literalExpression, data)
    fun visitParameter(parameter: JKParameter, data: D): R = visitVariable(parameter, data)
    fun visitStringLiteralExpression(stringLiteralExpression: JKStringLiteralExpression, data: D): R =
        visitLiteralExpression(stringLiteralExpression, data)

    fun visitStubExpression(stubExpression: JKStubExpression, data: D): R = visitExpression(stubExpression, data)
    fun visitLoopStatement(loopStatement: JKLoopStatement, data: D): R = visitStatement(loopStatement, data)
    fun visitBlockStatement(blockStatement: JKBlockStatement, data: D): R = visitStatement(blockStatement, data)
    fun visitBlockStatementWithoutBrackets(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets, data: D): R =
        visitStatement(blockStatementWithoutBrackets, data)

    fun visitThisExpression(thisExpression: JKThisExpression, data: D): R = visitExpression(thisExpression, data)
    fun visitSuperExpression(superExpression: JKSuperExpression, data: D): R = visitExpression(superExpression, data)
    fun visitWhileStatement(whileStatement: JKWhileStatement, data: D): R = visitLoopStatement(whileStatement, data)
    fun visitDoWhileStatement(doWhileStatement: JKDoWhileStatement, data: D): R = visitLoopStatement(doWhileStatement, data)
    fun visitBreakStatement(breakStatement: JKBreakStatement, data: D): R = visitStatement(breakStatement, data)
    fun visitBreakWithLabelStatement(breakWithLabelStatement: JKBreakWithLabelStatement, data: D): R =
        visitBreakStatement(breakWithLabelStatement, data)

    fun visitIfStatement(ifStatement: JKIfStatement, data: D): R = visitStatement(ifStatement, data)
    fun visitIfElseStatement(ifElseStatement: JKIfElseStatement, data: D): R = visitIfStatement(ifElseStatement, data)
    fun visitIfElseExpression(ifElseExpression: JKIfElseExpression, data: D): R = visitExpression(ifElseExpression, data)
    fun visitAssignableExpression(assignableExpression: JKAssignableExpression, data: D): R = visitExpression(assignableExpression, data)
    fun visitLambdaExpression(lambdaExpression: JKLambdaExpression, data: D): R = visitExpression(lambdaExpression, data)
    fun visitDelegationConstructorCall(delegationConstructorCall: JKDelegationConstructorCall, data: D): R =
        visitMethodCallExpression(delegationConstructorCall, data)

    fun visitLabel(label: JKLabel, data: D): R = visitTreeElement(label, data)
    fun visitLabelEmpty(labelEmpty: JKLabelEmpty, data: D): R = visitLabel(labelEmpty, data)
    fun visitLabelText(labelText: JKLabelText, data: D): R = visitLabel(labelText, data)
    fun visitContinueStatement(continueStatement: JKContinueStatement, data: D): R = visitStatement(continueStatement, data)
    fun visitLabeledStatement(labeledStatement: JKLabeledStatement, data: D): R = visitExpression(labeledStatement, data)
    fun visitEmptyStatement(emptyStatement: JKEmptyStatement, data: D): R = visitStatement(emptyStatement, data)
    fun visitTypeParameterList(typeParameterList: JKTypeParameterList, data: D): R = visitTreeElement(typeParameterList, data)
    fun visitTypeParameter(typeParameter: JKTypeParameter, data: D): R = visitTreeElement(typeParameter, data)
    fun visitTypeParameterListOwner(typeParameterListOwner: JKTypeParameterListOwner, data: D): R =
        visitTreeElement(typeParameterListOwner, data)

    fun visitEnumConstant(enumConstant: JKEnumConstant, data: D): R = visitVariable(enumConstant, data)
    fun visitForInStatement(forInStatement: JKForInStatement, data: D): R = visitStatement(forInStatement, data)
    fun visitPackageDeclaration(packageDeclaration: JKPackageDeclaration, data: D): R = visitDeclaration(packageDeclaration, data)
    fun visitClassLiteralExpression(classLiteralExpression: JKClassLiteralExpression, data: D): R =
        visitExpression(classLiteralExpression, data)

    fun visitAnnotationMemberValue(annotationMemberValue: JKAnnotationMemberValue, data: D): R =
        visitTreeElement(annotationMemberValue, data)

    fun visitField(field: JKField, data: D): R = visitVariable(field, data)
    fun visitJavaField(javaField: JKJavaField, data: D): R = visitField(javaField, data)
    fun visitJavaMethod(javaMethod: JKJavaMethod, data: D): R = visitMethod(javaMethod, data)
    fun visitJavaMethodCallExpression(javaMethodCallExpression: JKJavaMethodCallExpression, data: D): R =
        visitMethodCallExpression(javaMethodCallExpression, data)

    fun visitClassBody(classBody: JKClassBody, data: D): R = visitTreeElement(classBody, data)
    fun visitEmptyClassBody(emptyClassBody: JKEmptyClassBody, data: D): R = visitClassBody(emptyClassBody, data)
    fun visitJavaNewExpression(javaNewExpression: JKJavaNewExpression, data: D): R = visitExpression(javaNewExpression, data)
    fun visitJavaDefaultNewExpression(javaDefaultNewExpression: JKJavaDefaultNewExpression, data: D): R =
        visitExpression(javaDefaultNewExpression, data)

    fun visitJavaNewEmptyArray(javaNewEmptyArray: JKJavaNewEmptyArray, data: D): R = visitExpression(javaNewEmptyArray, data)
    fun visitJavaNewArray(javaNewArray: JKJavaNewArray, data: D): R = visitExpression(javaNewArray, data)
    fun visitJavaLiteralExpression(javaLiteralExpression: JKJavaLiteralExpression, data: D): R =
        visitLiteralExpression(javaLiteralExpression, data)

    fun visitReturnStatement(returnStatement: JKReturnStatement, data: D): R = visitStatement(returnStatement, data)
    fun visitJavaAssertStatement(javaAssertStatement: JKJavaAssertStatement, data: D): R = visitStatement(javaAssertStatement, data)
    fun visitJavaForLoopStatement(javaForLoopStatement: JKJavaForLoopStatement, data: D): R = visitLoopStatement(javaForLoopStatement, data)
    fun visitJavaPolyadicExpression(javaPolyadicExpression: JKJavaPolyadicExpression, data: D): R =
        visitExpression(javaPolyadicExpression, data)

    fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: D): R =
        visitExpression(javaAssignmentExpression, data)

    fun visitJavaThrowStatement(javaThrowStatement: JKJavaThrowStatement, data: D): R = visitStatement(javaThrowStatement, data)
    fun visitJavaTryStatement(javaTryStatement: JKJavaTryStatement, data: D): R = visitStatement(javaTryStatement, data)
    fun visitJavaTryCatchSection(javaTryCatchSection: JKJavaTryCatchSection, data: D): R = visitTreeElement(javaTryCatchSection, data)
    fun visitJavaSwitchStatement(javaSwitchStatement: JKJavaSwitchStatement, data: D): R = visitStatement(javaSwitchStatement, data)
    fun visitJavaSwitchCase(javaSwitchCase: JKJavaSwitchCase, data: D): R = visitTreeElement(javaSwitchCase, data)
    fun visitJavaDefaultSwitchCase(javaDefaultSwitchCase: JKJavaDefaultSwitchCase, data: D): R =
        visitJavaSwitchCase(javaDefaultSwitchCase, data)

    fun visitJavaLabelSwitchCase(javaLabelSwitchCase: JKJavaLabelSwitchCase, data: D): R = visitJavaSwitchCase(javaLabelSwitchCase, data)
    fun visitJavaContinueStatement(javaContinueStatement: JKJavaContinueStatement, data: D): R = visitStatement(javaContinueStatement, data)
    fun visitJavaSynchronizedStatement(javaSynchronizedStatement: JKJavaSynchronizedStatement, data: D): R =
        visitStatement(javaSynchronizedStatement, data)

    fun visitJavaAnnotationMethod(javaAnnotationMethod: JKJavaAnnotationMethod, data: D): R = visitMethod(javaAnnotationMethod, data)
    fun visitJavaStaticInitDeclaration(javaStaticInitDeclaration: JKJavaStaticInitDeclaration, data: D): R =
        visitDeclaration(javaStaticInitDeclaration, data)

    fun visitKtGetterOrSetter(ktGetterOrSetter: JKKtGetterOrSetter, data: D): R = visitTreeElement(ktGetterOrSetter, data)
    fun visitKtEmptyGetterOrSetter(ktEmptyGetterOrSetter: JKKtEmptyGetterOrSetter, data: D): R =
        visitKtGetterOrSetter(ktEmptyGetterOrSetter, data)

    fun visitKtProperty(ktProperty: JKKtProperty, data: D): R = visitField(ktProperty, data)
    fun visitKtFunction(ktFunction: JKKtFunction, data: D): R = visitMethod(ktFunction, data)
    fun visitKtConstructor(ktConstructor: JKKtConstructor, data: D): R = visitMethod(ktConstructor, data)
    fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: D): R = visitKtConstructor(ktPrimaryConstructor, data)
    fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: D): R = visitStatement(ktAssignmentStatement, data)
    fun visitKtCall(ktCall: JKKtCall, data: D): R = visitMethodCallExpression(ktCall, data)
    fun visitKtMethodCallExpression(ktMethodCallExpression: JKKtMethodCallExpression, data: D): R =
        visitMethodCallExpression(ktMethodCallExpression, data)

    fun visitKtAlsoCallExpression(ktAlsoCallExpression: JKKtAlsoCallExpression, data: D): R =
        visitKtMethodCallExpression(ktAlsoCallExpression, data)

    fun visitKtLiteralExpression(ktLiteralExpression: JKKtLiteralExpression, data: D): R = visitLiteralExpression(ktLiteralExpression, data)
    fun visitKtWhenStatement(ktWhenStatement: JKKtWhenStatement, data: D): R = visitStatement(ktWhenStatement, data)
    fun visitKtWhenCase(ktWhenCase: JKKtWhenCase, data: D): R = visitTreeElement(ktWhenCase, data)
    fun visitKtWhenLabel(ktWhenLabel: JKKtWhenLabel, data: D): R = visitTreeElement(ktWhenLabel, data)
    fun visitKtElseWhenLabel(ktElseWhenLabel: JKKtElseWhenLabel, data: D): R = visitKtWhenLabel(ktElseWhenLabel, data)
    fun visitKtValueWhenLabel(ktValueWhenLabel: JKKtValueWhenLabel, data: D): R = visitKtWhenLabel(ktValueWhenLabel, data)
    fun visitKtIsExpression(ktIsExpression: JKKtIsExpression, data: D): R = visitExpression(ktIsExpression, data)
    fun visitKtInitDeclaration(ktInitDeclaration: JKKtInitDeclaration, data: D): R = visitDeclaration(ktInitDeclaration, data)
    fun visitKtConvertedFromForLoopSyntheticWhileStatement(
        ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement,
        data: D
    ): R = visitStatement(ktConvertedFromForLoopSyntheticWhileStatement, data)

    fun visitKtThrowExpression(ktThrowExpression: JKKtThrowExpression, data: D): R = visitExpression(ktThrowExpression, data)
    fun visitKtTryExpression(ktTryExpression: JKKtTryExpression, data: D): R = visitExpression(ktTryExpression, data)
    fun visitKtTryCatchSection(ktTryCatchSection: JKKtTryCatchSection, data: D): R = visitTreeElement(ktTryCatchSection, data)
    fun visitKtAnnotationArrayInitializerExpression(
        ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression,
        data: D
    ): R = visitExpression(ktAnnotationArrayInitializerExpression, data)
}
