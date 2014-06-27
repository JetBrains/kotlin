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

class ConstructorConverter(private val psiClass: PsiClass, private val converter: Converter) {
    private val typeConverter = converter.typeConverter

    private val tempValName: String = "__"
    private fun tempValIdentifier(): Identifier = Identifier(tempValName, false).assignNoPrototype()

    private val className = psiClass.getName()!!
    private val constructors = psiClass.getConstructors()
    private val constructorsToDrop = HashSet<PsiMethod>()
    private val lastParamDefaults = ArrayList<Expression>() // defaults for a few last parameters of primary constructor in reverse order
    private val primaryConstructor: PsiMethod? = when (constructors.size) {
        0 -> null
        1 -> constructors.single()
        else -> choosePrimaryConstructor()
    }

    private class TargetConstructorInfo(
            /**
             * Target constructor (one which is finally invoked by this one)
             */
            val constructor: PsiMethod,
            /**
             * Is not null if this constructor is equivalent to the target constructor with a few last parameters having default values
             */
            val parameterDefaults: List<Expression>?)

    private fun choosePrimaryConstructor(): PsiMethod? {
        val toTargetConstructorMap = HashMap<PsiMethod, TargetConstructorInfo>()
        for (constructor in constructors) {
            val firstStatement = constructor.getBody()?.getStatements()?.firstOrNull()
            val methodCall = (firstStatement as? PsiExpressionStatement)?.getExpression() as? PsiMethodCallExpression
            if (methodCall != null) {
                val refExpr = methodCall.getMethodExpression()
                if (refExpr.getCanonicalText() == "this") {
                    val target = refExpr.resolve() as? PsiMethod
                    if (target != null && target.isConstructor()) {
                        var parameterDefaults = calcTargetParameterDefaults(constructor, target, methodCall)

                        val finalTargetInfo = toTargetConstructorMap[target]
                        if (finalTargetInfo != null && parameterDefaults != null) {
                            parameterDefaults = if (finalTargetInfo.parameterDefaults != null)
                                parameterDefaults!! + finalTargetInfo.parameterDefaults
                            else
                                null
                        }
                        val finalTarget = finalTargetInfo?.constructor ?: target!! //TODO: see KT-5335

                        toTargetConstructorMap[constructor] = TargetConstructorInfo(finalTarget, parameterDefaults)
                        for (entry in toTargetConstructorMap.entrySet()) {
                            if (entry.value.constructor == constructor) {
                                val newParameterDefaults = if (parameterDefaults != null)
                                    entry.value.parameterDefaults?.plus(parameterDefaults!!)
                                else
                                    null
                                entry.setValue(TargetConstructorInfo(finalTarget, newParameterDefaults))
                            }
                        }
                    }
                }
            }
        }

        val candidates = constructors.filter { it !in toTargetConstructorMap }
        if (candidates.size != 1) return null // there should be only one constructor which does not call other constructor
        val primary = candidates.single()
        if (toTargetConstructorMap.values().any() { it.constructor != primary }) return null // all other constructors call our candidate (directly or indirectly)

        dropConstructorsForDefaultValues(primary, toTargetConstructorMap)

        return primary
    }

    private fun calcTargetParameterDefaults(constructor: PsiMethod, target: PsiMethod, targetCall: PsiMethodCallExpression): List<Expression>? {
        if (constructor.getBody()!!.getStatements().size != 1) return null // constructor's body should consist of only "this(...)"
        val parameters = constructor.getParameterList().getParameters()
        val targetParameters = target.getParameterList().getParameters()
        if (parameters.size >= targetParameters.size) return null
        val args = targetCall.getArgumentList().getExpressions()
        if (args.size != targetParameters.size) return null // incorrect code

        for (i in parameters.indices) {
            val parameter = parameters[i]
            val targetParameter = targetParameters[i]
            if (parameter.getName() != targetParameter.getName() || parameter.getType() != targetParameter.getType()) return null
            val arg = args[i]
            if (arg !is PsiReferenceExpression || arg.getQualifier() != null) return null
            if (arg.resolve() != parameter) return null
        }

        val result = ArrayList<Expression>(args.size - parameters.size)
        for (i in (parameters.size..args.size-1)) {
            result.add(converter.convertExpression(args[i]))
        }
        return result
    }

