package org.jetbrains.kotlin.nj2k.tree.visitors

import org.jetbrains.kotlin.nj2k.tree.*

abstract class JKVisitorWithCommentsPrinting : JKVisitor() {
    abstract fun printLeftNonCodeElements(element: JKFormattingOwner)
    abstract fun printRightNonCodeElements(element: JKFormattingOwner)

    override fun visitTreeElement(treeElement: JKTreeElement) {
        printLeftNonCodeElements(treeElement)
        visitTreeElementRaw(treeElement)
        printRightNonCodeElements(treeElement)
    }

    abstract fun visitTreeElementRaw(treeElement: JKTreeElement)

    override fun visitDeclaration(declaration: JKDeclaration) {
        printLeftNonCodeElements(declaration)
        visitDeclarationRaw(declaration)
        printRightNonCodeElements(declaration)
    }

    open fun visitDeclarationRaw(declaration: JKDeclaration) = visitTreeElementRaw(declaration)

    override fun visitClass(klass: JKClass) {
        printLeftNonCodeElements(klass)
        visitClassRaw(klass)
        printRightNonCodeElements(klass)
    }

    open fun visitClassRaw(klass: JKClass) = visitDeclarationRaw(klass)

    override fun visitVariable(variable: JKVariable) {
        printLeftNonCodeElements(variable)
        visitVariableRaw(variable)
        printRightNonCodeElements(variable)
    }

    open fun visitVariableRaw(variable: JKVariable) = visitDeclarationRaw(variable)

    override fun visitLocalVariable(localVariable: JKLocalVariable) {
        printLeftNonCodeElements(localVariable)
        visitLocalVariableRaw(localVariable)
        printRightNonCodeElements(localVariable)
    }

    open fun visitLocalVariableRaw(localVariable: JKLocalVariable) = visitVariableRaw(localVariable)

    override fun visitForLoopVariable(forLoopVariable: JKForLoopVariable) {
        printLeftNonCodeElements(forLoopVariable)
        visitForLoopVariableRaw(forLoopVariable)
        printRightNonCodeElements(forLoopVariable)
    }

    open fun visitForLoopVariableRaw(forLoopVariable: JKForLoopVariable) = visitVariableRaw(forLoopVariable)

    override fun visitParameter(parameter: JKParameter) {
        printLeftNonCodeElements(parameter)
        visitParameterRaw(parameter)
        printRightNonCodeElements(parameter)
    }

    open fun visitParameterRaw(parameter: JKParameter) = visitVariableRaw(parameter)

    override fun visitEnumConstant(enumConstant: JKEnumConstant) {
        printLeftNonCodeElements(enumConstant)
        visitEnumConstantRaw(enumConstant)
        printRightNonCodeElements(enumConstant)
    }

    open fun visitEnumConstantRaw(enumConstant: JKEnumConstant) = visitVariableRaw(enumConstant)

    override fun visitTypeParameter(typeParameter: JKTypeParameter) {
        printLeftNonCodeElements(typeParameter)
        visitTypeParameterRaw(typeParameter)
        printRightNonCodeElements(typeParameter)
    }

    open fun visitTypeParameterRaw(typeParameter: JKTypeParameter) = visitDeclarationRaw(typeParameter)

    override fun visitMethod(method: JKMethod) {
        printLeftNonCodeElements(method)
        visitMethodRaw(method)
        printRightNonCodeElements(method)
    }

    open fun visitMethodRaw(method: JKMethod) = visitDeclarationRaw(method)

    override fun visitMethodImpl(methodImpl: JKMethodImpl) {
        printLeftNonCodeElements(methodImpl)
        visitMethodImplRaw(methodImpl)
        printRightNonCodeElements(methodImpl)
    }

    open fun visitMethodImplRaw(methodImpl: JKMethodImpl) = visitMethodRaw(methodImpl)

    override fun visitConstructor(constructor: JKConstructor) {
        printLeftNonCodeElements(constructor)
        visitConstructorRaw(constructor)
        printRightNonCodeElements(constructor)
    }

    open fun visitConstructorRaw(constructor: JKConstructor) = visitMethodRaw(constructor)

    override fun visitConstructorImpl(constructorImpl: JKConstructorImpl) {
        printLeftNonCodeElements(constructorImpl)
        visitConstructorImplRaw(constructorImpl)
        printRightNonCodeElements(constructorImpl)
    }

