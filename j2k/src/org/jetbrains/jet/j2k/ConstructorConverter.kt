/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import com.intellij.psi.*
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.visitors.ExpressionVisitor
import com.intellij.psi.util.PsiUtil
import java.util.HashMap
import java.util.ArrayList
import java.util.HashSet

class ConstructorConverter(private val converter: Converter) {
    private val typeConverter = converter.typeConverter

    public fun convertConstructor(constructor: PsiMethod,
                                          annotations: Annotations,
                                          modifiers: Modifiers,
                                          membersToRemove: MutableSet<PsiMember>,
                                          postProcessBody: (Block) -> Block): Member {
        if (constructor.isPrimaryConstructor()) {
            return convertPrimaryConstructor(constructor, annotations, modifiers, membersToRemove, postProcessBody)
        }
        else {
            val params = converter.convertParameterList(constructor.getParameterList())
            val bodyConverter = converter.withExpressionVisitor { object : ExpressionVisitor(it, mapOf()/*TODO: see KT-5327*/) {
                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    if (isQualifierEmptyOrThis(expression)) {
                        val member = expression.getReference()?.resolve() as? PsiMember
                        if (member != null &&
                                !member.isConstructor() &&
                                member.getContainingClass() == constructor.getContainingClass() &&
                                !member.hasModifierProperty(PsiModifier.STATIC)) {
                            val isNullable = member is PsiField && typeConverter.variableNullability(member).isNullable(converter.settings)
                            result = QualifiedExpression(tempValIdentifier(), Identifier(expression.getReferenceName()!!, isNullable).assignNoPrototype())
                            return
                        }
                    }

                    super.visitReferenceExpression(expression)
                }
            }}
            var body = postProcessBody(bodyConverter.convertBlock(constructor.getBody()))
            val containingClass = constructor.getContainingClass()
            val typeParameterList = converter.convertTypeParameterList(containingClass?.getTypeParameterList())
            val factoryFunctionType = ClassType(containingClass?.declarationIdentifier() ?: Identifier.Empty,
                                                typeParameterList.parameters,
                                                Nullability.NotNull,
                                                converter.settings).assignNoPrototype()
            return FactoryFunction(annotations, modifiers, factoryFunctionType, params, typeParameterList, body)
        }
    }

    private fun convertPrimaryConstructor(constructor: PsiMethod,
                                          annotations: Annotations,
                                          modifiers: Modifiers,
                                          membersToRemove: MutableSet<PsiMember>,
                                          postProcessBody: (Block) -> Block): PrimaryConstructor {
        val params = constructor.getParameterList().getParameters()
        val parameterToField = HashMap<PsiParameter, Pair<PsiField, Type>>()
        val body = constructor.getBody()
        val block = if (body != null) {
            val statementsToRemove = HashSet<PsiStatement>()
            val usageReplacementMap = HashMap<PsiVariable, String>()
            for (parameter in params) {
                val (field, initializationStatement) = findBackingFieldForConstructorParameter(parameter, constructor) ?: continue

                val fieldType = typeConverter.convertVariableType(field)
                val parameterType = typeConverter.convertVariableType(parameter)
                // types can be different only in nullability
                val `type` = if (fieldType == parameterType) {
                    fieldType
                }
                else if (fieldType.toNotNullType() == parameterType.toNotNullType()) {
                    if (fieldType.isNullable) fieldType else parameterType // prefer nullable one
                }
                else {
                    continue
                }

                parameterToField.put(parameter, field to `type`)
                statementsToRemove.add(initializationStatement)
                membersToRemove.add(field)

                if (field.getName() != parameter.getName()) {
                    usageReplacementMap.put(parameter, field.getName()!!)
                }
            }

            val bodyConverter = converter.withExpressionVisitor { ExpressionVisitor(it, usageReplacementMap) }
            postProcessBody(bodyConverter.convertBlock(body, false, { !statementsToRemove.contains(it) }))
        }
        else {
            Block.Empty
        }

        val parameterList = ParameterList(params.map { parameter ->
            if (!parameterToField.containsKey(parameter)) {
                converter.convertParameter(parameter)
            }
            else {
                val (field, `type`) = parameterToField[parameter]!!
                Parameter(field.declarationIdentifier(),
                          `type`,
                          if (isVal(field)) Parameter.VarValModifier.Val else Parameter.VarValModifier.Var,
                          converter.convertAnnotations(parameter) + converter.convertAnnotations(field),
                          converter.convertModifiers(field).filter { it in ACCESS_MODIFIERS }).assignPrototypes(listOf(parameter, field), CommentsAndSpacesInheritance(blankLinesBefore = false))
            }
        }).assignPrototype(constructor.getParameterList())
        return PrimaryConstructor(annotations, modifiers, parameterList, block).assignPrototype(constructor)
    }

    private fun findBackingFieldForConstructorParameter(parameter: PsiParameter, constructor: PsiMethod): Pair<PsiField, PsiStatement>? {
        val body = constructor.getBody() ?: return null

        val refs = findVariableUsages(parameter, body)

        if (refs.any { PsiUtil.isAccessedForWriting(it) }) return null

        for (ref in refs) {
            val assignment = ref.getParent() as? PsiAssignmentExpression ?: continue
            if (assignment.getOperationSign().getTokenType() != JavaTokenType.EQ) continue
            val assignee = assignment.getLExpression() as? PsiReferenceExpression ?: continue
            if (!isQualifierEmptyOrThis(assignee)) continue
            val field = assignee.resolve() as? PsiField ?: continue
            if (field.getContainingClass() != constructor.getContainingClass()) continue
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue
            if (field.getInitializer() != null) continue

            // assignment should be a top-level statement
            val statement = assignment.getParent() as? PsiExpressionStatement ?: continue
            if (statement.getParent() != body) continue

            // and no other assignments to field should exist in the constructor
            if (findVariableUsages(field, body).any { it != assignee && PsiUtil.isAccessedForWriting(it) && isQualifierEmptyOrThis(it) }) continue
            //TODO: check access to field before assignment

            return field to statement
        }

        return null
    }

    public fun postProcessConstructors(classBody: ClassBody, psiClass: PsiClass): ClassBody {
        if (psiClass.getPrimaryConstructor() == null && psiClass.getConstructors().size > 1) {
            return generateArtificialPrimaryConstructor(psiClass.declarationIdentifier(), classBody)
        }
        else {
            correctFactoryFunctions(classBody, psiClass.getName()!!)
            return classBody
        }
    }

    private fun generateArtificialPrimaryConstructor(className: Identifier, classBody: ClassBody): ClassBody {
        assert(classBody.primaryConstructorSignature == null)

        val fieldsToInitialize = classBody.members.filterIsInstance(javaClass<Field>()).filter { it.isVal }
        val initializers = HashMap<Field, Expression?>()
        for (factoryFunction in classBody.factoryFunctions()) {
            for (field in fieldsToInitialize) {
                initializers.put(field, getDefaultInitializer(field))
            }

            val statements = ArrayList<Statement>()
            for (statement in factoryFunction.body!!.statements) {
                var keepStatement = true
                if (statement is AssignmentExpression) {
                    val assignee = statement.left
                    if (assignee is QualifiedExpression && (assignee.qualifier as? Identifier)?.name == tempValName) {
                        val name = (assignee.identifier as Identifier).name
                        for (field in fieldsToInitialize) {
                            if (name == field.identifier.name) {
                                initializers.put(field, statement.right)
                                keepStatement = false
                            }

                        }
                    }

                }

                if (keepStatement) {
                    statements.add(statement)
                }
            }

            val arguments = fieldsToInitialize.map { initializers[it] ?: LiteralExpression("null").assignNoPrototype() }
            val initializer = MethodCallExpression.buildNotNull(null, className.name, arguments).assignNoPrototype()
            if (statements.isNotEmpty()) {
                val localVar = LocalVariable(tempValIdentifier(),
                                             Annotations.Empty,
                                             Modifiers.Empty,
                                             null,
                                             initializer,
                                             true).assignNoPrototype()
                statements.add(0, DeclarationStatement(listOf(localVar)).assignNoPrototype())
                statements.add(ReturnStatement(tempValIdentifier()).assignNoPrototype())
            }
            else {
                statements.add(ReturnStatement(initializer).assignNoPrototype())
            }

            factoryFunction.body = Block(statements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype()).assignNoPrototype()
        }

        val parameters = fieldsToInitialize.map { field ->
            val varValModifier = if (field.isVal) Parameter.VarValModifier.Val else Parameter.VarValModifier.Var
            Parameter(field.identifier, field.`type`, varValModifier, field.annotations, field.modifiers.filter { it in ACCESS_MODIFIERS }).assignPrototypesFrom(field)
        }

        val modifiers = Modifiers(listOf(Modifier.PRIVATE)).assignNoPrototype()
        val parameterList = ParameterList(parameters).assignNoPrototype()
        val constructorSignature = PrimaryConstructorSignature(modifiers, parameterList).assignNoPrototype()
        val updatedMembers = classBody.members.filter { !fieldsToInitialize.contains(it) }
        return ClassBody(constructorSignature, updatedMembers, classBody.classObjectMembers, classBody.lBrace, classBody.rBrace)
    }

    private fun correctFactoryFunctions(classBody: ClassBody, className: String) {
        for (factoryFunction in classBody.factoryFunctions()) {
            val body = factoryFunction.body!!
            val statements = correctFactoryFunctionStatements(body, className)
            factoryFunction.body = Block(statements, body.lBrace, body.rBrace).assignPrototypesFrom(body)
        }
    }

    private fun correctFactoryFunctionStatements(body: Block, className: String): List<Statement> {
        val statements = ArrayList(body.statements)

        // searching for other constructor call in form "this(...)"
        // it's not necessary the first statement because of statements inserted for writable parameters
        for (i in statements.indices) {
            val statement = statements[i]
            if (statement is MethodCallExpression) {
                if ((statement.methodExpression as? Identifier)?.name == "this") {
                    val constructorCall = MethodCallExpression.buildNotNull(null, className, statement.arguments).assignPrototypesFrom(statement)
                    if (i == statements.lastIndex) { // constructor call is the last statement - no intermediate variable needed
                        statements[i] = ReturnStatement(constructorCall).assignNoPrototype()
                        return statements
                    }
                    val localVar = LocalVariable(tempValIdentifier(), Annotations.Empty, Modifiers.Empty, null, constructorCall, true).assignNoPrototype()
                    statements[i] = DeclarationStatement(listOf(localVar)).assignNoPrototype()
                    break
                }
            }
        }

        statements.add(ReturnStatement(tempValIdentifier()).assignNoPrototype())
        return statements
    }

    private fun ClassBody.factoryFunctions() = classObjectMembers.filterIsInstance(javaClass<FactoryFunction>())

    private val tempValName: String = "__"
    private fun tempValIdentifier(): Identifier = Identifier(tempValName, false).assignNoPrototype()
}