    private fun dropConstructorsForDefaultValues(primary: PsiMethod, toTargetConstructorMap: Map<PsiMethod, TargetConstructorInfo>) {
        //TODO: should we drop when annotations exist?

        val dropCandidates = toTargetConstructorMap
                .filter { it.value.parameterDefaults != null }
                .map { it.key }
                .filter { it.accessModifier() == primary.accessModifier() }
                .sortBy { -it.getParameterList().getParametersCount() } // we will try to drop them starting from ones with more parameters
        val primaryParamCount = primary.getParameterList().getParametersCount()
        @DropCandidatesLoop
        for (constructor in dropCandidates) {
            val paramCount = constructor.getParameterList().getParametersCount()
            assert(paramCount < primaryParamCount)
            val defaults = toTargetConstructorMap[constructor]!!.parameterDefaults!!
            assert(defaults.size == primaryParamCount - paramCount)

            for (i in (0..defaults.size-1)) {
                val default = defaults[defaults.size - i - 1]
                if (i < lastParamDefaults.size) { // default for this parameter has already been assigned
                    if (lastParamDefaults[i].canonicalCode() != default.canonicalCode()) continue@DropCandidatesLoop
                }
                else {
                    lastParamDefaults.add(default)
                }
            }

            constructorsToDrop.add(constructor)
        }
    }

    public fun baseClassParams(): PsiExpressionList? {
        if (primaryConstructor == null) return null
        val statement = primaryConstructor.getBody()?.getStatements()?.firstOrNull()
        val methodCall = (statement as? PsiExpressionStatement)?.getExpression() as? PsiMethodCallExpression
        if (methodCall != null && methodCall.isSuperConstructorCall()) {
            return methodCall.getArgumentList()
        }
        else {
            return null
        }
    }

