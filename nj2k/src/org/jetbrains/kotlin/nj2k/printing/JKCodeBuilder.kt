/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.printing

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.symbols.getDisplayFqName
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitorWithCommentsPrinting
import org.jetbrains.kotlin.nj2k.types.JKContextType
import org.jetbrains.kotlin.nj2k.types.isInterface
import org.jetbrains.kotlin.nj2k.types.updateNullability
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


internal class JKCodeBuilder(context: NewJ2kConverterContext) {
    private val elementInfoStorage = context.elementsInfoStorage
    private val printer = JKPrinter(context.project, context.importStorage, elementInfoStorage)
    private val commentPrinter = JKCommentPrinter(printer)

    fun printCodeOut(root: JKTreeElement): String {
        Visitor().also { root.accept(it) }
        return printer.toString().replace("\r\n", "\n")
    }

    private inner class Visitor : JKVisitorWithCommentsPrinting() {
        override fun printLeftNonCodeElements(element: JKFormattingOwner) {
            commentPrinter.printTrailingComments(element)
        }

        override fun printRightNonCodeElements(element: JKFormattingOwner) {
            commentPrinter.printLeadingComments(element)
        }

        private fun renderTokenElement(tokenElement: JKTokenElement) {
            printLeftNonCodeElements(tokenElement)
            printer.print(tokenElement.text)
            printRightNonCodeElements(tokenElement)
        }

        private fun renderExtraTypeParametersUpperBounds(typeParameterList: JKTypeParameterList) {
            val extraTypeBounds = typeParameterList.typeParameters
                .filter { it.upperBounds.size > 1 }
            if (extraTypeBounds.isNotEmpty()) {
                printer.print(" where ")
                val typeParametersWithBounds =
                    extraTypeBounds.flatMap { typeParameter ->
                        typeParameter.upperBounds.map { bound ->
                            typeParameter.name to bound
                        }
                    }
                printer.renderList(typeParametersWithBounds) { (name, bound) ->
                    name.accept(this)
                    printer.print(" : ")
                    bound.accept(this)
                }
            }
        }

        private fun renderModifiersList(modifiersList: JKModifiersListOwner) {
            val hasOverrideModifier = modifiersList
                .safeAs<JKOtherModifiersOwner>()
                ?.hasOtherModifier(OtherModifier.OVERRIDE) == true
            modifiersList.forEachModifier { modifierElement ->
                if (modifierElement.modifier == Modality.FINAL || modifierElement.modifier == Visibility.PUBLIC) {
                    if (hasOverrideModifier) {
                        modifierElement.accept(this)
                    } else {
                        printLeftNonCodeElements(modifierElement)
                        printRightNonCodeElements(modifierElement)
                    }
                } else {
                    modifierElement.accept(this)
                }
                printer.print(" ")
            }
        }

        override fun visitTreeElementRaw(treeElement: JKTreeElement) {
            printer.print("/* !!! Hit visitElement for element type: ${treeElement::class} !!! */")
        }

        override fun visitModifierElementRaw(modifierElement: JKModifierElement) {
            if (modifierElement.modifier != Modality.FINAL) {
                printer.print(modifierElement.modifier.text)
            }
        }

        override fun visitTreeRootRaw(treeRoot: JKTreeRoot) {
            treeRoot.element.accept(this)
        }

        override fun visitKtTryExpressionRaw(ktTryExpression: JKKtTryExpression) {
            printer.print("try ")
            ktTryExpression.tryBlock.accept(this)
            ktTryExpression.catchSections.forEach { it.accept(this) }
            if (ktTryExpression.finallyBlock != JKBodyStub) {
                printer.print("finally ")
                ktTryExpression.finallyBlock.accept(this)
            }
        }

        override fun visitKtTryCatchSectionRaw(ktTryCatchSection: JKKtTryCatchSection) {
            printer.print("catch ")
            printer.par {
                ktTryCatchSection.parameter.accept(this)
            }
            ktTryCatchSection.block.accept(this)
        }

        override fun visitForInStatementRaw(forInStatement: JKForInStatement) {
            printer.print("for (")
            forInStatement.declaration.accept(this)
            printer.print(" in ")
            forInStatement.iterationExpression.accept(this)
            printer.print(") ")
            if (forInStatement.body.isEmpty()) {
                printer.print(";")
            } else {
                forInStatement.body.accept(this)
            }
        }

        override fun visitKtThrowExpressionRaw(ktThrowExpression: JKKtThrowExpression) {
            printer.print("throw ")
            ktThrowExpression.exception.accept(this)
        }

        override fun visitDoWhileStatementRaw(doWhileStatement: JKDoWhileStatement) {
            printer.print("do ")
            doWhileStatement.body.accept(this)
            printer.print(" while (")
            doWhileStatement.condition.accept(this)
            printer.print(")")
        }

        override fun visitClassAccessExpressionRaw(classAccessExpression: JKClassAccessExpression) {
            printer.renderSymbol(classAccessExpression.identifier, classAccessExpression)
        }

        override fun visitFileRaw(file: JKFile) {
            if (file.packageDeclaration.name.value.isNotEmpty()) {
                file.packageDeclaration.accept(this)
            }
            file.importList.accept(this)
            file.declarationList.forEach { it.accept(this) }
        }


        override fun visitPackageDeclarationRaw(packageDeclaration: JKPackageDeclaration) {
            printer.print("package ")
            val packageNameEscaped = packageDeclaration.name.value.escapedAsQualifiedName()
            printer.print(packageNameEscaped)
            printer.println()
        }

        override fun visitImportListRaw(importList: JKImportList) {
            importList.imports.forEach { it.accept(this) }
        }

        override fun visitImportStatementRaw(importStatement: JKImportStatement) {
            printer.print("import ")
            val importNameEscaped =
                importStatement.name.value.escapedAsQualifiedName()
            printer.print(importNameEscaped)
            printer.println()
        }

        override fun visitBreakStatementRaw(breakStatement: JKBreakStatement) {
            printer.print("break")
            breakStatement.label.accept(this)
        }

        override fun visitClassRaw(klass: JKClass) {
            klass.annotationList.accept(this)
            if (klass.annotationList.annotations.isNotEmpty()) {
                printer.println()
            }
            renderModifiersList(klass)
            printer.print(" ")
            printer.print(klass.classKind.text)
            printer.print(" ")
            klass.name.accept(this)
            klass.typeParameterList.accept(this)
            printer.print(" ")

            val primaryConstructor = klass.primaryConstructor()
            primaryConstructor?.accept(this)

            if (klass.inheritance.present()) {
                printer.print(" : ")
                klass.inheritance.accept(this)
            }

            renderExtraTypeParametersUpperBounds(klass.typeParameterList)

            klass.classBody.accept(this)
        }

        override fun visitInheritanceInfoRaw(inheritanceInfo: JKInheritanceInfo) {
            val parentClass = inheritanceInfo.parentOfType<JKClass>()!!
            val isInInterface = parentClass.classKind == JKClass.ClassKind.INTERFACE
            val extendTypes = inheritanceInfo.extends.map { it.type.updateNullability(Nullability.NotNull) }
            val implementTypes = inheritanceInfo.implements.map { it.type.updateNullability(Nullability.NotNull) }
            if (isInInterface) {
                printer.renderList(extendTypes) { printer.renderType(it, null) }
            } else {
                extendTypes.singleOrNull()?.also { superType ->
                    printer.renderType(superType, null)
                    val primaryConstructor = parentClass.primaryConstructor()
                    val delegationCall =
                        primaryConstructor
                            ?.delegationCall
                            ?.let { it as? JKDelegationConstructorCall }
                    if (delegationCall != null) {
                        printer.par { delegationCall.arguments.accept(this) }
                    } else if (!superType.isInterface() && primaryConstructor != null) {
                        printer.print("()")
                    }
                }
            }

            if (implementTypes.isNotEmpty() && extendTypes.size == 1) {
                printer.print(", ")
            }
            printer.renderList(implementTypes) { printer.renderType(it, null) }
        }


        private fun renderEnumConstants(enumConstants: List<JKEnumConstant>) {
            printer.renderList(enumConstants) {
                it.accept(this)
            }
        }

        private fun renderNonEnumClassDeclarations(declarations: List<JKDeclaration>) {
            printer.renderList(declarations, { printer.println() }) {
                it.accept(this)
            }
        }

        override fun visitFieldRaw(field: JKField) {
            field.annotationList.accept(this)
            if (field.annotationList.annotations.isNotEmpty()) {
                printer.println()
            }
            renderModifiersList(field)

            printer.print(" ")
            field.name.accept(this)
            if (field.type.present()) {
                printer.print(":")
                field.type.accept(this)
            }
            if (field.initializer !is JKStubExpression) {
                printer.print(" = ")
                field.initializer.accept(this)
            }
        }

        override fun visitEnumConstantRaw(enumConstant: JKEnumConstant) {
            enumConstant.name.accept(this)
            if (enumConstant.arguments.arguments.isNotEmpty()) {
                printer.par {
                    enumConstant.arguments.accept(this)
                }
            }
            if (enumConstant.body.declarations.isNotEmpty()) {
                enumConstant.body.accept(this)
            }
        }

        override fun visitKtInitDeclarationRaw(ktInitDeclaration: JKKtInitDeclaration) {
            if (ktInitDeclaration.block.statements.isNotEmpty()) {
                printer.print("init ")
                ktInitDeclaration.block.accept(this)
            }
        }

        override fun visitIsExpressionRaw(isExpression: JKIsExpression) {
            isExpression.expression.accept(this)
            printer.print(" is ")
            isExpression.type.accept(this)
        }

        override fun visitParameterRaw(parameter: JKParameter) {
            renderModifiersList(parameter)
            printer.print(" ")
            parameter.annotationList.accept(this)
            printer.print(" ")
            if (parameter.isVarArgs) {
                printer.print("vararg ")
            }
            if (parameter.parent is JKKtPrimaryConstructor
                && (parameter.parent?.parent?.parent as? JKClass)?.classKind == JKClass.ClassKind.ANNOTATION
            ) {
                printer.print(" val ")
            }
            parameter.name.accept(this)
            if (parameter.type.present() && parameter.type.type !is JKContextType) {
                printer.print(":")
                parameter.type.accept(this)
            }
            if (parameter.initializer !is JKStubExpression) {
                printer.print(" = ")
                parameter.initializer.accept(this)
            }
        }

        override fun visitKtAnnotationArrayInitializerExpressionRaw(
            ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression
        ) {
            printer.print("[")
            printer.renderList(ktAnnotationArrayInitializerExpression.initializers) {
                it.accept(this)
            }
            printer.print("]")
        }

        override fun visitForLoopVariableRaw(forLoopVariable: JKForLoopVariable) {
            forLoopVariable.name.accept(this)
            if (forLoopVariable.type.present() && forLoopVariable.type.type !is JKContextType) {
                printer.print(": ")
                forLoopVariable.type.accept(this)
            }
        }

        override fun visitMethodRaw(method: JKMethod) {
            if (method.annotationList.annotations.isNotEmpty()) {
                method.annotationList.accept(this)
                printer.print(" ")
            }
            renderModifiersList(method)
            printer.print(" fun ")
            method.typeParameterList.accept(this)

            elementInfoStorage.getOrCreateInfoForElement(method).let {
                printer.print(it.render())
            }
            method.name.accept(this)
            renderTokenElement(method.leftParen)
            printer.renderList(method.parameters) {
                it.accept(this)
            }
            renderTokenElement(method.rightParen)
            printer.print(": ")
            method.returnType.accept(this)
            renderExtraTypeParametersUpperBounds(method.typeParameterList)
            method.block.accept(this)
        }

        override fun visitIfElseExpressionRaw(ifElseExpression: JKIfElseExpression) {
            printer.print("if (")
            ifElseExpression.condition.accept(this)
            printer.print(")")
            ifElseExpression.thenBranch.accept(this)
            if (ifElseExpression.elseBranch !is JKStubExpression) {
                printer.print(" else ")
                ifElseExpression.elseBranch.accept(this)
            }
        }


        override fun visitIfElseStatementRaw(ifElseStatement: JKIfElseStatement) {
            printer.print("if (")
            ifElseStatement.condition.accept(this)
            printer.print(")")
            if (ifElseStatement.thenBranch.isEmpty()) {
                printer.print(";")
            } else {
                ifElseStatement.thenBranch.accept(this)
            }
            if (!ifElseStatement.elseBranch.isEmpty()) {
                printer.print(" else ")
                ifElseStatement.elseBranch.accept(this)
            }
        }


        override fun visitBinaryExpressionRaw(binaryExpression: JKBinaryExpression) {
            binaryExpression.left.accept(this)
            printer.print(" ")
            printer.print(binaryExpression.operator.token.text)
            printer.print(" ")
            binaryExpression.right.accept(this)
        }

        override fun visitTypeParameterListRaw(typeParameterList: JKTypeParameterList) {
            if (typeParameterList.typeParameters.isNotEmpty()) {
                printer.par(JKPrinterBase.ParenthesisKind.ANGLE) {
                    printer.renderList(typeParameterList.typeParameters) {
                        it.accept(this)
                    }
                }
            }
        }

        override fun visitTypeParameterRaw(typeParameter: JKTypeParameter) {
            typeParameter.name.accept(this)
            if (typeParameter.upperBounds.size == 1) {
                printer.print(" : ")
                typeParameter.upperBounds.single().accept(this)
            }
        }

        override fun visitLiteralExpressionRaw(literalExpression: JKLiteralExpression) {
            printer.print(literalExpression.literal)
        }

        override fun visitPrefixExpressionRaw(prefixExpression: JKPrefixExpression) {
            printer.print(prefixExpression.operator.token.text)
            prefixExpression.expression.accept(this)
        }

        override fun visitThisExpressionRaw(thisExpression: JKThisExpression) {
            printer.print("this")
            thisExpression.qualifierLabel.accept(this)
        }

        override fun visitSuperExpressionRaw(superExpression: JKSuperExpression) {
            printer.print("super")
            superExpression.qualifierLabel.accept(this)
        }

        override fun visitContinueStatementRaw(continueStatement: JKContinueStatement) {
            printer.print("continue")
            continueStatement.label.accept(this)
            printer.print(" ")
        }

        override fun visitLabelEmptyRaw(labelEmpty: JKLabelEmpty) {}

        override fun visitLabelTextRaw(labelText: JKLabelText) {
            printer.print("@")
            labelText.label.accept(this)
            printer.print(" ")
        }

        override fun visitLabeledExpressionRaw(labeledExpression: JKLabeledExpression) {
            for (label in labeledExpression.labels) {
                label.accept(this)
                printer.print("@")
            }
            labeledExpression.statement.accept(this)
        }

        override fun visitNameIdentifierRaw(nameIdentifier: JKNameIdentifier) {
            printer.print(nameIdentifier.value.escaped())
        }

        override fun visitPostfixExpressionRaw(postfixExpression: JKPostfixExpression) {
            postfixExpression.expression.accept(this)
            printer.print(postfixExpression.operator.token.text)
        }

        override fun visitQualifiedExpressionRaw(qualifiedExpression: JKQualifiedExpression) {
            qualifiedExpression.receiver.accept(this)
            printer.print(".")
            qualifiedExpression.selector.accept(this)
        }


        override fun visitArgumentListRaw(argumentList: JKArgumentList) {
            printer.renderList(argumentList.arguments) { it.accept(this) }
        }

        override fun visitArgumentRaw(argument: JKArgument) {
            argument.value.accept(this)
        }

        override fun visitNamedArgumentRaw(namedArgument: JKNamedArgument) {
            namedArgument.name.accept(this)
            printer.print(" = ")
            namedArgument.value.accept(this)
        }

        override fun visitCallExpressionRaw(callExpression: JKCallExpression) {
            printer.renderSymbol(callExpression.identifier, callExpression)
            callExpression.typeArgumentList.accept(this)
            printer.par {
                callExpression.arguments.accept(this)
            }
        }

        override fun visitTypeArgumentListRaw(typeArgumentList: JKTypeArgumentList) {
            if (typeArgumentList.typeArguments.isNotEmpty()) {
                printer.par(JKPrinterBase.ParenthesisKind.ANGLE) {
                    printer.renderList(typeArgumentList.typeArguments) {
                        it.accept(this)
                    }
                }
            }
        }

        override fun visitParenthesizedExpressionRaw(parenthesizedExpression: JKParenthesizedExpression) {
            printer.par {
                parenthesizedExpression.expression.accept(this)
            }
        }

        override fun visitDeclarationStatementRaw(declarationStatement: JKDeclarationStatement) {
            printer.renderList(declarationStatement.declaredStatements, { printer.println() }) {
                it.accept(this)
            }
        }

        override fun visitTypeCastExpressionRaw(typeCastExpression: JKTypeCastExpression) {
            typeCastExpression.expression.accept(this)
            printer.print(" as ")
            typeCastExpression.type.accept(this)
        }

        override fun visitWhileStatementRaw(whileStatement: JKWhileStatement) {
            printer.print("while (")
            whileStatement.condition.accept(this)
            printer.print(")")
            if (whileStatement.body.isEmpty()) {
                printer.print(";")
            } else {
                whileStatement.body.accept(this)
            }
        }

        override fun visitLocalVariableRaw(localVariable: JKLocalVariable) {
            printer.print(" ")
            localVariable.annotationList.accept(this)
            printer.print(" ")
            renderModifiersList(localVariable)
            printer.print(" ")
            localVariable.name.accept(this)
            if (localVariable.type.present() && localVariable.type.type != JKContextType) {
                printer.print(": ")
                localVariable.type.accept(this)
            }
            if (localVariable.initializer !is JKStubExpression) {
                printer.print(" = ")
                localVariable.initializer.accept(this)
            }
        }

        override fun visitEmptyStatementRaw(emptyStatement: JKEmptyStatement) {}

        override fun visitStubExpressionRaw(stubExpression: JKStubExpression) {}

        override fun visitKtConvertedFromForLoopSyntheticWhileStatementRaw(
            ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement
        ) {
            printer.renderList(
                ktConvertedFromForLoopSyntheticWhileStatement.variableDeclarations,
                { printer.println() }) {
                it.accept(this)
            }
            printer.println()
            ktConvertedFromForLoopSyntheticWhileStatement.whileStatement.accept(this)
        }

        override fun visitNewExpressionRaw(newExpression: JKNewExpression) {
            if (newExpression.isAnonymousClass) {
                printer.print("object : ")
            }
            printer.renderSymbol(newExpression.classSymbol, newExpression)
            newExpression.typeArgumentList.accept(this)
            if (!newExpression.classSymbol.isInterface() || newExpression.arguments.arguments.isNotEmpty()) {
                printer.par(JKPrinterBase.ParenthesisKind.ROUND) {
                    newExpression.arguments.accept(this)
                }
            }
            if (newExpression.isAnonymousClass) {
                newExpression.classBody.accept(this)
            }
        }

        override fun visitKtItExpressionRaw(ktItExpression: JKKtItExpression) {
            printer.print("it")
        }

        override fun visitClassBodyRaw(classBody: JKClassBody) {
            val declarationsToPrint = classBody.declarations.filterNot { it is JKKtPrimaryConstructor }
            renderTokenElement(classBody.leftBrace)
            if (declarationsToPrint.isNotEmpty()) {
                printer.indented {
                    printer.println()
                    val enumConstants = declarationsToPrint.filterIsInstance<JKEnumConstant>()
                    val otherDeclarations = declarationsToPrint.filterNot { it is JKEnumConstant }
                    renderEnumConstants(enumConstants)
                    if ((classBody.parent as? JKClass)?.classKind == JKClass.ClassKind.ENUM
                        && otherDeclarations.isNotEmpty()
                    ) {
                        printer.print(";")
                        printer.println()
                    }
                    if (enumConstants.isNotEmpty() && otherDeclarations.isNotEmpty()) {
                        printer.println()
                    }
                    renderNonEnumClassDeclarations(otherDeclarations)
                }
                printer.println()
            }
            renderTokenElement(classBody.rightBrace)
        }

        override fun visitTypeElementRaw(typeElement: JKTypeElement) {
            typeElement.annotationList.accept(this)
            printer.renderType(typeElement.type, typeElement)
        }

        override fun visitBlockRaw(block: JKBlock) {
            renderTokenElement(block.leftBrace)
            if (block.statements.isNotEmpty()) {
                printer.indented {
                    printer.println()
                    printer.renderList(block.statements, { printer.println() }) {
                        it.accept(this)
                    }
                }
                printer.println()
            }
            renderTokenElement(block.rightBrace)
        }

        override fun visitBlockStatementWithoutBracketsRaw(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets) {
            printer.renderList(blockStatementWithoutBrackets.statements, { printer.println() }) {
                it.accept(this)
            }
        }

        override fun visitExpressionStatementRaw(expressionStatement: JKExpressionStatement) {
            expressionStatement.expression.accept(this)
        }

        override fun visitReturnStatementRaw(returnStatement: JKReturnStatement) {
            printer.print("return")
            returnStatement.label.accept(this)
            printer.print(" ")
            returnStatement.expression.accept(this)
        }

        override fun visitFieldAccessExpressionRaw(fieldAccessExpression: JKFieldAccessExpression) {
            printer.renderSymbol(fieldAccessExpression.identifier, fieldAccessExpression)
        }

        override fun visitPackageAccessExpressionRaw(packageAccessExpression: JKPackageAccessExpression) {
            printer.renderSymbol(packageAccessExpression.identifier, packageAccessExpression)
        }

        override fun visitMethodReferenceExpressionRaw(methodReferenceExpression: JKMethodReferenceExpression) {
            methodReferenceExpression.qualifier.accept(this)
            printer.print("::")
            val needFqName = methodReferenceExpression.qualifier is JKStubExpression
            val displayName =
                if (needFqName) methodReferenceExpression.identifier.getDisplayFqName()
                else methodReferenceExpression.identifier.name

            printer.print(displayName.escapedAsQualifiedName())

        }

        override fun visitDelegationConstructorCallRaw(delegationConstructorCall: JKDelegationConstructorCall) {
            delegationConstructorCall.expression.accept(this)
            printer.par {
                delegationConstructorCall.arguments.accept(this)
            }
        }

        private fun renderParameterList(parameters: List<JKParameter>) {
            printer.par(JKPrinterBase.ParenthesisKind.ROUND) {
                printer.renderList(parameters) {
                    it.accept(this)
                }
            }
        }

        override fun visitConstructorRaw(constructor: JKConstructor) {
            constructor.annotationList.accept(this)
            if (constructor.annotationList.annotations.isNotEmpty()) {
                printer.println()
            }
            renderModifiersList(constructor)
            printer.print(" constructor")
            renderParameterList(constructor.parameters)
            if (constructor.delegationCall !is JKStubExpression) {
                printer.print(" : ")
                constructor.delegationCall.accept(this)
            }
            constructor.block.accept(this)
        }

        override fun visitKtPrimaryConstructorRaw(ktPrimaryConstructor: JKKtPrimaryConstructor) {
            ktPrimaryConstructor.annotationList.accept(this)
            printer.print(" ")
            renderModifiersList(ktPrimaryConstructor)
            printer.print(" constructor ")
            if (ktPrimaryConstructor.parameters.isNotEmpty()) {
                renderParameterList(ktPrimaryConstructor.parameters)
            } else {
                printer.print("()")
            }
        }

        override fun visitLambdaExpressionRaw(lambdaExpression: JKLambdaExpression) {
            val printLambda = {
                printer.par(JKPrinterBase.ParenthesisKind.CURVED) {
                    if (lambdaExpression.statement.statements.size > 1)
                        printer.println()
                    printer.renderList(lambdaExpression.parameters) {
                        it.accept(this)
                    }
                    if (lambdaExpression.parameters.isNotEmpty()) {
                        printer.print(" -> ")
                    }

                    val statement = lambdaExpression.statement
                    if (statement is JKBlockStatement) {
                        printer.renderList(statement.block.statements, { printer.println() }) { it.accept(this) }
                    } else {
                        statement.accept(this)
                    }
                    if (lambdaExpression.statement.statements.size > 1) {
                        printer.println()
                    }
                }
            }
            if (lambdaExpression.functionalType.present()) {
                printer.renderType(lambdaExpression.functionalType.type, lambdaExpression)
                printer.print(" ")
                printer.par(JKPrinterBase.ParenthesisKind.ROUND, printLambda)
            } else {
                printLambda()
            }
        }

        override fun visitBlockStatementRaw(blockStatement: JKBlockStatement) {
            blockStatement.block.accept(this)
        }

        override fun visitKtAssignmentStatementRaw(ktAssignmentStatement: JKKtAssignmentStatement) {
            ktAssignmentStatement.field.accept(this)
            printer.print(" ")
            printer.print(ktAssignmentStatement.token.text)
            printer.print(" ")
            ktAssignmentStatement.expression.accept(this)
        }

        override fun visitAssignmentChainAlsoLinkRaw(assignmentChainAlsoLink: JKAssignmentChainAlsoLink) {
            assignmentChainAlsoLink.receiver.accept(this)
            printer.print(".also({ ")
            assignmentChainAlsoLink.assignmentStatement.accept(this)
            printer.print(" })")
        }

        override fun visitAssignmentChainLetLinkRaw(assignmentChainLetLink: JKAssignmentChainLetLink) {
            assignmentChainLetLink.receiver.accept(this)
            printer.print(".let({ ")
            assignmentChainLetLink.assignmentStatement.accept(this)
            printer.print("; ")
            assignmentChainLetLink.field.accept(this)
            printer.print(" })")
        }

        override fun visitKtWhenStatementRaw(ktWhenStatement: JKKtWhenStatement) {
            printer.print("when(")
            ktWhenStatement.expression.accept(this)
            printer.print(")")
            printer.block() {
                printer.renderList(ktWhenStatement.cases, { printer.println() }) {
                    it.accept(this)
                }
            }
        }

        override fun visitAnnotationListRaw(annotationList: JKAnnotationList) {
            printer.renderList(annotationList.annotations, " ") {
                it.accept(this)
            }
        }

        override fun visitAnnotationRaw(annotation: JKAnnotation) {
            printer.print("@")
            printer.renderSymbol(annotation.classSymbol, annotation)
            if (annotation.arguments.isNotEmpty()) {
                printer.par {
                    printer.renderList(annotation.arguments) { it.accept(this) }
                }
            }
        }

        override fun visitAnnotationNameParameterRaw(annotationNameParameter: JKAnnotationNameParameter) {
            annotationNameParameter.name.accept(this)
            printer.print(" = ")
            annotationNameParameter.value.accept(this)
        }

        override fun visitAnnotationParameterRaw(annotationParameter: JKAnnotationParameter) {
            annotationParameter.value.accept(this)
        }

        override fun visitClassLiteralExpressionRaw(classLiteralExpression: JKClassLiteralExpression) {
            if (classLiteralExpression.literalType == JKClassLiteralExpression.ClassLiteralType.JAVA_VOID_TYPE) {
                printer.print("Void.TYPE")
            } else {
                printer.renderType(classLiteralExpression.classType.type, classLiteralExpression)
                printer.print("::")
                when (classLiteralExpression.literalType) {
                    JKClassLiteralExpression.ClassLiteralType.KOTLIN_CLASS -> printer.print("class")
                    JKClassLiteralExpression.ClassLiteralType.JAVA_CLASS -> printer.print("class.java")
                    JKClassLiteralExpression.ClassLiteralType.JAVA_PRIMITIVE_CLASS -> printer.print("class.javaPrimitiveType")
                    JKClassLiteralExpression.ClassLiteralType.JAVA_VOID_TYPE -> Unit
                }
            }
        }

        override fun visitKtWhenCaseRaw(ktWhenCase: JKKtWhenCase) {
            printer.renderList(ktWhenCase.labels) {
                it.accept(this)
            }
            printer.print(" -> ")
            ktWhenCase.statement.accept(this)
        }

        override fun visitKtElseWhenLabelRaw(ktElseWhenLabel: JKKtElseWhenLabel) {
            printer.print("else")
        }

        override fun visitKtValueWhenLabelRaw(ktValueWhenLabel: JKKtValueWhenLabel) {
            ktValueWhenLabel.expression.accept(this)
        }
    }
}
