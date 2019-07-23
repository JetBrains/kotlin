/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.conversions.parentOfType
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*
import org.jetbrains.kotlin.nj2k.tree.visitors.JKVisitorWithCommentsPrinting
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class NewCodeBuilder(context: NewJ2kConverterContext) {
    private val elementInfoStorage = context.elementsInfoStorage
    private val builder = StringBuilder()
    private val printer = JKPrinter(builder, elementInfoStorage)

    private fun classKindString(kind: JKClass.ClassKind): String = when (kind) {
        JKClass.ClassKind.ANNOTATION -> "annotation class"
        JKClass.ClassKind.CLASS -> "class"
        JKClass.ClassKind.ENUM -> "enum class"
        JKClass.ClassKind.INTERFACE -> "interface"
        JKClass.ClassKind.OBJECT -> "object"
        JKClass.ClassKind.COMPANION -> "companion object"
    }

    inner class Visitor : JKVisitorWithCommentsPrinting {
        private val printedTokens = mutableSetOf<JKNonCodeElement>()

        //TODO move to ast transformation phase
        private fun JKNonCodeElement.shouldBeDropped(): Boolean =
            this is JKCommentElement && text.startsWith("//noinspection")

        private fun JKNonCodeElement.createText() =
            if (this !in printedTokens) {
                printedTokens += this
                text
            } else ""


        private fun List<JKNonCodeElement>.createText(): String {
            val text = filterNot { it.shouldBeDropped() }.joinToString("") { token -> token.createText() }
            val needNewLine = text.lastIndexOf('\n') < text.lastIndexOf("//")
            return text + "\n".takeIf { needNewLine }.orEmpty()
        }

        private fun JKNonCodeElementsListOwner.needPreserveSpacesAfterLastCommit() =
            this is JKArgument
                    || this is JKParameter
                    || safeAs<JKTreeElement>()?.parent is JKArgument
                    || safeAs<JKTreeElement>()?.parent is JKBinaryExpression

        override fun printLeftNonCodeElements(element: JKNonCodeElementsListOwner) {
            val text = element.leftNonCodeElements
                .let {
                    if (element.needPreserveSpacesAfterLastCommit()) it
                    else it.dropWhile { it is JKSpaceElement }
                }.createText()
            printer.printWithNoIndent(text)
        }


        override fun printRightNonCodeElements(element: JKNonCodeElementsListOwner) {
            val text = element.rightNonCodeElements
                .let {
                    if (element.needPreserveSpacesAfterLastCommit()) it
                    else it.dropLastWhile { it is JKSpaceElement }
                }.createText()
            printer.printWithNoIndent(text)
        }

        private fun renderTokenElement(tokenElement: JKTokenElement) {
            printLeftNonCodeElements(tokenElement)
            printer.printWithNoIndent(tokenElement.text)
            printRightNonCodeElements(tokenElement)
        }

        override fun visitModifierElementRaw(modifierElement: JKModifierElement) {
            if (modifierElement.modifier != Modality.FINAL) {
                printer.printWithNoIndent(modifierElement.modifier.text)
            }
        }

        private fun renderExtraTypeParametersUpperBounds(typeParameterList: JKTypeParameterList) {
            val extraTypeBounds = typeParameterList.typeParameters
                .filter { it.upperBounds.size > 1 }
            if (extraTypeBounds.isNotEmpty()) {
                printer.printWithNoIndent(" where ")
                val typeParametersWithBoudnds =
                    extraTypeBounds.flatMap { typeParameter ->
                        typeParameter.upperBounds.map { bound ->
                            typeParameter.name to bound
                        }
                    }
                printer.renderList(typeParametersWithBoudnds) { (name, bound) ->
                    name.accept(this)
                    printer.printWithNoIndent(" : ")
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
            printer.printWithNoIndent("try ")
            ktTryExpression.tryBlock.accept(this)
            ktTryExpression.catchSections.forEach { it.accept(this) }
            if (ktTryExpression.finallyBlock != JKBodyStubImpl) {
                printer.printWithNoIndent("finally ")
                ktTryExpression.finallyBlock.accept(this)
            }
        }

        override fun visitKtTryCatchSectionRaw(ktTryCatchSection: JKKtTryCatchSection) {
            printer.printWithNoIndent("catch ")
            printer.par {
                ktTryCatchSection.parameter.accept(this)
            }
            ktTryCatchSection.block.accept(this)
        }

        override fun visitForInStatementRaw(forInStatement: JKForInStatement) {
            printer.printWithNoIndent("for (")
            forInStatement.declaration.accept(this)
            printer.printWithNoIndent(" in ")
            forInStatement.iterationExpression.accept(this)
            printer.printWithNoIndent(") ")
            if (forInStatement.body.isEmpty()) {
                printer.printWithNoIndent(";")
            } else {
                forInStatement.body.accept(this)
            }
        }

        override fun visitKtThrowExpressionRaw(ktThrowExpression: JKKtThrowExpression) {
            printer.printWithNoIndent("throw ")
            ktThrowExpression.exception.accept(this)
        }

        override fun visitDoWhileStatementRaw(doWhileStatement: JKDoWhileStatement) {
            printer.printWithNoIndent("do ")
            doWhileStatement.body.accept(this)
            printer.printWithNoIndent(" while (")
            doWhileStatement.condition.accept(this)
            printer.printWithNoIndent(")")
        }

        override fun visitClassAccessExpressionRaw(classAccessExpression: JKClassAccessExpression) {
            printer.renderClassSymbol(classAccessExpression.identifier, classAccessExpression)
        }

        override fun visitFileRaw(file: JKFile) {
            if (file.packageDeclaration.packageName.value.isNotEmpty()) {
                file.packageDeclaration.accept(this)
            }
            file.importList.accept(this)
            file.declarationList.forEach { it.accept(this) }
        }


        override fun visitPackageDeclarationRaw(packageDeclaration: JKPackageDeclaration) {
            printer.printWithNoIndent("package ")
            val packageNameEscaped =
                packageDeclaration.packageName.value.escapedAsQualifiedName()
            printer.printlnWithNoIndent(packageNameEscaped)
        }

        override fun visitImportListRaw(importList: JKImportList) {
            importList.imports.forEach { it.accept(this) }
        }

        override fun visitImportStatementRaw(importStatement: JKImportStatement) {
            printer.printWithNoIndent("import ")
            val importNameEscaped =
                importStatement.name.value.escapedAsQualifiedName()
            printer.printlnWithNoIndent(importNameEscaped)
        }

        override fun visitBreakStatementRaw(breakStatement: JKBreakStatement) {
            printer.printWithNoIndent("break")
        }

        override fun visitBreakWithLabelStatementRaw(breakWithLabelStatement: JKBreakWithLabelStatement) {
            printer.printWithNoIndent("break@")
            breakWithLabelStatement.label.accept(this)
        }

        private fun renderModifiersList(modifiersList: JKModifiersListOwner) {
            val hasOverrideModifier = modifiersList.safeAs<JKOtherModifiersOwner>()
                ?.otherModifierElements
                ?.any { it.otherModifier == OtherModifier.OVERRIDE } == true
            printer.renderList(modifiersList.modifierElements(), " ") { modifierElement ->
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
            }
        }

        override fun visitClassRaw(klass: JKClass) {
            klass.annotationList.accept(this)
            if (klass.annotationList.annotations.isNotEmpty()) {
                printer.println()
            }
            renderModifiersList(klass)
            builder.append(" ")
            printer.print(classKindString(klass.classKind))
            builder.append(" ")
            klass.name.accept(this)
            klass.typeParameterList.accept(this)
            printer.printWithNoIndent(" ")

            val primaryConstructor = klass.primaryConstructor()
            primaryConstructor?.accept(this)


            if (klass.inheritance.present()) {
                printer.printWithNoIndent(" : ")
                klass.inheritance.accept(this)
            }

            //TODO should it be here?
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
                        printer.printWithNoIndent("()")
                    }
                }
            }

            if (implementTypes.isNotEmpty() && extendTypes.size == 1) {
                printer.printWithNoIndent(", ")
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


        override fun visitKtPropertyRaw(ktProperty: JKKtProperty) {
            ktProperty.annotationList.accept(this)
            if (ktProperty.annotationList.annotations.isNotEmpty()) {
                printer.println()
            }
            renderModifiersList(ktProperty)

            printer.printWithNoIndent(" ")
            ktProperty.name.accept(this)
            if (ktProperty.type.present()) {
                printer.printWithNoIndent(":")
                ktProperty.type.accept(this)
            }
            if (ktProperty.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
                ktProperty.initializer.accept(this)
            }
            if (ktProperty.getter !is JKKtEmptyGetterOrSetter) {
                printer.printlnWithNoIndent()
                ktProperty.getter.accept(this)
            }
            if (ktProperty.setter !is JKKtEmptyGetterOrSetter) {
                printer.printlnWithNoIndent()
                ktProperty.setter.accept(this)
            }
        }

        override fun visitEnumConstantRaw(enumConstant: JKEnumConstant) {
            enumConstant.name.accept(this)
            if (enumConstant.arguments.arguments.isNotEmpty()) {
                printer.par {
                    enumConstant.arguments.accept(this)
                }
            }
            if (enumConstant.body !is JKEmptyClassBody) {
                enumConstant.body.accept(this)
            }
        }

        override fun visitKtInitDeclarationRaw(ktInitDeclaration: JKKtInitDeclaration) {
            if (ktInitDeclaration.block.statements.isNotEmpty()) {
                printer.println()
                printer.print("init ")
                ktInitDeclaration.block.accept(this)
            }
        }

        override fun visitKtIsExpressionRaw(ktIsExpression: JKKtIsExpression) {
            ktIsExpression.expression.accept(this)
            printer.printWithNoIndent(" is ")
            ktIsExpression.type.accept(this)
        }

        override fun visitParameterRaw(parameter: JKParameter) {
            renderModifiersList(parameter)
            printer.printWithNoIndent(" ")
            parameter.annotationList.accept(this)
            printer.printWithNoIndent(" ")
            if (parameter.isVarArgs) {
                printer.printWithNoIndent("vararg ")
            }
            if (parameter.parent is JKKtPrimaryConstructor
                && (parameter.parent?.parent?.parent as? JKClass)?.classKind == JKClass.ClassKind.ANNOTATION
            ) {//TODO get rid of
                printer.print(" val ")
            }
            parameter.name.accept(this)
            if (parameter.type.present() && parameter.type.type !is JKContextType) {
                printer.printWithNoIndent(":")
                parameter.type.accept(this)
            }
            if (parameter.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
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
                printer.printWithNoIndent(": ")
                forLoopVariable.type.accept(this)
            }
        }

        override fun visitKtFunctionRaw(ktFunction: JKKtFunction) {
            printer.printIndent()
            if (ktFunction.annotationList.annotations.isNotEmpty()) {
                ktFunction.annotationList.accept(this)
                printer.printlnWithNoIndent(" ")
            }
            renderModifiersList(ktFunction)
            printer.printWithNoIndent(" fun ")
            ktFunction.typeParameterList.accept(this)

            elementInfoStorage.getOrCreateInfoForElement(ktFunction).let {
                printer.printWithNoIndent(it.render())
            }
            ktFunction.name.accept(this)
            renderTokenElement(ktFunction.leftParen)
            printer.renderList(ktFunction.parameters) {
                it.accept(this)
            }
            renderTokenElement(ktFunction.rightParen)
            printer.printWithNoIndent(": ")
            ktFunction.returnType.accept(this)
            renderExtraTypeParametersUpperBounds(ktFunction.typeParameterList)
            ktFunction.block.accept(this)
        }

        override fun visitIfElseExpressionRaw(ifElseExpression: JKIfElseExpression) {
            printer.printWithNoIndent("if (")
            ifElseExpression.condition.accept(this)
            printer.printWithNoIndent(")")
            ifElseExpression.thenBranch.accept(this)
            printer.printWithNoIndent(" else ")
            ifElseExpression.elseBranch.accept(this)
        }

        override fun visitIfStatementRaw(ifStatement: JKIfStatement) {
            printer.printWithNoIndent("if (")
            ifStatement.condition.accept(this)
            printer.printWithNoIndent(")")
            if (ifStatement.thenBranch.isEmpty()) {
                printer.printWithNoIndent(";")
            } else {
                ifStatement.thenBranch.accept(this)
            }
        }

        override fun visitIfElseStatementRaw(ifElseStatement: JKIfElseStatement) {
            visitIfStatement(ifElseStatement)
            printer.printWithNoIndent(" else ")
            ifElseStatement.elseBranch.accept(this)
        }

        override fun visitKtGetterOrSetterRaw(ktGetterOrSetter: JKKtGetterOrSetter) {
            printer.indented {
                renderModifiersList(ktGetterOrSetter)
                printer.printWithNoIndent(" ")
                when (ktGetterOrSetter.kind) {
                    JKKtGetterOrSetter.Kind.GETTER -> printer.printWithNoIndent("get")
                    JKKtGetterOrSetter.Kind.SETTER -> printer.printWithNoIndent("set")
                }
                if (!ktGetterOrSetter.body.isEmpty()) {
                    when (ktGetterOrSetter.kind) {
                        JKKtGetterOrSetter.Kind.GETTER -> printer.printWithNoIndent("() ")
                        JKKtGetterOrSetter.Kind.SETTER -> printer.printWithNoIndent("(value) ")
                    }
                    ktGetterOrSetter.body.accept(this)
                }
            }
            printer.printlnWithNoIndent()
        }

        override fun visitKtEmptyGetterOrSetterRaw(ktEmptyGetterOrSetter: JKKtEmptyGetterOrSetter) {

        }

        override fun visitBinaryExpressionRaw(binaryExpression: JKBinaryExpression) {
            binaryExpression.left.accept(this)
            printer.printWithNoIndent(" ")
            printer.printWithNoIndent(binaryExpression.operator.token.text)
            printer.printWithNoIndent(" ")
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
                printer.printWithNoIndent(" : ")
                typeParameter.upperBounds.single().accept(this)
            }
        }

        override fun visitLiteralExpressionRaw(literalExpression: JKLiteralExpression) {
            printer.printWithNoIndent(literalExpression.literal)
        }

        override fun visitPrefixExpressionRaw(prefixExpression: JKPrefixExpression) {
            printer.printWithNoIndent(prefixExpression.operator.token.text)
            prefixExpression.expression.accept(this)
        }

        override fun visitThisExpressionRaw(thisExpression: JKThisExpression) {
            printer.printWithNoIndent("this")
            thisExpression.qualifierLabel.accept(this)
        }

        override fun visitSuperExpressionRaw(superExpression: JKSuperExpression) {
            printer.printWithNoIndent("super")
            superExpression.qualifierLabel.accept(this)
        }

        override fun visitContinueStatementRaw(continueStatement: JKContinueStatement) {
            printer.printWithNoIndent("continue")
            continueStatement.label.accept(this)
            printer.printWithNoIndent(" ")
        }

        override fun visitLabelEmptyRaw(labelEmpty: JKLabelEmpty) {

        }

        override fun visitLabelTextRaw(labelText: JKLabelText) {
            printer.printWithNoIndent("@")
            labelText.label.accept(this)
            printer.printWithNoIndent(" ")
        }

        override fun visitLabeledStatementRaw(labeledStatement: JKLabeledStatement) {
            for (label in labeledStatement.labels) {
                label.accept(this)
                printer.printWithNoIndent("@")
            }
            labeledStatement.statement.accept(this)
        }

        override fun visitNameIdentifierRaw(nameIdentifier: JKNameIdentifier) {
            printer.printWithNoIndent(nameIdentifier.value.escaped())
        }

        override fun visitPostfixExpressionRaw(postfixExpression: JKPostfixExpression) {
            postfixExpression.expression.accept(this)
            printer.printWithNoIndent(postfixExpression.operator.token.text)
        }

        override fun visitQualifiedExpressionRaw(qualifiedExpression: JKQualifiedExpression) {
            qualifiedExpression.receiver.accept(this)
            printer.printWithNoIndent(
                when (qualifiedExpression.operator) {
                    is JKJavaQualifierImpl.DOT /*<-remove this TODO!*/, is JKKtQualifierImpl.DOT -> "."
                    is JKKtQualifierImpl.SAFE -> "?."
                    else -> TODO()
                }
            )
            qualifiedExpression.selector.accept(this)
        }

        override fun visitExpressionListRaw(expressionList: JKExpressionList) {
            printer.renderList(expressionList.expressions) { it.accept(this) }
        }


        override fun visitArgumentListRaw(argumentList: JKArgumentList) {
            printer.renderList(argumentList.arguments) { it.accept(this) }
        }

        override fun visitArgumentRaw(argument: JKArgument) {
            argument.value.accept(this)
        }

        override fun visitNamedArgumentRaw(namedArgument: JKNamedArgument) {
            namedArgument.name.accept(this)
            printer.printWithNoIndent(" = ")
            namedArgument.value.accept(this)
        }

        override fun visitMethodCallExpressionRaw(methodCallExpression: JKMethodCallExpression) {
            printer.printWithNoIndent(FqName(methodCallExpression.identifier.fqName).shortName().asString().escaped())
            methodCallExpression.typeArgumentList.accept(this)
            printer.par {
                methodCallExpression.arguments.accept(this)
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
            printer.printWithNoIndent(" as ")
            typeCastExpression.type.accept(this)
        }

        override fun visitWhileStatementRaw(whileStatement: JKWhileStatement) {
            printer.print("while(")
            whileStatement.condition.accept(this)
            printer.printWithNoIndent(")")
            if (whileStatement.body.isEmpty()) {
                printer.printWithNoIndent(";")
            } else {
                whileStatement.body.accept(this)
            }
        }

        override fun visitLocalVariableRaw(localVariable: JKLocalVariable) {
            printer.printWithNoIndent(" ")
            localVariable.annotationList.accept(this)
            printer.printWithNoIndent(" ")
            renderModifiersList(localVariable)
            printer.printWithNoIndent(" ")
            localVariable.name.accept(this)
            if (localVariable.type.present() && localVariable.type.type != JKContextType) {
                printer.printWithNoIndent(": ")
                localVariable.type.accept(this)
            }
            if (localVariable.initializer !is JKStubExpression) {
                printer.printWithNoIndent(" = ")
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
            printer.printlnWithNoIndent()
            ktConvertedFromForLoopSyntheticWhileStatement.whileStatement.accept(this)
        }

        override fun visitJavaNewExpressionRaw(javaNewExpression: JKJavaNewExpression) {
            if (javaNewExpression.isAnonymousClass()) {
                printer.printWithNoIndent("object : ")
            }
            printer.renderClassSymbol(javaNewExpression.classSymbol, javaNewExpression)
            javaNewExpression.typeArgumentList.accept(this)
            if (!javaNewExpression.classSymbol.isInterface() || javaNewExpression.arguments.arguments.isNotEmpty()) {
                printer.par(ParenthesisKind.ROUND) {
                    javaNewExpression.arguments.accept(this)
                }
            }
            if (javaNewExpression.isAnonymousClass()) {
                javaNewExpression.classBody.accept(this)
            }
        }


        override fun visitClassBodyRaw(classBody: JKClassBody) {
            val declarationsToPrint = classBody.declarations.filterNot { it is JKKtPrimaryConstructor }
            renderTokenElement(classBody.leftBrace)
            val enumConstants = declarationsToPrint.filterIsInstance<JKEnumConstant>()
            val otherDeclarations = declarationsToPrint.filterNot { it is JKEnumConstant }
            renderEnumConstants(enumConstants)
            if ((classBody.parent as? JKClass)?.classKind == JKClass.ClassKind.ENUM
                && otherDeclarations.isNotEmpty()
            ) {
                printer.printlnWithNoIndent(";")
            }
            if (enumConstants.isNotEmpty() && otherDeclarations.isNotEmpty()) {
                printer.println()
            }
            renderNonEnumClassDeclarations(otherDeclarations)
            renderTokenElement(classBody.rightBrace)
        }

        override fun visitEmptyClassBodyRaw(emptyClassBody: JKEmptyClassBody) {}

        override fun visitTypeElementRaw(typeElement: JKTypeElement) {
            printer.renderType(typeElement.type, typeElement)
        }

        override fun visitBlockRaw(block: JKBlock) {
            renderTokenElement(block.leftBrace)
            printer.renderList(block.statements, { printer.println() }) {
                it.accept(this)
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
            printer.printWithNoIndent(fieldAccessExpression.identifier.name.escaped())
        }

        override fun visitArrayAccessExpressionRaw(arrayAccessExpression: JKArrayAccessExpression) {
            arrayAccessExpression.expression.accept(this)
            printer.par(ParenthesisKind.SQUARE) { arrayAccessExpression.indexExpression.accept(this) }
        }

        override fun visitPackageAccessExpressionRaw(packageAccessExpression: JKPackageAccessExpression) {
            printer.printWithNoIndent(packageAccessExpression.identifier.name.escaped())
        }

        override fun visitMethodReferenceExpressionRaw(methodReferenceExpression: JKMethodReferenceExpression) {
            methodReferenceExpression.qualifier.accept(this)
            printer.printWithNoIndent("::")
            val needFqName = methodReferenceExpression.qualifier is JKStubExpression
            val displayName =
                if (needFqName) methodReferenceExpression.identifier.getDisplayName()
                else methodReferenceExpression.identifier.name

            printer.printWithNoIndent(displayName.escapedAsQualifiedName())

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

        override fun visitKtConstructorRaw(ktConstructor: JKKtConstructor) {
            ktConstructor.annotationList.accept(this)
            if (ktConstructor.annotationList.annotations.isNotEmpty()) {
                printer.println()
            }
            renderModifiersList(ktConstructor)
            printer.print(" constructor")
            renderParameterList(ktConstructor.parameters)
            if (ktConstructor.delegationCall !is JKStubExpression) {
                builder.append(" : ")
                ktConstructor.delegationCall.accept(this)
            }
            ktConstructor.block.accept(this)
        }

        override fun visitKtPrimaryConstructorRaw(ktPrimaryConstructor: JKKtPrimaryConstructor) {
            ktPrimaryConstructor.annotationList.accept(this)
            printer.printWithNoIndent(" ")
            renderModifiersList(ktPrimaryConstructor)
            printer.printWithNoIndent(" constructor ")
            if (ktPrimaryConstructor.parameters.isNotEmpty()) {
                renderParameterList(ktPrimaryConstructor.parameters)
            } else {
                printer.print("()")
            }
        }

        override fun visitBodyStub(bodyStub: JKBodyStub) {
        }

        override fun visitLambdaExpressionRaw(lambdaExpression: JKLambdaExpression) {
            val printLambda = {
                printer.par(ParenthesisKind.CURVED) {
                    if (lambdaExpression.statement.statements.size > 1)
                        printer.println()
                    lambdaExpression.parameters.firstOrNull()?.accept(this)
                    lambdaExpression.parameters.asSequence().drop(1).forEach { printer.printWithNoIndent(", "); it.accept(this) }
                    if (lambdaExpression.parameters.isNotEmpty()) {
                        printer.printWithNoIndent(" -> ")
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
                printer.printWithNoIndent(" ")
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
            printer.printWithNoIndent(" ")
            printer.printWithNoIndent(ktAssignmentStatement.operator.token.text)
            printer.printWithNoIndent(" ")
            ktAssignmentStatement.expression.accept(this)
        }

        override fun visitKtWhenStatementRaw(ktWhenStatement: JKKtWhenStatement) {
            printer.printWithNoIndent("when(")
            ktWhenStatement.expression.accept(this)
            printer.printWithNoIndent(")")
            printer.block(multiline = true) {
                printer.renderList(ktWhenStatement.cases, { printer.printlnWithNoIndent() }) {
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
            printer.printWithNoIndent("@")
            printer.printWithNoIndent(annotation.classSymbol.fqName.escapedAsQualifiedName())
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
            if (classLiteralExpression.literalType == JKClassLiteralExpression.LiteralType.JAVA_VOID_TYPE) {
                printer.printWithNoIndent("Void.TYPE")
            } else {
                printer.renderType(classLiteralExpression.classType.type, classLiteralExpression)
                printer.printWithNoIndent("::")
                when (classLiteralExpression.literalType) {
                    JKClassLiteralExpression.LiteralType.KOTLIN_CLASS -> printer.printWithNoIndent("class")
                    JKClassLiteralExpression.LiteralType.JAVA_CLASS -> printer.printWithNoIndent("class.java")
                    JKClassLiteralExpression.LiteralType.JAVA_PRIMITIVE_CLASS -> printer.printWithNoIndent("class.javaPrimitiveType")
                    JKClassLiteralExpression.LiteralType.JAVA_VOID_TYPE -> {
                    }
                }
            }
        }

        override fun visitKtWhenCaseRaw(ktWhenCase: JKKtWhenCase) {
            printer.renderList(ktWhenCase.labels, ", ") {
                it.accept(this)
            }
            printer.printWithNoIndent(" -> ")
            ktWhenCase.statement.accept(this)
        }

        override fun visitKtElseWhenLabelRaw(ktElseWhenLabel: JKKtElseWhenLabel) {
            printer.printWithNoIndent("else")
        }

        override fun visitKtValueWhenLabelRaw(ktValueWhenLabel: JKKtValueWhenLabel) {
            ktValueWhenLabel.expression.accept(this)
        }
    }


    fun printCodeOut(root: JKTreeElement): String {
        Visitor().also { root.accept(it) }
        return builder.toString().replace("\r\n", "\n")
    }
}

enum class ParenthesisKind(val open: String, val close: String) {
    ROUND("(", ")"),
    SQUARE("[", "]"),
    CURVED("{", "}"),
    CURVED_MULTILINE("{\n", "}\n"),
    INLINE_COMMENT("/*", "*/"),
    ANGLE("<", ">")
}

private class JKPrinter(
    stringBuilder: StringBuilder,
    private val elementInfoStorage: JKElementInfoStorage
) : Printer(stringBuilder) {

    inline fun indented(block: () -> Unit) {
        this.pushIndent()
        block()
        this.popIndent()
    }

    inline fun block(multiline: Boolean = false, crossinline body: () -> Unit) {
        par(ParenthesisKind.CURVED) {
            indented(body)
        }
    }

    inline fun par(kind: ParenthesisKind = ParenthesisKind.ROUND, body: () -> Unit) {
        printWithNoIndent(kind.open)
        body()
        printWithNoIndent(kind.close)
    }

    private fun JKClassSymbol.needFqName(): Boolean =
        fqName !in mappedToKotlinFqNames


    fun renderType(type: JKType, owner: JKTreeElement?) {
        if (type is JKNoTypeImpl) return
        elementInfoStorage.getOrCreateInfoForElement(type).let {
            print(it.render())
        }
        when (type) {
            is JKClassType -> {
                renderClassSymbol(type.classReference, owner)
            }
            is JKContextType -> return
            is JKStarProjectionType ->
                printWithNoIndent("*")
            is JKTypeParameterType ->
                printWithNoIndent(type.name)
            is JKVarianceTypeParameterType -> {
                when (type.variance) {
                    JKVarianceTypeParameterType.Variance.IN -> printWithNoIndent("in ")
                    JKVarianceTypeParameterType.Variance.OUT -> printWithNoIndent("out ")
                }
                renderType(type.boundType, null)
            }
            else -> printWithNoIndent("Unit /* TODO: ${type::class} */")
        }
        if (type is JKParametrizedType && type.parameters.isNotEmpty()) {
            par(ParenthesisKind.ANGLE) {
                renderList(type.parameters, renderElement = { renderType(it, null) })
            }
        }
        if (type.nullability == Nullability.Nullable) {
            printWithNoIndent("?")
        }
    }

    fun renderClassSymbol(classSymbol: JKClassSymbol, owner: JKTreeElement?) {
        val needFqName = classSymbol.needFqName() && owner?.isSelectorOfQualifiedExpression() != true
        val displayName = if (needFqName) classSymbol.getDisplayName() else classSymbol.name
        printWithNoIndent(displayName.escapedAsQualifiedName())
    }

    inline fun <T> renderList(list: List<T>, separator: String = ", ", renderElement: (T) -> Unit) =
        renderList(list, { printWithNoIndent(separator) }, renderElement)

    inline fun <T> renderList(list: List<T>, separator: () -> Unit, renderElement: (T) -> Unit) {
        val (head, tail) = list.headTail()
        head?.let(renderElement) ?: return
        tail?.forEach {
            separator()
            renderElement(it)
        }
    }

    private fun JKTreeElement.isSelectorOfQualifiedExpression() =
        parent?.safeAs<JKQualifiedExpression>()?.selector == this
}


private fun String.escapedAsQualifiedName(): String =
    split('.')
        .map { it.escaped() }
        .joinToString(".") { it }

private fun <T> List<T>.headTail(): Pair<T?, List<T>?> {
    val head = this.firstOrNull()
    val tail = if (size <= 1) null else subList(1, size)
    return head to tail
}


private val mappedToKotlinFqNames =
    setOf(
        "java.util.ArrayList",
        "java.util.LinkedHashMap",
        "java.util.HashMap",
        "java.util.LinkedHashSet",
        "java.util.HashSet"
    )

