/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.symbols.JKSymbol
import org.jetbrains.kotlin.nj2k.symbols.getDisplayFqName
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitorWithCommentsPrinting
import org.jetbrains.kotlin.nj2k.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class NewCodeBuilder(context: NewJ2kConverterContext) {
    private val elementInfoStorage = context.elementsInfoStorage
    private val printer = JKPrinter(context.project, context.importStorage, elementInfoStorage)

    private fun classKindString(kind: JKClass.ClassKind): String = when (kind) {
        JKClass.ClassKind.ANNOTATION -> "annotation class"
        JKClass.ClassKind.CLASS -> "class"
        JKClass.ClassKind.ENUM -> "enum class"
        JKClass.ClassKind.INTERFACE -> "interface"
        JKClass.ClassKind.OBJECT -> "object"
        JKClass.ClassKind.COMPANION -> "companion object"
    }

    inner class Visitor : JKVisitorWithCommentsPrinting() {
        private val printedTokens = mutableSetOf<JKComment>()

        //TODO move to ast transformation phase
        private fun JKComment.shouldBeDropped(): Boolean =
            text.startsWith("//noinspection")

        private fun JKComment.createText() =
            if (this !in printedTokens) {
                printedTokens += this
                text
            } else null


        private fun List<JKComment>.createText(): String = buildString {
            var needNewLine = false
            for (comment in this@createText) {
                if (comment.shouldBeDropped()) continue
                val text = comment.createText() ?: continue
                if (needNewLine) appendln() else append(' ')
                append(text)
                needNewLine = text.startsWith("//") || '\n' in text
            }
        }

        private fun String.hasNoLineBreakAfterSingleLineComment() = lastIndexOf('\n') < lastIndexOf("//")

        override fun printLeftNonCodeElements(element: JKFormattingOwner) {
            val text = element.trailingComments.createText()
            printer.print(text)

            val addNewLine = element.hasTrailingLineBreak
                    || element is JKDeclaration && element.trailingComments.isNotEmpty() // add new line between comment & declaration
                    || text.hasNoLineBreakAfterSingleLineComment()

            if (addNewLine) printer.println()
        }


        override fun printRightNonCodeElements(element: JKFormattingOwner) {
            val text = element.leadingComments.createText()
            printer.print(text)

            val addNewLine = element.hasLeadingLineBreak || text.hasNoLineBreakAfterSingleLineComment()
            if (addNewLine) printer.println()
        }

        private fun renderTokenElement(tokenElement: JKTokenElement) {
            printLeftNonCodeElements(tokenElement)
            printer.print(tokenElement.text)
            printRightNonCodeElements(tokenElement)
        }

        override fun visitModifierElementRaw(modifierElement: JKModifierElement) {
            if (modifierElement.modifier != Modality.FINAL) {
                printer.print(modifierElement.modifier.text)
            }
        }

        private fun renderExtraTypeParametersUpperBounds(typeParameterList: JKTypeParameterList) {
            val extraTypeBounds = typeParameterList.typeParameters
                .filter { it.upperBounds.size > 1 }
            if (extraTypeBounds.isNotEmpty()) {
                printer.print(" where ")
                val typeParametersWithBoudnds =
                    extraTypeBounds.flatMap { typeParameter ->
                        typeParameter.upperBounds.map { bound ->
                            typeParameter.name to bound
                        }
                    }
                printer.renderList(typeParametersWithBoudnds) { (name, bound) ->
                    name.accept(this)
                    printer.print(" : ")
                    bound.accept(this)
                }
            }
        }

        override fun visitTreeElementRaw(treeElement: JKTreeElement) {
            printer.print("/* !!! Hit visitElement for element type: ${treeElement::class} !!! */")
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


        private fun renderModifiersList(modifiersList: JKModifiersListOwner) {
            val hasOverrideModifier = modifiersList.safeAs<JKOtherModifiersOwner>()
                ?.otherModifierElements
                ?.any { it.otherModifier == OtherModifier.OVERRIDE } == true
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

        override fun visitClassRaw(klass: JKClass) {
            klass.annotationList.accept(this)
            if (klass.annotationList.annotations.isNotEmpty()) {
                printer.println()
            }
            renderModifiersList(klass)
            printer.print(" ")
            printer.print(classKindString(klass.classKind))
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

        override fun visitKtAnnotationArrayInitializerExpressionRaw(ktAnnotationArrayInitializerExpression: JKKtAnnotationArrayInitializerExpression) {
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
                printer.par(ParenthesisKind.ANGLE) {
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

        override fun visitLabelEmptyRaw(labelEmpty: JKLabelEmpty) {

        }

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
                printer.par(ParenthesisKind.ANGLE) {
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

        override fun visitEmptyStatementRaw(emptyStatement: JKEmptyStatement) {
        }

        override fun visitStubExpressionRaw(stubExpression: JKStubExpression) {
        }

        override fun visitKtConvertedFromForLoopSyntheticWhileStatementRaw(
            ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement
        ) {
            ktConvertedFromForLoopSyntheticWhileStatement.variableDeclaration.accept(this)
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
                printer.par(ParenthesisKind.ROUND) {
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
            printer.par(ParenthesisKind.ROUND) {
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
                printer.par(ParenthesisKind.CURVED) {
                    if (lambdaExpression.statement.statements.size > 1)
                        printer.println()
                    lambdaExpression.parameters.firstOrNull()?.accept(this)
                    lambdaExpression.parameters.asSequence().drop(1).forEach { printer.print(", "); it.accept(this) }
                    if (lambdaExpression.parameters.isNotEmpty()) {
                        printer.print(" -> ")
                    }

                    val statement = lambdaExpression.statement
                    if (statement is JKBlockStatement) {
                        printer.renderList(statement.block.statements, { printer.println() }) { it.accept(this) }
                    } else {
                        statement.accept(this)
                    }
                    if (lambdaExpression.statement.statements.size > 1)
                        printer.println()
                }
            }
            if (lambdaExpression.functionalType.present()) {
                printer.renderType(lambdaExpression.functionalType.type, lambdaExpression)
                printer.print(" ")
                printer.par(ParenthesisKind.ROUND, printLambda)
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
                    JKClassLiteralExpression.ClassLiteralType.JAVA_VOID_TYPE -> {
                    }
                }
            }
        }

        override fun visitKtWhenCaseRaw(ktWhenCase: JKKtWhenCase) {
            printer.renderList(ktWhenCase.labels, ", ") {
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


    fun printCodeOut(root: JKTreeElement): String {
        Visitor().also { root.accept(it) }
        return printer.toString().replace("\r\n", "\n")
    }
}

enum class ParenthesisKind(val open: String, val close: String) {
    ROUND("(", ")"),
    CURVED("{", "}"),
    ANGLE("<", ">")
}


private class JKPrinter(
    project: Project,
    importStorage: JKImportStorage,
    private val elementInfoStorage: JKElementInfoStorage
) {
    val symbolRenderer = JKSymbolRenderer(importStorage, project)
    private val stringBuilder: StringBuilder = StringBuilder()
    private var currentIndent = 0;
    private val indentSymbol = " ".repeat(4)

    private var lastSymbolIsLineBreak = false

    override fun toString(): String = stringBuilder.toString()

    fun print(value: String) {
        if (value.isNotEmpty()) {
            lastSymbolIsLineBreak = false
        }
        stringBuilder.append(value)
    }

    fun println() {
        if (lastSymbolIsLineBreak) return
        stringBuilder.append('\n')
        repeat(currentIndent) {
            stringBuilder.append(indentSymbol)
        }
        lastSymbolIsLineBreak = true
    }


    inline fun indented(block: () -> Unit) {
        currentIndent++
        block()
        currentIndent--
    }

    inline fun block(crossinline body: () -> Unit) {
        par(ParenthesisKind.CURVED) {
            indented(body)
        }
    }

    inline fun par(kind: ParenthesisKind = ParenthesisKind.ROUND, body: () -> Unit) {
        print(kind.open)
        body()
        print(kind.close)
    }


    private fun JKType.renderTypeInfo() {
        this@JKPrinter.print(elementInfoStorage.getOrCreateInfoForElement(this).render())
    }

    fun renderType(type: JKType, owner: JKTreeElement?) {
        if (type is JKNoType) return
        if (type is JKCapturedType) {
            when (val wildcard = type.wildcardType) {
                is JKVarianceTypeParameterType -> {
                    renderType(wildcard.boundType, owner)
                }
                is JKStarProjectionType -> {
                    type.renderTypeInfo()
                    this.print("Any?")
                }
            }
            return
        }
        type.renderTypeInfo()
        when (type) {
            is JKClassType -> {
                renderSymbol(type.classReference, owner)
            }
            is JKContextType -> return
            is JKStarProjectionType ->
                this.print("*")
            is JKTypeParameterType ->
                this.print(type.identifier.name)
            is JKVarianceTypeParameterType -> {
                when (type.variance) {
                    JKVarianceTypeParameterType.Variance.IN -> this.print("in ")
                    JKVarianceTypeParameterType.Variance.OUT -> this.print("out ")
                }
                renderType(type.boundType, null)
            }
            else -> this.print("Unit /* TODO: ${type::class} */")
        }
        if (type is JKParametrizedType && type.parameters.isNotEmpty()) {
            par(ParenthesisKind.ANGLE) {
                renderList(type.parameters, renderElement = { renderType(it, null) })
            }
        }
        // we print undefined types as nullable because we need smartcast to work in nullability inference in post-processing
        if (type !is JKWildCardType
            && (type.nullability == Nullability.Default
                    && owner?.safeAs<JKLambdaExpression>()?.functionalType?.type != type
                    || type.nullability == Nullability.Nullable)
        ) {
            this.print("?")
        }
    }

    fun renderSymbol(symbol: JKSymbol, owner: JKTreeElement?) {
        print(symbolRenderer.renderSymbol(symbol, owner))
    }

    inline fun <T> renderList(list: List<T>, separator: String = ", ", renderElement: (T) -> Unit) =
        renderList(list, { this.print(separator) }, renderElement)

    inline fun <T> renderList(list: List<T>, separator: () -> Unit, renderElement: (T) -> Unit) {
        val (head, tail) = list.headTail()
        head?.let(renderElement) ?: return
        tail?.forEach {
            separator()
            renderElement(it)
        }
    }
}


fun String.escapedAsQualifiedName(): String =
    split('.')
        .map { it.escaped() }
        .joinToString(".") { it }

private fun <T> List<T>.headTail(): Pair<T?, List<T>?> {
    val head = this.firstOrNull()
    val tail = if (size <= 1) null else subList(1, size)
    return head to tail
}
