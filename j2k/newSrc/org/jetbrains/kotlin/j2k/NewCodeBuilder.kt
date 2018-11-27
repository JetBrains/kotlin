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

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.j2k.NewCodeBuilder.ParenthesisKind.*
import org.jetbrains.kotlin.j2k.ast.Mutability
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.j2k.tree.visitors.JKVisitorVoid
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class NewCodeBuilder {

    val builder = StringBuilder()
    val printer = Printer(builder)

    private fun classKindString(kind: JKClass.ClassKind): String = when (kind) {
        JKClass.ClassKind.ANNOTATION -> "annotation class"
        JKClass.ClassKind.CLASS -> "class"
        JKClass.ClassKind.ENUM -> "enum class"
        JKClass.ClassKind.INTERFACE -> "interface"
        JKClass.ClassKind.OBJECT -> "object"
        JKClass.ClassKind.COMPANION -> "companion object"
    }

    inner class Visitor : JKVisitorVoid {
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
                renderList(typeParametersWithBoudnds) { (name, bound) ->
                    name.accept(this)
                    printer.printWithNoIndent(" : ")
                    bound.accept(this)
                }
            }
        }

        override fun visitTreeElement(treeElement: JKTreeElement) {
            printer.print("/* !!! Hit visitElement for element type: ${treeElement::class} !!! */")
        }

        override fun visitKtTryExpression(ktTryExpression: JKKtTryExpression) {
            printer.printWithNoIndent("try ")
            if (ktTryExpression.tryBlock != JKBodyStub) {
                printer.block { ktTryExpression.tryBlock.accept(this) }
            }
            ktTryExpression.catchSections.forEach { it.accept(this) }
            if (ktTryExpression.finallyBlock != JKBodyStub) {
                printer.printWithNoIndent("finally ")
                printer.block { ktTryExpression.finallyBlock.accept(this) }
            }
        }

        override fun visitKtTryCatchSection(ktTryCatchSection: JKKtTryCatchSection) {
            printer.printWithNoIndent("catch ")
            printer.par {
                ktTryCatchSection.parameter.accept(this)
            }
            if (ktTryCatchSection.block != JKBodyStub) {
                printer.block { ktTryCatchSection.block.accept(this) }
            }
        }

        override fun visitForInStatement(forInStatement: JKForInStatement) {
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

        override fun visitKtThrowExpression(ktThrowExpression: JKKtThrowExpression) {
            printer.printWithNoIndent("throw ")
            ktThrowExpression.exception.accept(this)
        }

        override fun visitDoWhileStatement(doWhileStatement: JKDoWhileStatement) {
            printer.printWithNoIndent("do ")
            doWhileStatement.body.accept(this)
            printer.printWithNoIndent(" while (")
            doWhileStatement.condition.accept(this)
            printer.printWithNoIndent(")")
        }

        override fun visitClassAccessExpression(classAccessExpression: JKClassAccessExpression) {
            //TODO handle class Access in seperate conversion?
            printer.printWithNoIndent(classAccessExpression.identifier.fqName!!.substringAfter("java.lang."))
        }

        override fun visitFile(file: JKFile) {
            if (file.packageDeclaration.packageName.value.isNotEmpty()) {
                file.packageDeclaration.accept(this)
            }
            file.importList.forEach { it.accept(this) }
            file.declarationList.forEach { it.accept(this) }
        }

        private fun String.escapedAsQualifiedName(): String =
            split('.')
                .map { it.escaped() }
                .joinToString(".") { it }

        override fun visitPackageDeclaration(packageDeclaration: JKPackageDeclaration) {
            printer.printWithNoIndent("package ")
            val packageNameEscaped =
                packageDeclaration.packageName.value.escapedAsQualifiedName()
            printer.printlnWithNoIndent(packageNameEscaped)
        }

        override fun visitImportStatement(importStatement: JKImportStatement) {
            printer.printWithNoIndent("import ")
            val importNameEscaped =
                importStatement.name.value.escapedAsQualifiedName()
            printer.printlnWithNoIndent(importNameEscaped)
        }

        override fun visitBreakStatement(breakStatement: JKBreakStatement) {
            printer.printWithNoIndent("break")
        }

        override fun visitBreakWithLabelStatement(breakWithLabelStatement: JKBreakWithLabelStatement) {
            printer.printWithNoIndent("break@")
            breakWithLabelStatement.label.accept(this)
        }

        private fun renderModifiersList(modifiersList: JKModifiersListOwner) {
            renderList(modifiersList.modifiers(), " ") {
                printer.printWithNoIndent(it.text)
            }
        }

        private inline fun <T> renderList(list: List<T>, separator: String = ", ", renderElement: (T) -> Unit) =
            renderList(list, { printer.printWithNoIndent(separator) }, renderElement)

        private inline fun <T> renderList(list: List<T>, separator: () -> Unit, renderElement: (T) -> Unit) {
            val (head, tail) = list.headTail()
            head?.let(renderElement) ?: return
            tail?.forEach {
                separator()
                renderElement(it)
            }
        }

        override fun visitClass(klass: JKClass) {
            renderModifiersList(klass)
            builder.append(" ")
            printer.print(classKindString(klass.classKind))
            builder.append(" ")
            klass.name.accept(this)
            klass.typeParameterList.accept(this)
            printer.printWithNoIndent(" ")

            val primaryConstructor = klass.primaryConstructor()
            primaryConstructor?.accept(this)

            if (klass.inheritance.inherit.isNotEmpty()) {
                printer.printWithNoIndent(" : ")

                val delegationCall = primaryConstructor?.delegationCall as? JKDelegationConstructorCall
                renderList(klass.inheritance.inherit) {
                    it.accept(this)
                    if (delegationCall != null && delegationCall.isCallOfConstructorOf(it.type)) {
                        printer.par {
                            delegationCall.arguments.accept(this)
                        }
                    }
                }
            }

            //TODO should it be here?
            renderExtraTypeParametersUpperBounds(klass.typeParameterList)

            klass.classBody.accept(this)
        }

        private fun renderEnumConstants(enumConstants: List<JKEnumConstant>) {
            renderList(enumConstants) {
                it.accept(this)
            }

        }

        private fun renderNonEnumClassDeclarations(declarations: List<JKDeclaration>) {
            renderList(declarations, { printer.println() }) {
                it.accept(this)
            }
        }


        override fun visitKtProperty(ktProperty: JKKtProperty) {
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

        override fun visitEnumConstant(enumConstant: JKEnumConstant) {
            enumConstant.name.accept(this)
            if (enumConstant.arguments.expressions.isNotEmpty()) {
                printer.par {
                    enumConstant.arguments.accept(this)
                }
            }
        }

        override fun visitKtInitDeclaration(ktInitDeclaration: JKKtInitDeclaration) {
            if (ktInitDeclaration.block.statements.isNotEmpty()) {
                printer.print("init ")
                printer.block(multiline = true) {
                    ktInitDeclaration.block.accept(this)
                }
            }
        }

        override fun visitKtIsExpression(ktIsExpression: JKKtIsExpression) {
            ktIsExpression.expression.accept(this)
            printer.printWithNoIndent(" is ")
            ktIsExpression.type.accept(this)
        }

        override fun visitParameter(parameter: JKParameter) {
            renderModifiersList(parameter)
            printer.printWithNoIndent(" ")
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

        override fun visitForLoopVariable(forLoopVariable: JKForLoopVariable) {
            forLoopVariable.name.accept(this)
            if (forLoopVariable.type.present() && forLoopVariable.type.type !is JKContextType) {
                printer.printWithNoIndent(": ")
                forLoopVariable.type.accept(this)
            }
        }

        override fun visitKtFunction(ktFunction: JKKtFunction) {
            printer.printIndent()
            if (ktFunction.annotationList.annotations.isNotEmpty()) {
                ktFunction.annotationList.accept(this)
                printer.printlnWithNoIndent(" ")
            }
            renderModifiersList(ktFunction)
            printer.printWithNoIndent(" fun ")
            ktFunction.typeParameterList.accept(this)
            ktFunction.name.accept(this)
            printer.printWithNoIndent("(")
            renderList(ktFunction.parameters) {
                it.accept(this)
            }
            printer.printWithNoIndent(")", ": ")
            ktFunction.returnType.accept(this)
            renderExtraTypeParametersUpperBounds(ktFunction.typeParameterList)
            if (ktFunction.block !== JKBodyStub) {
                printer.block(multiline = ktFunction.block.statements.isNotEmpty()) {
                    ktFunction.block.accept(this)
                }
            }
        }

        override fun visitKtOperatorExpression(ktOperatorExpression: JKKtOperatorExpression) {
            ktOperatorExpression.receiver.accept(this)
            printer.printWithNoIndent(" ")
            printer.printWithNoIndent(ktOperatorExpression.identifier.name)
            printer.printWithNoIndent(" ")
            ktOperatorExpression.argument.accept(this)
        }

        override fun visitIfElseExpression(ifElseExpression: JKIfElseExpression) {
            printer.printWithNoIndent("if (")
            ifElseExpression.condition.accept(this)
            printer.printWithNoIndent(")")
            ifElseExpression.thenBranch.accept(this)
            printer.printWithNoIndent(" else ")
            ifElseExpression.elseBranch.accept(this)
        }

        override fun visitIfStatement(ifStatement: JKIfStatement) {
            printer.printWithNoIndent("if (")
            ifStatement.condition.accept(this)
            printer.printWithNoIndent(")")
            renderStatementOrBlock(ifStatement.thenBranch)
        }

        override fun visitIfElseStatement(ifElseStatement: JKIfElseStatement) {
            visitIfStatement(ifElseStatement)
            printer.printWithNoIndent(" else ")
            renderStatementOrBlock(ifElseStatement.elseBranch)
        }

        private fun renderStatementOrBlock(statement: JKStatement, multiline: Boolean = false) {
            if (statement is JKBlockStatement) {
                printer.block(multiline) {
                    statement.block.accept(this)
                }
            } else {
                statement.accept(this)
            }
        }

        override fun visitKtGetterOrSetter(ktGetterOrSetter: JKKtGetterOrSetter) {
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

        override fun visitKtEmptyGetterOrSetter(ktEmptyGetterOrSetter: JKKtEmptyGetterOrSetter) {

        }

        override fun visitBinaryExpression(binaryExpression: JKBinaryExpression) {
            binaryExpression.left.accept(this)
            printer.printWithNoIndent(" ")
            printer.printWithNoIndent(binaryExpression.operator.token.text)
            printer.printWithNoIndent(" ")
            binaryExpression.right.accept(this)
        }

        override fun visitTypeParameterList(typeParameterList: JKTypeParameterList) {
            if (typeParameterList.typeParameters.isNotEmpty()) {
                printer.par(ANGLE) {
                    renderList(typeParameterList.typeParameters) {
                        it.accept(this)
                    }
                }
            }
        }

        override fun visitTypeParameter(typeParameter: JKTypeParameter) {
            typeParameter.name.accept(this)
            if (typeParameter.upperBounds.size == 1) {
                printer.printWithNoIndent(" : ")
                typeParameter.upperBounds.single().accept(this)
            }
        }

        override fun visitLiteralExpression(literalExpression: JKLiteralExpression) {
            printer.printWithNoIndent(literalExpression.literal)
        }

        override fun visitPrefixExpression(prefixExpression: JKPrefixExpression) {
            printer.printWithNoIndent(prefixExpression.operator.token.text)
            prefixExpression.expression.accept(this)
        }

        override fun visitThisExpression(thisExpression: JKThisExpression) {
            printer.printWithNoIndent("this")
            thisExpression.qualifierLabel.accept(this)
        }

        override fun visitSuperExpression(superExpression: JKSuperExpression) {
            printer.printWithNoIndent("super")
        }

        override fun visitContinueStatement(continueStatement: JKContinueStatement) {
            printer.printWithNoIndent("continue")
            continueStatement.label.accept(this)
            printer.printlnWithNoIndent(" ")
        }

        override fun visitLabelEmpty(labelEmpty: JKLabelEmpty) {

        }

        override fun visitLabelText(labelText: JKLabelText) {
            printer.printWithNoIndent("@")
            labelText.label.accept(this)
            printer.printWithNoIndent(" ")
        }

        override fun visitLabeledStatement(labeledStatement: JKLabeledStatement) {
            for (label in labeledStatement.labels) {
                label.accept(this)
                printer.printWithNoIndent("@")
            }
            labeledStatement.statement.accept(this)
        }

        override fun visitNameIdentifier(nameIdentifier: JKNameIdentifier) {
            printer.printWithNoIndent(nameIdentifier.value.escaped())
        }

        override fun visitPostfixExpression(postfixExpression: JKPostfixExpression) {
            postfixExpression.expression.accept(this)
            printer.printWithNoIndent(postfixExpression.operator.token.text)
        }

        override fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression) {
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

        override fun visitExpressionList(expressionList: JKExpressionList) {
            renderList(expressionList.expressions) { it.accept(this) }
        }

        override fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression) {
            printer.printWithNoIndent(FqName(methodCallExpression.identifier.fqName).shortName().asString())
            methodCallExpression.typeArgumentList.accept(this)
            printer.par {
                methodCallExpression.arguments.accept(this)
            }
        }

        override fun visitTypeArgumentList(typeArgumentList: JKTypeArgumentList) {
            if (typeArgumentList.typeArguments.isNotEmpty()) {
                printer.par(ANGLE) {
                    renderList(typeArgumentList.typeArguments) {
                        it.accept(this)
                    }
                }
            }
        }

        override fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression) {
            printer.par {
                parenthesizedExpression.expression.accept(this)
            }
        }

        override fun visitDeclarationStatement(declarationStatement: JKDeclarationStatement) {
            declarationStatement.declaredStatements.forEach {
                it.accept(this)
                printer.printlnWithNoIndent()
            }
        }

        override fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression) {
            typeCastExpression.expression.accept(this)
            printer.printWithNoIndent(" as ")
            typeCastExpression.type.accept(this)
        }

        override fun visitWhileStatement(whileStatement: JKWhileStatement) {
            printer.print("while(")
            whileStatement.condition.accept(this)
            printer.printWithNoIndent(")")
            if (whileStatement.body.isEmpty()) {
                printer.printWithNoIndent(";")
            } else {
                renderStatementOrBlock(whileStatement.body, multiline = true)
            }
        }

        override fun visitLocalVariable(localVariable: JKLocalVariable) {
//            if (localVariable.parent !is JKForInStatement) {
//                if (localVariable.modifierList.modality == JKModifierTokens.FINAL) {
//                    printer.print("val")
//                } else {
//                    printer.print("var")
//                }
//            }
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

        override fun visitEmptyStatement(emptyStatement: JKEmptyStatement) {
        }

        override fun visitStubExpression(stubExpression: JKStubExpression) {
        }

        override fun visitKtConvertedFromForLoopSyntheticWhileStatement(
            ktConvertedFromForLoopSyntheticWhileStatement: JKKtConvertedFromForLoopSyntheticWhileStatement
        ) {
            ktConvertedFromForLoopSyntheticWhileStatement.variableDeclaration.accept(this)
            printer.printlnWithNoIndent()
            ktConvertedFromForLoopSyntheticWhileStatement.whileStatement.accept(this)
        }

        private fun renderType(type: JKType) {
            if (type is JKNoTypeImpl) return
            when (type) {
                is JKClassType -> type.classReference.fqName?.let { printer.printWithNoIndent(FqName(it).shortName().asString()) }
                is JKUnresolvedClassType -> printer.printWithNoIndent(type.name)
                is JKContextType -> return
                is JKStarProjectionType ->
                    printer.printWithNoIndent("*")
                is JKTypeParameterType ->
                    printer.printWithNoIndent(type.name)
                is JKVarianceTypeParameterType -> {
                    when (type.variance) {
                        JKVarianceTypeParameterType.Variance.IN -> printer.printWithNoIndent("in ")
                        JKVarianceTypeParameterType.Variance.OUT -> printer.printWithNoIndent("out ")
                    }
                    renderType(type.boundType)
                }
                else -> printer.printWithNoIndent("Unit /* TODO: ${type::class} */")
            }
            if (type is JKParametrizedType && type.parameters.isNotEmpty()) {
                printer.par(ANGLE) {
                    renderList(type.parameters, renderElement = ::renderType)
                }
            }
            when (type.nullability) {
                Nullability.Nullable -> printer.printWithNoIndent("?")
                Nullability.Default -> printer.printWithNoIndent("?")// /* TODO: Default */")
                else -> {
                }
            }
        }

        override fun visitJavaNewExpression(javaNewExpression: JKJavaNewExpression) {
            if (javaNewExpression.isAnonymousClass()) {
                printer.printWithNoIndent("object : ")
            }
            printer.printWithNoIndent(javaNewExpression.classSymbol.name)
            javaNewExpression.typeArgumentList.accept(this)
            if (javaNewExpression.constructorIsPresent()) {
                printer.par(ROUND) {
                    javaNewExpression.arguments.accept(this)
                }
            }
            if (javaNewExpression.isAnonymousClass()) {
                javaNewExpression.classBody.accept(this)
            }
        }


        override fun visitClassBody(classBody: JKClassBody) {
            val declarationsToPrint = classBody.declarations.filterNot { it is JKKtPrimaryConstructor }
            printer.block(multiline = true) {
                renderEnumConstants(declarationsToPrint.filterIsInstance())
                renderNonEnumClassDeclarations(declarationsToPrint.filterNot { it is JKEnumConstant })
            }
        }

        override fun visitEmptyClassBody(emptyClassBody: JKEmptyClassBody) {}

        override fun visitTypeElement(typeElement: JKTypeElement) {
            renderType(typeElement.type)
        }

        override fun visitBlock(block: JKBlock) {
            renderList(block.statements, { printer.println() }) {
                it.accept(this)
            }
        }

        override fun visitBlockStatementWithoutBrackets(blockStatementWithoutBrackets: JKBlockStatementWithoutBrackets) {
            blockStatementWithoutBrackets.block.accept(this)
        }

        override fun visitExpressionStatement(expressionStatement: JKExpressionStatement) {
            printer.printIndent()
            expressionStatement.expression.accept(this)
        }

        override fun visitReturnStatement(returnStatement: JKReturnStatement) {
            printer.print("return ")
            returnStatement.expression.accept(this)
        }

        override fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression) {
            printer.printWithNoIndent(FqName(fieldAccessExpression.identifier.fqName).shortName().asString())
        }

        override fun visitArrayAccessExpression(arrayAccessExpression: JKArrayAccessExpression) {
            arrayAccessExpression.expression.accept(this)
            printer.par(SQUARE) { arrayAccessExpression.indexExpression.accept(this) }
        }

        override fun visitDelegationConstructorCall(delegationConstructorCall: JKDelegationConstructorCall) {
            delegationConstructorCall.expression.accept(this)
            printer.par {
                delegationConstructorCall.arguments.accept(this)
            }
        }

        private fun renderParameterList(parameters: List<JKParameter>) {
            printer.par(ROUND) {
                renderList(parameters) {
                    it.accept(this)
                }
            }
        }

        override fun visitKtConstructor(ktConstructor: JKKtConstructor) {
            renderModifiersList(ktConstructor)
            printer.print(" constructor")
            renderParameterList(ktConstructor.parameters)
            if (ktConstructor.delegationCall !is JKStubExpression) {
                builder.append(" : ")
                ktConstructor.delegationCall.accept(this)
            }
            if (ktConstructor.block !== JKBodyStub) {
                printer.block(multiline = true) {
                    ktConstructor.block.accept(this)
                }
            }
        }

        override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor) {
            renderModifiersList(ktPrimaryConstructor)
            printer.printWithNoIndent(" constructor ")
            if (ktPrimaryConstructor.parameters.isNotEmpty()) {
                renderParameterList(ktPrimaryConstructor.parameters)
            }
        }

        private inline fun Printer.indented(block: () -> Unit) {
            this.pushIndent()
            block()
            this.popIndent()
        }

        private inline fun Printer.block(multiline: Boolean = false, crossinline body: () -> Unit) {
            par(if (multiline) CURVED_MULTILINE else CURVED) {
                indented(body)
            }
        }

        private inline fun Printer.par(kind: ParenthesisKind = ParenthesisKind.ROUND, body: () -> Unit) {
            this.printWithNoIndent(kind.open)
            body()
            this.printWithNoIndent(kind.close)
        }

        override fun visitLambdaExpression(lambdaExpression: JKLambdaExpression) {
            printer.par(CURVED_MULTILINE) {
                lambdaExpression.parameters.firstOrNull()?.accept(this)
                lambdaExpression.parameters.asSequence().drop(1).forEach { printer.printWithNoIndent(", "); it.accept(this) }
                if (lambdaExpression.parameters.isNotEmpty()) {
                    printer.printWithNoIndent(" -> ")
                }

                val statement = lambdaExpression.statement
                if (statement is JKBlockStatement) {
                    statement.block.accept(this)
                } else {
                    statement.accept(this)
                }
            }
        }

        override fun visitBlockStatement(blockStatement: JKBlockStatement) {
            printer.par(CURVED) {
                blockStatement.block.accept(this)
            }
        }

        /*override fun visitParameter(parameter: JKParameter) {
            printer.printWithNoIndent(parameter.name.value)
        }*/

        override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement) {
            ktAssignmentStatement.field.accept(this)
            printer.printWithNoIndent(" ")
            printer.printWithNoIndent(ktAssignmentStatement.operator.token.text)
            printer.printWithNoIndent(" ")
            ktAssignmentStatement.expression.accept(this)
        }

        override fun visitKtWhenStatement(ktWhenStatement: JKKtWhenStatement) {
            printer.printWithNoIndent("when(")
            ktWhenStatement.expression.accept(this)
            printer.printWithNoIndent(")")
            printer.block(multiline = true) {
                renderList(ktWhenStatement.cases, { printer.printlnWithNoIndent() }) {
                    it.accept(this)
                }
            }
        }

        override fun visitAnnotationList(annotationList: JKAnnotationList) {
            renderList(annotationList.annotations, " ") {
                it.accept(this)
            }
        }

        override fun visitAnnotation(annotation: JKAnnotation) {
            printer.printWithNoIndent("@")
            printer.printWithNoIndent(annotation.classSymbol.name)
            if (annotation.arguments.expressions.isNotEmpty()) {
                printer.par {
                    annotation.arguments.accept(this)
                }
            }
        }

        override fun visitClassLiteralExpression(classLiteralExpression: JKClassLiteralExpression) {
            renderType(classLiteralExpression.classType.type.updateNullability(Nullability.NotNull))
            printer.printWithNoIndent("::")
            when (classLiteralExpression.literalType) {
                JKClassLiteralExpression.LiteralType.KOTLIN_CLASS -> printer.printWithNoIndent("class")
                JKClassLiteralExpression.LiteralType.JAVA_CLASS -> printer.printWithNoIndent("class.java")
                JKClassLiteralExpression.LiteralType.JAVA_PRIMITIVE_CLASS -> printer.printWithNoIndent("class.javaPrimitiveType")
            }
        }

        override fun visitKtWhenCase(ktWhenCase: JKKtWhenCase) {
            renderList(ktWhenCase.labels, ", ") {
                it.accept(this)
            }
            printer.printWithNoIndent(" -> ")
            ktWhenCase.statement.accept(this)
        }

        override fun visitKtElseWhenLabel(ktElseWhenLabel: JKKtElseWhenLabel) {
            printer.printWithNoIndent("else")
        }

        override fun visitKtValueWhenLabel(ktValueWhenLabel: JKKtValueWhenLabel) {
            ktValueWhenLabel.expression.accept(this)
        }
    }

    private enum class ParenthesisKind(val open: String, val close: String) {
        ROUND("(", ")"), SQUARE("[", "]"), CURVED("{", "}"), CURVED_MULTILINE("{\n", "}\n"), INLINE_COMMENT("/*", "*/"), ANGLE("<", ">")
    }


    fun printCodeOut(root: JKTreeElement): String {
        Visitor().also { root.accept(it) }
        return builder.toString()
    }
}



private inline fun <T> List<T>.headTail(): Pair<T?, List<T>?> {
    val head = this.firstOrNull()
    val tail = if (size <= 1) null else subList(1, size)
    return head to tail
}

private inline fun JKClass.primaryConstructor(): JKKtPrimaryConstructor? =
    classBody.declarations.firstIsInstanceOrNull()


private inline fun JKDelegationConstructorCall.isCallOfConstructorOf(type: JKType): Boolean {
    return when (type) {
        is JKClassType -> {
            val symbol = type.classReference as? JKClassSymbol ?: return false
            this.identifier.name == symbol.name && this.identifier.declaredIn == symbol
        }
        is JKUnresolvedClassType -> {
            this.identifier.name == type.name
        }
        else -> false
    }
}
private val KEYWORDS = KtTokens.KEYWORDS.types.map { (it as KtKeywordToken).value }.toSet()

private fun String.escaped() =
    if (this in KEYWORDS || '$' in this) "`$this`"
    else this