    open fun visitConstructorImplRaw(constructorImpl: JKConstructorImpl) = visitConstructorRaw(constructorImpl)

    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) {
        printLeftNonCodeElements(ktPrimaryConstructor)
        visitKtPrimaryConstructorRaw(ktPrimaryConstructor)
        printRightNonCodeElements(ktPrimaryConstructor)
    }

    open fun visitKtPrimaryConstructorRaw(ktPrimaryConstructor: JKKtPrimaryConstructor) = visitConstructorRaw(ktPrimaryConstructor)

    override fun visitField(field: JKField) {
        printLeftNonCodeElements(field)
        visitFieldRaw(field)
        printRightNonCodeElements(field)
    }

    open fun visitFieldRaw(field: JKField) = visitVariableRaw(field)

    override fun visitKtInitDeclaration(ktInitDeclaration: JKKtInitDeclaration) {
        printLeftNonCodeElements(ktInitDeclaration)
        visitKtInitDeclarationRaw(ktInitDeclaration)
        printRightNonCodeElements(ktInitDeclaration)
    }

    open fun visitKtInitDeclarationRaw(ktInitDeclaration: JKKtInitDeclaration) = visitDeclarationRaw(ktInitDeclaration)

    override fun visitJavaStaticInitDeclaration(javaStaticInitDeclaration: JKJavaStaticInitDeclaration) {
        printLeftNonCodeElements(javaStaticInitDeclaration)
        visitJavaStaticInitDeclarationRaw(javaStaticInitDeclaration)
        printRightNonCodeElements(javaStaticInitDeclaration)
    }

    open fun visitJavaStaticInitDeclarationRaw(javaStaticInitDeclaration: JKJavaStaticInitDeclaration) =
        visitDeclarationRaw(javaStaticInitDeclaration)

    override fun visitTreeRoot(treeRoot: JKTreeRoot) {
        printLeftNonCodeElements(treeRoot)
        visitTreeRootRaw(treeRoot)
        printRightNonCodeElements(treeRoot)
    }

    open fun visitTreeRootRaw(treeRoot: JKTreeRoot) = visitTreeElementRaw(treeRoot)

    override fun visitFile(file: JKFile) {
        printLeftNonCodeElements(file)
        visitFileRaw(file)
        printRightNonCodeElements(file)
    }

    open fun visitFileRaw(file: JKFile) = visitTreeElementRaw(file)

    override fun visitTypeElement(typeElement: JKTypeElement) {
        printLeftNonCodeElements(typeElement)
        visitTypeElementRaw(typeElement)
        printRightNonCodeElements(typeElement)
    }

    open fun visitTypeElementRaw(typeElement: JKTypeElement) = visitTreeElementRaw(typeElement)

    override fun visitBlock(block: JKBlock) {
        printLeftNonCodeElements(block)
        visitBlockRaw(block)
        printRightNonCodeElements(block)
    }

    open fun visitBlockRaw(block: JKBlock) = visitTreeElementRaw(block)

    override fun visitInheritanceInfo(inheritanceInfo: JKInheritanceInfo) {
        printLeftNonCodeElements(inheritanceInfo)
        visitInheritanceInfoRaw(inheritanceInfo)
        printRightNonCodeElements(inheritanceInfo)
    }

    open fun visitInheritanceInfoRaw(inheritanceInfo: JKInheritanceInfo) = visitTreeElementRaw(inheritanceInfo)

    override fun visitPackageDeclaration(packageDeclaration: JKPackageDeclaration) {
        printLeftNonCodeElements(packageDeclaration)
        visitPackageDeclarationRaw(packageDeclaration)
        printRightNonCodeElements(packageDeclaration)
    }

    open fun visitPackageDeclarationRaw(packageDeclaration: JKPackageDeclaration) = visitTreeElementRaw(packageDeclaration)

    override fun visitLabel(label: JKLabel) {
        printLeftNonCodeElements(label)
        visitLabelRaw(label)
        printRightNonCodeElements(label)
    }

    open fun visitLabelRaw(label: JKLabel) = visitTreeElementRaw(label)

    override fun visitLabelEmpty(labelEmpty: JKLabelEmpty) {
        printLeftNonCodeElements(labelEmpty)
        visitLabelEmptyRaw(labelEmpty)
        printRightNonCodeElements(labelEmpty)
    }

    open fun visitLabelEmptyRaw(labelEmpty: JKLabelEmpty) = visitLabelRaw(labelEmpty)

    override fun visitLabelText(labelText: JKLabelText) {
        printLeftNonCodeElements(labelText)
        visitLabelTextRaw(labelText)
        printRightNonCodeElements(labelText)
    }

    open fun visitLabelTextRaw(labelText: JKLabelText) = visitLabelRaw(labelText)

    override fun visitImportStatement(importStatement: JKImportStatement) {
        printLeftNonCodeElements(importStatement)
        visitImportStatementRaw(importStatement)
        printRightNonCodeElements(importStatement)
    }

    open fun visitImportStatementRaw(importStatement: JKImportStatement) = visitTreeElementRaw(importStatement)

    override fun visitImportList(importList: JKImportList) {
        printLeftNonCodeElements(importList)
        visitImportListRaw(importList)
        printRightNonCodeElements(importList)
    }

    open fun visitImportListRaw(importList: JKImportList) = visitTreeElementRaw(importList)

    override fun visitAnnotationParameter(annotationParameter: JKAnnotationParameter) {
        printLeftNonCodeElements(annotationParameter)
        visitAnnotationParameterRaw(annotationParameter)
        printRightNonCodeElements(annotationParameter)
    }

    open fun visitAnnotationParameterRaw(annotationParameter: JKAnnotationParameter) = visitTreeElementRaw(annotationParameter)

    override fun visitAnnotationParameterImpl(annotationParameterImpl: JKAnnotationParameterImpl) {
        printLeftNonCodeElements(annotationParameterImpl)
        visitAnnotationParameterImplRaw(annotationParameterImpl)
        printRightNonCodeElements(annotationParameterImpl)
    }

    open fun visitAnnotationParameterImplRaw(annotationParameterImpl: JKAnnotationParameterImpl) =
        visitAnnotationParameterRaw(annotationParameterImpl)

    override fun visitAnnotationNameParameter(annotationNameParameter: JKAnnotationNameParameter) {
        printLeftNonCodeElements(annotationNameParameter)
        visitAnnotationNameParameterRaw(annotationNameParameter)
        printRightNonCodeElements(annotationNameParameter)
    }

    open fun visitAnnotationNameParameterRaw(annotationNameParameter: JKAnnotationNameParameter) =
        visitAnnotationParameterRaw(annotationNameParameter)

    override fun visitArgument(argument: JKArgument) {
        printLeftNonCodeElements(argument)
        visitArgumentRaw(argument)
        printRightNonCodeElements(argument)
    }

    open fun visitArgumentRaw(argument: JKArgument) = visitTreeElementRaw(argument)

    override fun visitNamedArgument(namedArgument: JKNamedArgument) {
        printLeftNonCodeElements(namedArgument)
        visitNamedArgumentRaw(namedArgument)
        printRightNonCodeElements(namedArgument)
    }

    open fun visitNamedArgumentRaw(namedArgument: JKNamedArgument) = visitArgumentRaw(namedArgument)

    override fun visitArgumentImpl(argumentImpl: JKArgumentImpl) {
        printLeftNonCodeElements(argumentImpl)
        visitArgumentImplRaw(argumentImpl)
        printRightNonCodeElements(argumentImpl)
    }

    open fun visitArgumentImplRaw(argumentImpl: JKArgumentImpl) = visitArgumentRaw(argumentImpl)

    override fun visitArgumentList(argumentList: JKArgumentList) {
        printLeftNonCodeElements(argumentList)
        visitArgumentListRaw(argumentList)
        printRightNonCodeElements(argumentList)
    }

    open fun visitArgumentListRaw(argumentList: JKArgumentList) = visitTreeElementRaw(argumentList)

    override fun visitTypeParameterList(typeParameterList: JKTypeParameterList) {
        printLeftNonCodeElements(typeParameterList)
        visitTypeParameterListRaw(typeParameterList)
        printRightNonCodeElements(typeParameterList)
    }

    open fun visitTypeParameterListRaw(typeParameterList: JKTypeParameterList) = visitTreeElementRaw(typeParameterList)

    override fun visitAnnotationList(annotationList: JKAnnotationList) {
        printLeftNonCodeElements(annotationList)
        visitAnnotationListRaw(annotationList)
        printRightNonCodeElements(annotationList)
    }

    open fun visitAnnotationListRaw(annotationList: JKAnnotationList) = visitTreeElementRaw(annotationList)

    override fun visitAnnotation(annotation: JKAnnotation) {
        printLeftNonCodeElements(annotation)
        visitAnnotationRaw(annotation)
        printRightNonCodeElements(annotation)
    }

    open fun visitAnnotationRaw(annotation: JKAnnotation) = visitTreeElementRaw(annotation)

    override fun visitTypeArgumentList(typeArgumentList: JKTypeArgumentList) {
        printLeftNonCodeElements(typeArgumentList)
        visitTypeArgumentListRaw(typeArgumentList)
        printRightNonCodeElements(typeArgumentList)
    }

    open fun visitTypeArgumentListRaw(typeArgumentList: JKTypeArgumentList) = visitTreeElementRaw(typeArgumentList)

    override fun visitNameIdentifier(nameIdentifier: JKNameIdentifier) {
        printLeftNonCodeElements(nameIdentifier)
        visitNameIdentifierRaw(nameIdentifier)
        printRightNonCodeElements(nameIdentifier)
    }

    open fun visitNameIdentifierRaw(nameIdentifier: JKNameIdentifier) = visitTreeElementRaw(nameIdentifier)

    override fun visitBlockImpl(blockImpl: JKBlockImpl) {
        printLeftNonCodeElements(blockImpl)
        visitBlockImplRaw(blockImpl)
        printRightNonCodeElements(blockImpl)
    }

    open fun visitBlockImplRaw(blockImpl: JKBlockImpl) = visitBlockRaw(blockImpl)

    override fun visitKtWhenCase(ktWhenCase: JKKtWhenCase) {
        printLeftNonCodeElements(ktWhenCase)
        visitKtWhenCaseRaw(ktWhenCase)
        printRightNonCodeElements(ktWhenCase)
    }

    open fun visitKtWhenCaseRaw(ktWhenCase: JKKtWhenCase) = visitTreeElementRaw(ktWhenCase)

    override fun visitKtWhenLabel(ktWhenLabel: JKKtWhenLabel) {
        printLeftNonCodeElements(ktWhenLabel)
        visitKtWhenLabelRaw(ktWhenLabel)
        printRightNonCodeElements(ktWhenLabel)
    }

    open fun visitKtWhenLabelRaw(ktWhenLabel: JKKtWhenLabel) = visitTreeElementRaw(ktWhenLabel)

    override fun visitKtElseWhenLabel(ktElseWhenLabel: JKKtElseWhenLabel) {
        printLeftNonCodeElements(ktElseWhenLabel)
        visitKtElseWhenLabelRaw(ktElseWhenLabel)
        printRightNonCodeElements(ktElseWhenLabel)
    }

    open fun visitKtElseWhenLabelRaw(ktElseWhenLabel: JKKtElseWhenLabel) = visitKtWhenLabelRaw(ktElseWhenLabel)

    override fun visitKtValueWhenLabel(ktValueWhenLabel: JKKtValueWhenLabel) {
        printLeftNonCodeElements(ktValueWhenLabel)
        visitKtValueWhenLabelRaw(ktValueWhenLabel)
        printRightNonCodeElements(ktValueWhenLabel)
    }

    open fun visitKtValueWhenLabelRaw(ktValueWhenLabel: JKKtValueWhenLabel) = visitKtWhenLabelRaw(ktValueWhenLabel)

    override fun visitClassBody(classBody: JKClassBody) {
        printLeftNonCodeElements(classBody)
        visitClassBodyRaw(classBody)
        printRightNonCodeElements(classBody)
    }

    open fun visitClassBodyRaw(classBody: JKClassBody) = visitTreeElementRaw(classBody)

    override fun visitJavaTryCatchSection(javaTryCatchSection: JKJavaTryCatchSection) {
        printLeftNonCodeElements(javaTryCatchSection)
        visitJavaTryCatchSectionRaw(javaTryCatchSection)
        printRightNonCodeElements(javaTryCatchSection)
    }

    open fun visitJavaTryCatchSectionRaw(javaTryCatchSection: JKJavaTryCatchSection) = visitStatementRaw(javaTryCatchSection)

    override fun visitJavaSwitchCase(javaSwitchCase: JKJavaSwitchCase) {
        printLeftNonCodeElements(javaSwitchCase)
        visitJavaSwitchCaseRaw(javaSwitchCase)
        printRightNonCodeElements(javaSwitchCase)
    }

    open fun visitJavaSwitchCaseRaw(javaSwitchCase: JKJavaSwitchCase) = visitTreeElementRaw(javaSwitchCase)

    override fun visitJavaDefaultSwitchCase(javaDefaultSwitchCase: JKJavaDefaultSwitchCase) {
        printLeftNonCodeElements(javaDefaultSwitchCase)
        visitJavaDefaultSwitchCaseRaw(javaDefaultSwitchCase)
        printRightNonCodeElements(javaDefaultSwitchCase)
    }

    open fun visitJavaDefaultSwitchCaseRaw(javaDefaultSwitchCase: JKJavaDefaultSwitchCase) = visitJavaSwitchCaseRaw(javaDefaultSwitchCase)

    override fun visitJavaLabelSwitchCase(javaLabelSwitchCase: JKJavaLabelSwitchCase) {
        printLeftNonCodeElements(javaLabelSwitchCase)
        visitJavaLabelSwitchCaseRaw(javaLabelSwitchCase)
        printRightNonCodeElements(javaLabelSwitchCase)
    }

    open fun visitJavaLabelSwitchCaseRaw(javaLabelSwitchCase: JKJavaLabelSwitchCase) = visitJavaSwitchCaseRaw(javaLabelSwitchCase)

    override fun visitExpression(expression: JKExpression) {
        printLeftNonCodeElements(expression)
        visitExpressionRaw(expression)
        printRightNonCodeElements(expression)
    }

    open fun visitExpressionRaw(expression: JKExpression) = visitTreeElementRaw(expression)

    override fun visitOperatorExpression(operatorExpression: JKOperatorExpression) {
        printLeftNonCodeElements(operatorExpression)
        visitOperatorExpressionRaw(operatorExpression)
        printRightNonCodeElements(operatorExpression)
    }

    open fun visitOperatorExpressionRaw(operatorExpression: JKOperatorExpression) = visitExpressionRaw(operatorExpression)

    override fun visitBinaryExpression(binaryExpression: JKBinaryExpression) {
        printLeftNonCodeElements(binaryExpression)
        visitBinaryExpressionRaw(binaryExpression)
        printRightNonCodeElements(binaryExpression)
    }

    open fun visitBinaryExpressionRaw(binaryExpression: JKBinaryExpression) = visitOperatorExpressionRaw(binaryExpression)

    override fun visitUnaryExpression(unaryExpression: JKUnaryExpression) {
        printLeftNonCodeElements(unaryExpression)
        visitUnaryExpressionRaw(unaryExpression)
        printRightNonCodeElements(unaryExpression)
    }

    open fun visitUnaryExpressionRaw(unaryExpression: JKUnaryExpression) = visitOperatorExpressionRaw(unaryExpression)

    override fun visitPrefixExpression(prefixExpression: JKPrefixExpression) {
        printLeftNonCodeElements(prefixExpression)
        visitPrefixExpressionRaw(prefixExpression)
        printRightNonCodeElements(prefixExpression)
    }

    open fun visitPrefixExpressionRaw(prefixExpression: JKPrefixExpression) = visitUnaryExpressionRaw(prefixExpression)

    override fun visitPostfixExpression(postfixExpression: JKPostfixExpression) {
        printLeftNonCodeElements(postfixExpression)
        visitPostfixExpressionRaw(postfixExpression)
        printRightNonCodeElements(postfixExpression)
    }

    open fun visitPostfixExpressionRaw(postfixExpression: JKPostfixExpression) = visitUnaryExpressionRaw(postfixExpression)

    override fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression) {
        printLeftNonCodeElements(qualifiedExpression)
        visitQualifiedExpressionRaw(qualifiedExpression)
        printRightNonCodeElements(qualifiedExpression)
    }

    open fun visitQualifiedExpressionRaw(qualifiedExpression: JKQualifiedExpression) = visitExpressionRaw(qualifiedExpression)

    override fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression) {
        printLeftNonCodeElements(parenthesizedExpression)
        visitParenthesizedExpressionRaw(parenthesizedExpression)
        printRightNonCodeElements(parenthesizedExpression)
    }

    open fun visitParenthesizedExpressionRaw(parenthesizedExpression: JKParenthesizedExpression) = visitExpressionRaw(parenthesizedExpression)

    override fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression) {
        printLeftNonCodeElements(typeCastExpression)
        visitTypeCastExpressionRaw(typeCastExpression)
        printRightNonCodeElements(typeCastExpression)
    }

    open fun visitTypeCastExpressionRaw(typeCastExpression: JKTypeCastExpression) = visitExpressionRaw(typeCastExpression)

    override fun visitLiteralExpression(literalExpression: JKLiteralExpression) {
        printLeftNonCodeElements(literalExpression)
        visitLiteralExpressionRaw(literalExpression)
        printRightNonCodeElements(literalExpression)
    }

    open fun visitLiteralExpressionRaw(literalExpression: JKLiteralExpression) = visitExpressionRaw(literalExpression)

    override fun visitStubExpression(stubExpression: JKStubExpression) {
        printLeftNonCodeElements(stubExpression)
        visitStubExpressionRaw(stubExpression)
        printRightNonCodeElements(stubExpression)
    }

    open fun visitStubExpressionRaw(stubExpression: JKStubExpression) = visitExpressionRaw(stubExpression)

    override fun visitThisExpression(thisExpression: JKThisExpression) {
        printLeftNonCodeElements(thisExpression)
        visitThisExpressionRaw(thisExpression)
        printRightNonCodeElements(thisExpression)
    }

    open fun visitThisExpressionRaw(thisExpression: JKThisExpression) = visitExpressionRaw(thisExpression)

    override fun visitSuperExpression(superExpression: JKSuperExpression) {
        printLeftNonCodeElements(superExpression)
        visitSuperExpressionRaw(superExpression)
        printRightNonCodeElements(superExpression)
    }

    open fun visitSuperExpressionRaw(superExpression: JKSuperExpression) = visitExpressionRaw(superExpression)

    override fun visitIfElseExpression(ifElseExpression: JKIfElseExpression) {
        printLeftNonCodeElements(ifElseExpression)
        visitIfElseExpressionRaw(ifElseExpression)
        printRightNonCodeElements(ifElseExpression)
    }

    open fun visitIfElseExpressionRaw(ifElseExpression: JKIfElseExpression) = visitExpressionRaw(ifElseExpression)

    override fun visitLambdaExpression(lambdaExpression: JKLambdaExpression) {
        printLeftNonCodeElements(lambdaExpression)
        visitLambdaExpressionRaw(lambdaExpression)
        printRightNonCodeElements(lambdaExpression)
    }

    open fun visitLambdaExpressionRaw(lambdaExpression: JKLambdaExpression) = visitExpressionRaw(lambdaExpression)

    override fun visitCallExpression(callExpression: JKCallExpression) {
        printLeftNonCodeElements(callExpression)
        visitCallExpressionRaw(callExpression)
        printRightNonCodeElements(callExpression)
    }

    open fun visitCallExpressionRaw(callExpression: JKCallExpression) = visitExpressionRaw(callExpression)

    override fun visitDelegationConstructorCall(delegationConstructorCall: JKDelegationConstructorCall) {
        printLeftNonCodeElements(delegationConstructorCall)
        visitDelegationConstructorCallRaw(delegationConstructorCall)
        printRightNonCodeElements(delegationConstructorCall)
    }

    open fun visitDelegationConstructorCallRaw(delegationConstructorCall: JKDelegationConstructorCall) =
        visitCallExpressionRaw(delegationConstructorCall)

    override fun visitCallExpressionImpl(callExpressionImpl: JKCallExpressionImpl) {
        printLeftNonCodeElements(callExpressionImpl)
        visitCallExpressionImplRaw(callExpressionImpl)
        printRightNonCodeElements(callExpressionImpl)
    }

    open fun visitCallExpressionImplRaw(callExpressionImpl: JKCallExpressionImpl) = visitCallExpressionRaw(callExpressionImpl)

    override fun visitNewExpression(newExpression: JKNewExpression) {
        printLeftNonCodeElements(newExpression)
        visitNewExpressionRaw(newExpression)
        printRightNonCodeElements(newExpression)
    }

    open fun visitNewExpressionRaw(newExpression: JKNewExpression) = visitExpressionRaw(newExpression)

    override fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression) {
        printLeftNonCodeElements(fieldAccessExpression)
        visitFieldAccessExpressionRaw(fieldAccessExpression)
        printRightNonCodeElements(fieldAccessExpression)
    }

    open fun visitFieldAccessExpressionRaw(fieldAccessExpression: JKFieldAccessExpression) = visitExpressionRaw(fieldAccessExpression)

    override fun visitPackageAccessExpression(packageAccessExpression: JKPackageAccessExpression) {
        printLeftNonCodeElements(packageAccessExpression)
        visitPackageAccessExpressionRaw(packageAccessExpression)
        printRightNonCodeElements(packageAccessExpression)
    }

    open fun visitPackageAccessExpressionRaw(packageAccessExpression: JKPackageAccessExpression) =
        visitExpressionRaw(packageAccessExpression)

    override fun visitMethodAccessExpression(methodAccessExpression: JKMethodAccessExpression) {
        printLeftNonCodeElements(methodAccessExpression)
        visitMethodAccessExpressionRaw(methodAccessExpression)
        printRightNonCodeElements(methodAccessExpression)
    }

    open fun visitMethodAccessExpressionRaw(methodAccessExpression: JKMethodAccessExpression) = visitExpressionRaw(methodAccessExpression)

    override fun visitClassAccessExpression(classAccessExpression: JKClassAccessExpression) {
        printLeftNonCodeElements(classAccessExpression)
        visitClassAccessExpressionRaw(classAccessExpression)
        printRightNonCodeElements(classAccessExpression)
    }

    open fun visitClassAccessExpressionRaw(classAccessExpression: JKClassAccessExpression) = visitExpressionRaw(classAccessExpression)

    override fun visitMethodReferenceExpression(methodReferenceExpression: JKMethodReferenceExpression) {
        printLeftNonCodeElements(methodReferenceExpression)
        visitMethodReferenceExpressionRaw(methodReferenceExpression)
        printRightNonCodeElements(methodReferenceExpression)
    }

    open fun visitMethodReferenceExpressionRaw(methodReferenceExpression: JKMethodReferenceExpression) =
        visitExpressionRaw(methodReferenceExpression)

    override fun visitLabeledExpression(labeledExpression: JKLabeledExpression) {
        printLeftNonCodeElements(labeledExpression)
        visitLabeledExpressionRaw(labeledExpression)
        printRightNonCodeElements(labeledExpression)
    }

    open fun visitLabeledExpressionRaw(labeledExpression: JKLabeledExpression) = visitExpressionRaw(labeledExpression)

    override fun visitClassLiteralExpression(classLiteralExpression: JKClassLiteralExpression) {
        printLeftNonCodeElements(classLiteralExpression)
        visitClassLiteralExpressionRaw(classLiteralExpression)
        printRightNonCodeElements(classLiteralExpression)
    }

    open fun visitClassLiteralExpressionRaw(classLiteralExpression: JKClassLiteralExpression) = visitExpressionRaw(classLiteralExpression)

    override fun visitKtAssignmentChainLink(ktAssignmentChainLink: JKKtAssignmentChainLink) {
        printLeftNonCodeElements(ktAssignmentChainLink)
        visitKtAssignmentChainLinkRaw(ktAssignmentChainLink)
        printRightNonCodeElements(ktAssignmentChainLink)
    }

    open fun visitKtAssignmentChainLinkRaw(ktAssignmentChainLink: JKKtAssignmentChainLink) = visitExpressionRaw(ktAssignmentChainLink)

    override fun visitAssignmentChainAlsoLink(assignmentChainAlsoLink: JKAssignmentChainAlsoLink) {
        printLeftNonCodeElements(assignmentChainAlsoLink)
        visitAssignmentChainAlsoLinkRaw(assignmentChainAlsoLink)
        printRightNonCodeElements(assignmentChainAlsoLink)
    }

    open fun visitAssignmentChainAlsoLinkRaw(assignmentChainAlsoLink: JKAssignmentChainAlsoLink) =
        visitKtAssignmentChainLinkRaw(assignmentChainAlsoLink)

    override fun visitAssignmentChainLetLink(assignmentChainLetLink: JKAssignmentChainLetLink) {
        printLeftNonCodeElements(assignmentChainLetLink)
        visitAssignmentChainLetLinkRaw(assignmentChainLetLink)
        printRightNonCodeElements(assignmentChainLetLink)
    }

    open fun visitAssignmentChainLetLinkRaw(assignmentChainLetLink: JKAssignmentChainLetLink) =
        visitKtAssignmentChainLinkRaw(assignmentChainLetLink)

    override fun visitIsExpression(isExpression: JKIsExpression) {
        printLeftNonCodeElements(isExpression)
        visitIsExpressionRaw(isExpression)
        printRightNonCodeElements(isExpression)
    }

    open fun visitIsExpressionRaw(isExpression: JKIsExpression) = visitExpressionRaw(isExpression)

    override fun visitKtThrowExpression(ktThrowExpression: JKKtThrowExpression) {
        printLeftNonCodeElements(ktThrowExpression)
        visitKtThrowExpressionRaw(ktThrowExpression)
        printRightNonCodeElements(ktThrowExpression)
    }

    open fun visitKtThrowExpressionRaw(ktThrowExpression: JKKtThrowExpression) = visitExpressionRaw(ktThrowExpression)

    override fun visitKtItExpression(ktItExpression: JKKtItExpression) {
        printLeftNonCodeElements(ktItExpression)
        visitKtItExpressionRaw(ktItExpression)
        printRightNonCodeElements(ktItExpression)
    }

    open fun visitKtItExpressionRaw(ktItExpression: JKKtItExpression) = visitExpressionRaw(ktItExpression)

    override fun visitKtAnnotationArrayInitializerExpression(ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression) {
        printLeftNonCodeElements(ktAnnotationArrayInitializerExpression)
        visitKtAnnotationArrayInitializerExpressionRaw(ktAnnotationArrayInitializerExpression)
        printRightNonCodeElements(ktAnnotationArrayInitializerExpression)
    }

    open fun visitKtAnnotationArrayInitializerExpressionRaw(ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression) =
        visitExpressionRaw(ktAnnotationArrayInitializerExpression)

    override fun visitKtTryExpression(ktTryExpression: JKKtTryExpression) {
        printLeftNonCodeElements(ktTryExpression)
        visitKtTryExpressionRaw(ktTryExpression)
        printRightNonCodeElements(ktTryExpression)
    }

    open fun visitKtTryExpressionRaw(ktTryExpression: JKKtTryExpression) = visitExpressionRaw(ktTryExpression)

    override fun visitKtTryCatchSection(ktTryCatchSection: JKKtTryCatchSection) {
        printLeftNonCodeElements(ktTryCatchSection)
        visitKtTryCatchSectionRaw(ktTryCatchSection)
        printRightNonCodeElements(ktTryCatchSection)
    }

    open fun visitKtTryCatchSectionRaw(ktTryCatchSection: JKKtTryCatchSection) = visitTreeElementRaw(ktTryCatchSection)

    override fun visitJavaNewEmptyArray(javaNewEmptyArray: JKJavaNewEmptyArray) {
        printLeftNonCodeElements(javaNewEmptyArray)
        visitJavaNewEmptyArrayRaw(javaNewEmptyArray)
        printRightNonCodeElements(javaNewEmptyArray)
    }

    open fun visitJavaNewEmptyArrayRaw(javaNewEmptyArray: JKJavaNewEmptyArray) = visitExpressionRaw(javaNewEmptyArray)

    override fun visitJavaNewArray(javaNewArray: JKJavaNewArray) {
        printLeftNonCodeElements(javaNewArray)
        visitJavaNewArrayRaw(javaNewArray)
        printRightNonCodeElements(javaNewArray)
    }

    open fun visitJavaNewArrayRaw(javaNewArray: JKJavaNewArray) = visitExpressionRaw(javaNewArray)

    override fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression) {
        printLeftNonCodeElements(javaAssignmentExpression)
        visitJavaAssignmentExpressionRaw(javaAssignmentExpression)
        printRightNonCodeElements(javaAssignmentExpression)
    }

    open fun visitJavaAssignmentExpressionRaw(javaAssignmentExpression: JKJavaAssignmentExpression) = visitExpressionRaw(javaAssignmentExpression)

    override fun visitModifierElement(modifierElement: JKModifierElement) {
        printLeftNonCodeElements(modifierElement)
        visitModifierElementRaw(modifierElement)
        printRightNonCodeElements(modifierElement)
    }

    open fun visitModifierElementRaw(modifierElement: JKModifierElement) = visitTreeElementRaw(modifierElement)

    override fun visitMutabilityModifierElement(mutabilityModifierElement: JKMutabilityModifierElement) {
        printLeftNonCodeElements(mutabilityModifierElement)
        visitMutabilityModifierElementRaw(mutabilityModifierElement)
        printRightNonCodeElements(mutabilityModifierElement)
    }

    open fun visitMutabilityModifierElementRaw(mutabilityModifierElement: JKMutabilityModifierElement) =
        visitModifierElementRaw(mutabilityModifierElement)

    override fun visitModalityModifierElement(modalityModifierElement: JKModalityModifierElement) {
        printLeftNonCodeElements(modalityModifierElement)
        visitModalityModifierElementRaw(modalityModifierElement)
        printRightNonCodeElements(modalityModifierElement)
    }

    open fun visitModalityModifierElementRaw(modalityModifierElement: JKModalityModifierElement) =
        visitModifierElementRaw(modalityModifierElement)

    override fun visitVisibilityModifierElement(visibilityModifierElement: JKVisibilityModifierElement) {
        printLeftNonCodeElements(visibilityModifierElement)
        visitVisibilityModifierElementRaw(visibilityModifierElement)
        printRightNonCodeElements(visibilityModifierElement)
    }

    open fun visitVisibilityModifierElementRaw(visibilityModifierElement: JKVisibilityModifierElement) =
        visitModifierElementRaw(visibilityModifierElement)

    override fun visitOtherModifierElement(otherModifierElement: JKOtherModifierElement) {
        printLeftNonCodeElements(otherModifierElement)
        visitOtherModifierElementRaw(otherModifierElement)
        printRightNonCodeElements(otherModifierElement)
    }

    open fun visitOtherModifierElementRaw(otherModifierElement: JKOtherModifierElement) = visitModifierElementRaw(otherModifierElement)

    override fun visitStatement(statement: JKStatement) {
        printLeftNonCodeElements(statement)
        visitStatementRaw(statement)
        printRightNonCodeElements(statement)
    }

    open fun visitStatementRaw(statement: JKStatement) = visitTreeElementRaw(statement)

    override fun visitEmptyStatement(emptyStatement: JKEmptyStatement) {
        printLeftNonCodeElements(emptyStatement)
        visitEmptyStatementRaw(emptyStatement)
        printRightNonCodeElements(emptyStatement)
    }

    open fun visitEmptyStatementRaw(emptyStatement: JKEmptyStatement) = visitStatementRaw(emptyStatement)

    override fun visitLoopStatement(loopStatement: JKLoopStatement) {
        printLeftNonCodeElements(loopStatement)
        visitLoopStatementRaw(loopStatement)
        printRightNonCodeElements(loopStatement)
    }

    open fun visitLoopStatementRaw(loopStatement: JKLoopStatement) = visitStatementRaw(loopStatement)

    override fun visitWhileStatement(whileStatement: JKWhileStatement) {
        printLeftNonCodeElements(whileStatement)
        visitWhileStatementRaw(whileStatement)
        printRightNonCodeElements(whileStatement)
    }

    open fun visitWhileStatementRaw(whileStatement: JKWhileStatement) = visitLoopStatementRaw(whileStatement)

    override fun visitDoWhileStatement(doWhileStatement: JKDoWhileStatement) {
        printLeftNonCodeElements(doWhileStatement)
        visitDoWhileStatementRaw(doWhileStatement)
        printRightNonCodeElements(doWhileStatement)
    }

    open fun visitDoWhileStatementRaw(doWhileStatement: JKDoWhileStatement) = visitLoopStatementRaw(doWhileStatement)

    override fun visitForInStatement(forInStatement: JKForInStatement) {
        printLeftNonCodeElements(forInStatement)
        visitForInStatementRaw(forInStatement)
        printRightNonCodeElements(forInStatement)
    }

    open fun visitForInStatementRaw(forInStatement: JKForInStatement) = visitStatementRaw(forInStatement)

    override fun visitIfElseStatement(ifElseStatement: JKIfElseStatement) {
        printLeftNonCodeElements(ifElseStatement)
        visitIfElseStatementRaw(ifElseStatement)
        printRightNonCodeElements(ifElseStatement)
    }

    open fun visitIfElseStatementRaw(ifElseStatement: JKIfElseStatement) = visitStatementRaw(ifElseStatement)

    override fun visitBreakStatement(breakStatement: JKBreakStatement) {
        printLeftNonCodeElements(breakStatement)
        visitBreakStatementRaw(breakStatement)
        printRightNonCodeElements(breakStatement)
    }

    open fun visitBreakStatementRaw(breakStatement: JKBreakStatement) = visitStatementRaw(breakStatement)

    override fun visitContinueStatement(continueStatement: JKContinueStatement) {
        printLeftNonCodeElements(continueStatement)
        visitContinueStatementRaw(continueStatement)
        printRightNonCodeElements(continueStatement)
    }

    open fun visitContinueStatementRaw(continueStatement: JKContinueStatement) = visitStatementRaw(continueStatement)

    override fun visitBlockStatement(blockStatement: JKBlockStatement) {
        printLeftNonCodeElements(blockStatement)
        visitBlockStatementRaw(blockStatement)
        printRightNonCodeElements(blockStatement)
    }

    open fun visitBlockStatementRaw(blockStatement: JKBlockStatement) = visitStatementRaw(blockStatement)

    override fun visitBlockStatementWithoutBrackets(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets) {
        printLeftNonCodeElements(blockStatementWithoutBrackets)
        visitBlockStatementWithoutBracketsRaw(blockStatementWithoutBrackets)
        printRightNonCodeElements(blockStatementWithoutBrackets)
    }

    open fun visitBlockStatementWithoutBracketsRaw(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets) =
        visitStatementRaw(blockStatementWithoutBrackets)

    override fun visitExpressionStatement(expressionStatement: JKExpressionStatement) {
        printLeftNonCodeElements(expressionStatement)
        visitExpressionStatementRaw(expressionStatement)
        printRightNonCodeElements(expressionStatement)
    }

    open fun visitExpressionStatementRaw(expressionStatement: JKExpressionStatement) = visitStatementRaw(expressionStatement)

    override fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement) {
        printLeftNonCodeElements(declarationStatement)
        visitDeclarationStatementRaw(declarationStatement)
        printRightNonCodeElements(declarationStatement)
    }

    open fun visitDeclarationStatementRaw(declarationStatement: JKDeclarationStatement) = visitStatementRaw(declarationStatement)

    override fun visitKtWhenStatement(ktWhenStatement: JKKtWhenStatement) {
        printLeftNonCodeElements(ktWhenStatement)
        visitKtWhenStatementRaw(ktWhenStatement)
        printRightNonCodeElements(ktWhenStatement)
    }

    open fun visitKtWhenStatementRaw(ktWhenStatement: JKKtWhenStatement) = visitStatementRaw(ktWhenStatement)

    override fun visitKtConvertedFromForLoopSyntheticWhileStatement(ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement) {
        printLeftNonCodeElements(ktConvertedFromForLoopSyntheticWhileStatement)
        visitKtConvertedFromForLoopSyntheticWhileStatementRaw(ktConvertedFromForLoopSyntheticWhileStatement)
        printRightNonCodeElements(ktConvertedFromForLoopSyntheticWhileStatement)
    }

    open fun visitKtConvertedFromForLoopSyntheticWhileStatementRaw(ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement) =
        visitStatementRaw(ktConvertedFromForLoopSyntheticWhileStatement)

    override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement) {
        printLeftNonCodeElements(ktAssignmentStatement)
        visitKtAssignmentStatementRaw(ktAssignmentStatement)
        printRightNonCodeElements(ktAssignmentStatement)
    }

    open fun visitKtAssignmentStatementRaw(ktAssignmentStatement: JKKtAssignmentStatement) = visitStatementRaw(ktAssignmentStatement)

    override fun visitReturnStatement(returnStatement: JKReturnStatement) {
        printLeftNonCodeElements(returnStatement)
        visitReturnStatementRaw(returnStatement)
        printRightNonCodeElements(returnStatement)
    }

    open fun visitReturnStatementRaw(returnStatement: JKReturnStatement) = visitStatementRaw(returnStatement)

    override fun visitJavaSwitchStatement(javaSwitchStatement: JKJavaSwitchStatement) {
        printLeftNonCodeElements(javaSwitchStatement)
        visitJavaSwitchStatementRaw(javaSwitchStatement)
        printRightNonCodeElements(javaSwitchStatement)
    }

    open fun visitJavaSwitchStatementRaw(javaSwitchStatement: JKJavaSwitchStatement) = visitStatementRaw(javaSwitchStatement)

    override fun visitJavaThrowStatement(javaThrowStatement: JKJavaThrowStatement) {
        printLeftNonCodeElements(javaThrowStatement)
        visitJavaThrowStatementRaw(javaThrowStatement)
        printRightNonCodeElements(javaThrowStatement)
    }

    open fun visitJavaThrowStatementRaw(javaThrowStatement: JKJavaThrowStatement) = visitStatementRaw(javaThrowStatement)

    override fun visitJavaTryStatement(javaTryStatement: JKJavaTryStatement) {
        printLeftNonCodeElements(javaTryStatement)
        visitJavaTryStatementRaw(javaTryStatement)
        printRightNonCodeElements(javaTryStatement)
    }

    open fun visitJavaTryStatementRaw(javaTryStatement: JKJavaTryStatement) = visitStatementRaw(javaTryStatement)

    override fun visitJavaSynchronizedStatement(javaSynchronizedStatement: JKJavaSynchronizedStatement) {
        printLeftNonCodeElements(javaSynchronizedStatement)
        visitJavaSynchronizedStatementRaw(javaSynchronizedStatement)
        printRightNonCodeElements(javaSynchronizedStatement)
    }

    open fun visitJavaSynchronizedStatementRaw(javaSynchronizedStatement: JKJavaSynchronizedStatement) = visitStatementRaw(javaSynchronizedStatement)

    override fun visitJavaAssertStatement(javaAssertStatement: JKJavaAssertStatement) {
        printLeftNonCodeElements(javaAssertStatement)
        visitJavaAssertStatementRaw(javaAssertStatement)
        printRightNonCodeElements(javaAssertStatement)
    }

    open fun visitJavaAssertStatementRaw(javaAssertStatement: JKJavaAssertStatement) = visitStatementRaw(javaAssertStatement)

    override fun visitJavaForLoopStatement(javaForLoopStatement: JKJavaForLoopStatement) {
        printLeftNonCodeElements(javaForLoopStatement)
        visitJavaForLoopStatementRaw(javaForLoopStatement)
        printRightNonCodeElements(javaForLoopStatement)
    }

    open fun visitJavaForLoopStatementRaw(javaForLoopStatement: JKJavaForLoopStatement) = visitLoopStatementRaw(javaForLoopStatement)

    override fun visitJavaAnnotationMethod(javaAnnotationMethod: JKJavaAnnotationMethod) {
        printLeftNonCodeElements(javaAnnotationMethod)
        visitJavaAnnotationMethodRaw(javaAnnotationMethod)
        printRightNonCodeElements(javaAnnotationMethod)
    }

    open fun visitJavaAnnotationMethodRaw(javaAnnotationMethod: JKJavaAnnotationMethod) = visitMethodRaw(javaAnnotationMethod)
}