    public fun convertConstructor(constructor: PsiMethod,
                                  annotations: Annotations,
                                  modifiers: Modifiers,
                                  membersToRemove: MutableSet<PsiMember>,
                                  postProcessBody: (Block) -> Block): Member? {
        if (constructor == primaryConstructor) {
            return convertPrimaryConstructor(constructor, annotations, modifiers, membersToRemove, postProcessBody)
        }
        else {
            if (constructor in constructorsToDrop) return null

            val params = converter.convertParameterList(constructor.getParameterList())
            val bodyConverter = converter.withExpressionVisitor { object : ExpressionVisitor(it, mapOf()/*TODO: see KT-5327*/) {
                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    if (isQualifierEmptyOrThis(expression)) {
                        val member = expression.getReference()?.resolve() as? PsiMember
                        if (member != null &&
                                !member.isConstructor() &&
                                member.getContainingClass() == constructor.getContainingClass()) {
                            val isNullable = member is PsiField && typeConverter.variableNullability(member).isNullable(converter.settings)
                            val qualifier = if (member.hasModifierProperty(PsiModifier.STATIC)) constructor.declarationIdentifier() else tempValIdentifier()
                            result = QualifiedExpression(qualifier, Identifier(expression.getReferenceName()!!, isNullable).assignNoPrototype())
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
            return FactoryFunction(constructor.declarationIdentifier(), annotations, correctFactoryFunctionAccess(modifiers),
                                   factoryFunctionType, params, typeParameterList, body)
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

            val bodyConverter = converter.withExpressionVisitor {
                object : ExpressionVisitor(it, usageReplacementMap) {
                    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                        if (expression.isSuperConstructorCall()) return // skip it
                        super.visitMethodCallExpression(expression)
                    }
                }
            }
            postProcessBody(bodyConverter.convertBlock(body, false, { !statementsToRemove.contains(it) }))
        }
        else {
            Block.Empty
        }

        val parameterList = ParameterList(params.indices.map { i ->
            val parameter = params[i]
            val indexFromEnd = params.size - i - 1
            val defaultValue = if (indexFromEnd < lastParamDefaults.size) lastParamDefaults[indexFromEnd] else null
            if (!parameterToField.containsKey(parameter)) {
                converter.convertParameter(parameter, defaultValue = defaultValue)
            }
            else {
                val (field, `type`) = parameterToField[parameter]!!
                Parameter(field.declarationIdentifier(),
                          `type`,
                          if (isVal(field)) Parameter.VarValModifier.Val else Parameter.VarValModifier.Var,
                          converter.convertAnnotations(parameter) + converter.convertAnnotations(field),
                          converter.convertModifiers(field).filter { it in ACCESS_MODIFIERS },
                          defaultValue).assignPrototypes(listOf(parameter, field), CommentsAndSpacesInheritance(blankLinesBefore = false))
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

    public fun postProcessConstructors(classBody: ClassBody): ClassBody {
        if (primaryConstructor == null && constructors.size > 1) {
            return generateArtificialPrimaryConstructor(classBody)
        }
        else {
            val updatedFunctions = replaceConstructorCallsInFactoryFunctions(classBody.factoryFunctions)
            return ClassBody(classBody.primaryConstructorSignature, classBody.members, classBody.classObjectMembers, updatedFunctions, classBody.lBrace, classBody.rBrace)
        }
    }

    private fun generateArtificialPrimaryConstructor(classBody: ClassBody): ClassBody {
        assert(classBody.primaryConstructorSignature == null)

        val fieldsToInitialize = classBody.members.filterIsInstance(javaClass<Field>()).filter { it.isVal }
        val updatedFactoryFunctions = ArrayList<FactoryFunction>()
        for (function in classBody.factoryFunctions) {
            val body = function.body!!
            // 2 cases: secondary constructor either calls another constructor or does not call any
            val newStatements = replaceConstructorCallInFactoryFunction(body) ?:
                    insertCallToArtificialPrimary(body, fieldsToInitialize)
            val newBody = Block(newStatements, LBrace().assignNoPrototype(), RBrace().assignNoPrototype()).assignNoPrototype()
            updatedFactoryFunctions.add(function.withBody(newBody))
        }

        val parameters = fieldsToInitialize.map { field ->
            val varValModifier = if (field.isVal) Parameter.VarValModifier.Val else Parameter.VarValModifier.Var
            Parameter(field.identifier, field.`type`, varValModifier, field.annotations, field.modifiers.filter { it in ACCESS_MODIFIERS }).assignPrototypesFrom(field)
        }

        val modifiers = Modifiers.Empty
        //TODO: we can generate it private when secondary constructors are supported by Kotlin
        //val modifiers = Modifiers(listOf(Modifier.PRIVATE)).assignNoPrototype()
        val parameterList = ParameterList(parameters).assignNoPrototype()
        val constructorSignature = PrimaryConstructorSignature(modifiers, parameterList).assignNoPrototype()
        val updatedMembers = classBody.members.filter { !fieldsToInitialize.contains(it) }
        return ClassBody(constructorSignature, updatedMembers, classBody.classObjectMembers, updatedFactoryFunctions, classBody.lBrace, classBody.rBrace)
    }

    private fun replaceConstructorCallsInFactoryFunctions(functions: List<FactoryFunction>): List<FactoryFunction> {
        return functions.map { function ->
            val body = function.body!!
            val statements = replaceConstructorCallInFactoryFunction(body)
            if (statements != null) {
                function.withBody(Block(statements, body.lBrace, body.rBrace).assignPrototypesFrom(body))
            }
            else {
                function
            }
        }
    }

    private fun replaceConstructorCallInFactoryFunction(body: Block): List<Statement>? {
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
                    statements.add(ReturnStatement(tempValIdentifier()).assignNoPrototype())
                    return statements
                }
            }
        }

        return null
    }

    private fun insertCallToArtificialPrimary(body: Block, fieldsToInitialize: Collection<Field>): List<Statement> {
        val initializers = HashMap<Field, Expression?>()
        for (field in fieldsToInitialize) {
            initializers.put(field, getDefaultInitializer(field))
        }

        val statements = ArrayList<Statement>()
        for (statement in body.statements) {
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
        val initializer = MethodCallExpression.buildNotNull(null, className, arguments).assignNoPrototype()
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
        return statements
    }

    private fun correctFactoryFunctionAccess(modifiers: Modifiers): Modifiers {
        val classAccess = converter.convertModifiers(psiClass).accessModifier()
        return when(modifiers.accessModifier()) {
            Modifier.PUBLIC -> modifiers.without(Modifier.PUBLIC).with(classAccess)
            Modifier.PROTECTED -> modifiers.without(Modifier.PROTECTED).with(classAccess)
            Modifier.PRIVATE -> modifiers
            else/*internal*/ -> if (classAccess != Modifier.PUBLIC) modifiers.with(classAccess) else modifiers
        }
    }

    private fun PsiMethodCallExpression.isSuperConstructorCall(): Boolean {
        val ref = getMethodExpression()
        return ref.getCanonicalText() == "super" && ref.resolve()?.isConstructor() ?: false
    }
}