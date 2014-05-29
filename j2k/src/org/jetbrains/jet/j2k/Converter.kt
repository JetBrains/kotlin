/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.j2k.ast.types.ClassType
import org.jetbrains.jet.j2k.ast.types.EmptyType
import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.visitors.*
import java.util.*
import com.intellij.psi.CommonClassNames.*
import org.jetbrains.jet.lang.types.expressions.OperatorConventions.*
import com.intellij.openapi.project.Project

public class Converter(val project: Project, val settings: ConverterSettings) {

    private var classIdentifiersSet: MutableSet<String> = HashSet()

    private val dispatcher: Dispatcher = Dispatcher(this)

    public var methodReturnType: PsiType? = null
        private set

    public fun setClassIdentifiers(identifiers: MutableSet<String>) {
        classIdentifiersSet = identifiers
    }

    public fun getClassIdentifiers(): Set<String> {
        return Collections.unmodifiableSet(classIdentifiersSet)
    }

    public fun clearClassIdentifiers() {
        classIdentifiersSet.clear()
    }

    public fun elementToKotlin(element: PsiElement): String
            = convertTopElement(element)?.toKotlin() ?: ""

    private fun convertTopElement(element: PsiElement?): Element? = when(element) {
        is PsiJavaFile -> convertFile(element)
        is PsiClass -> convertClass(element)
        is PsiMethod -> convertMethod(element)
        is PsiField -> convertField(element, element.getContainingClass())
        is PsiStatement -> convertStatement(element)
        is PsiExpression -> convertExpression(element)
        is PsiComment -> Comment(element.getText()!!)
        is PsiImportList -> convertImportList(element)
        is PsiImportStatementBase -> convertImport(element)
        is PsiPackageStatement -> PackageStatement(quoteKeywords(element.getPackageName() ?: ""))
        is PsiWhiteSpace -> WhiteSpace(element.getText()!!)
        else -> null
    }

    public fun convertFile(javaFile: PsiJavaFile): File {
        val fileMembers = FileMemberList(javaFile.getChildren().map { convertTopElement(it) }.filterNotNull())
        return File(fileMembers, createMainFunction(javaFile))
    }

    public fun convertAnonymousClass(anonymousClass: PsiAnonymousClass): AnonymousClass {
        return AnonymousClass(this, convertMembers(anonymousClass))
    }

    private fun convertMembers(psiClass: PsiClass): List<Element> {
        val allChildren = psiClass.getChildren().toList()
        val lBraceIndex = allChildren.indexOf(psiClass.getLBrace())
        return allChildren.subList(lBraceIndex, allChildren.size).map { convertMember(it, psiClass) }.filterNotNull()
    }

    private fun getComments(member: PsiMember): MemberComments {
        var relevantChildren = member.getChildren().toList()
        if (member is PsiClass) {
            val leftBraceIndex = relevantChildren.indexOf(member.getLBrace())
            relevantChildren = relevantChildren.subList(0, leftBraceIndex)
        }
        val whiteSpacesAndComments = relevantChildren
                .filter { it is PsiWhiteSpace || it is PsiComment }
                .map { convertElement(it) }
        return MemberComments(whiteSpacesAndComments)
    }

    private fun convertMember(e: PsiElement?, containingClass: PsiClass): Element? = when(e) {
        is PsiMethod -> convertMethod(e, true)
        is PsiField -> convertField(e, containingClass)
        is PsiClass -> convertClass(e)
        is PsiClassInitializer -> convertInitializer(e)
        else -> convertElement(e)
    }

    private fun convertClass(psiClass: PsiClass): Class {
        val modifiers = convertModifierList(psiClass.getModifierList())
        val fields = convertFields(psiClass.getFields(), psiClass)
        val typeParameters = convertTypeParameterList(psiClass.getTypeParameterList())
        val implementsTypes = convertToNotNullableTypes(psiClass.getImplementsListTypes())
        val extendsTypes = convertToNotNullableTypes(psiClass.getExtendsListTypes())
        val name = Identifier(psiClass.getName()!!)
        val members = ArrayList(convertMembers(psiClass))

        val baseClassParams: List<Expression> = run {
            val superVisitor = SuperVisitor()
            psiClass.accept(superVisitor)
            val resolvedSuperCallParameters = superVisitor.resolvedSuperCallParameters
            if (resolvedSuperCallParameters.size() == 1) {
                convertExpressions(resolvedSuperCallParameters.single().getExpressions())
            }
            else {
                listOf()
            }
        }

        if (!psiClass.isEnum() && !psiClass.isInterface() && psiClass.getConstructors().size > 1 && getPrimaryConstructorForThisCase(psiClass) == null) {
            val finalOrWithEmptyInitializer = fields.filter { it.isVal() || it.initializer.toKotlin().isEmpty() }
            val initializers = HashMap<String, String>()
            for (member in members) {
                if (member is Constructor && !member.isPrimary) {
                    for (field in finalOrWithEmptyInitializer) {
                        initializers.put(field.identifier.toKotlin(), getDefaultInitializer(field))
                    }

                    val newStatements = ArrayList<Statement>()
                    for (statement in member.block!!.statements) {
                        var isRemoved = false
                        if (statement is AssignmentExpression) {
                            val assignee = statement.left
                            if (assignee is CallChainExpression) {
                                for (field in finalOrWithEmptyInitializer) {
                                    val id = field.identifier.toKotlin()
                                    if (assignee.identifier.toKotlin().endsWith("." + id)) {
                                        initializers.put(id, statement.right.toKotlin())
                                        isRemoved = true
                                    }

                                }
                            }

                        }

                        if (!isRemoved) {
                            newStatements.add(statement)
                        }

                    }
                    newStatements.add(0, DummyStringExpression("val __ = " + createPrimaryConstructorInvocation(name.toKotlin(), finalOrWithEmptyInitializer, initializers)))
                    member.block = Block(newStatements)
                }
            }
            //TODO: comments?
            members.add(Constructor(this, Identifier.Empty, MemberComments.Empty, Collections.emptySet<Modifier>(),
                                    ClassType(name, listOf(), false, this),
                                    TypeParameterList.Empty,
                                    ParameterList(createParametersFromFields(finalOrWithEmptyInitializer)),
                                    Block(createInitStatementsFromFields(finalOrWithEmptyInitializer)),
                                    true))
        }

        if (psiClass.isInterface()) {
            return Trait(this, name, getComments(psiClass), modifiers, typeParameters, extendsTypes, listOf(), implementsTypes, members)
        }

        if (psiClass.isEnum()) {
            return Enum(this, name, getComments(psiClass), modifiers, typeParameters, listOf(), listOf(), implementsTypes, members)
        }

        return Class(this, name, getComments(psiClass), modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, members)
    }

    private fun convertInitializer(initializer: PsiClassInitializer): Initializer {
        return Initializer(convertBlock(initializer.getBody(), true), convertModifierList(initializer.getModifierList()))
    }

    private fun convertFields(fields: Array<PsiField>, psiClass: PsiClass): List<Field> {
        return fields.map { convertField(it, psiClass) }
    }

    private fun convertField(field: PsiField, psiClass: PsiClass?): Field {
        val modifiers = convertModifierList(field.getModifierList())
        if (field is PsiEnumConstant) {
            return EnumConstant(Identifier(field.getName()!!),
                                getComments(field),
                                modifiers,
                                convertType(field.getType()),
                                convertElement(field.getArgumentList()))
        }

        var kType = convertType(field.getType(), isAnnotatedAsNotNull(field.getModifierList()))
        if (field.hasModifierProperty(PsiModifier.FINAL) && isDefinitelyNotNull(field.getInitializer())) {
            kType = kType.convertedToNotNull();
        }

        return Field(Identifier(field.getName()!!),
                     getComments(field),
                     modifiers,
                     kType,
                     convertExpression(field.getInitializer(), field.getType()),
                     countWriteAccesses(field, psiClass))
    }

    private fun convertMethod(method: PsiMethod): Function {
        return convertMethod(method, true)
    }

    private fun convertMethod(method: PsiMethod, notEmpty: Boolean): Function {
        if (directlyOverridesMethodFromObject(method)) {
            dispatcher.expressionVisitor = ExpressionVisitorForDirectObjectInheritors(this)
        }
        else {
            dispatcher.expressionVisitor = ExpressionVisitor(this)
        }
        methodReturnType = method.getReturnType()
        val identifier = Identifier(method.getName())
        val returnType = convertType(method.getReturnType(), isAnnotatedAsNotNull(method.getModifierList()))
        val body = convertBlock(method.getBody(), notEmpty)

        val params = createFunctionParameters(method)
        val typeParameterList = convertTypeParameterList(method.getTypeParameterList())
        val modifiers = HashSet(convertModifierList(method.getModifierList()))
        if (isOverride(method)) {
            modifiers.add(Modifier.OVERRIDE)
        }

        val containingClass = method.getContainingClass()
        if (containingClass != null && containingClass.isInterface()) {
            modifiers.remove(Modifier.ABSTRACT)
        }

        if (isNotOpenMethod(method)) {
            modifiers.add(Modifier.NOT_OPEN)
        }

        if (method.isConstructor()) {
            return Constructor(this, identifier, getComments(method), modifiers, returnType, typeParameterList, params,
                               Block(body.statements), isConstructorPrimary(method))
        }

        return Function(this, identifier, getComments(method), modifiers, returnType, typeParameterList, params, body)
    }

    private fun createFunctionParameters(method: PsiMethod): ParameterList {
        val result = ArrayList<Parameter>()
        for (parameter in method.getParameterList().getParameters()) {
            result.add(Parameter(Identifier(parameter.getName()!!),
                                 convertType(parameter.getType(), isAnnotatedAsNotNull(parameter.getModifierList())),
                                 isReadOnly(parameter, method.getBody())))
        }
        return ParameterList(result)
    }

    public fun convertBlock(block: PsiCodeBlock?, notEmpty: Boolean = true): Block {
        if (block == null) return Block.Empty

        return Block(convertStatements(block.getChildren().toList()), notEmpty)
    }

    public fun convertStatements(statements: List<PsiElement>): StatementList {
        return StatementList(statements.map { if (it is PsiStatement) convertStatement(it) else convertElement(it) })
    }

    public fun convertStatement(statement: PsiStatement?): Statement {
        if (statement == null) return Statement.Empty

        val statementVisitor: StatementVisitor = StatementVisitor(this)
        statement.accept(statementVisitor)
        return statementVisitor.result
    }

    public fun convertExpressions(expressions: Array<PsiExpression>): List<Expression>
            = expressions.map { convertExpression(it) }

    public fun convertExpression(expression: PsiExpression?): Expression {
        if (expression == null) return Expression.Empty

        val expressionVisitor = dispatcher.expressionVisitor
        expression.accept(expressionVisitor)
        return expressionVisitor.result
    }

    public fun convertElement(element: PsiElement?): Element {
        if (element == null) return Element.Empty

        val elementVisitor = ElementVisitor(this)
        element.accept(elementVisitor)
        return elementVisitor.result
    }

    fun convertElements(elements: Array<out PsiElement?>): List<Element> {
        val result = ArrayList<Element>()
        for (element in elements) {
            result.add(convertElement(element))
        }
        return result
    }

    public fun convertTypeElement(element: PsiTypeElement?): TypeElement {
        return TypeElement(if (element == null)
                               EmptyType()
                           else
                               convertType(element.getType()))
    }

    public fun convertType(`type`: PsiType?): Type {
        if (`type` == null) return EmptyType()

        return `type`.accept<Type>(TypeVisitor(this))!!
    }

    public fun convertTypes(types: Array<PsiType>): List<Type> {
        return types.map { convertType(it) }
    }

    public fun convertType(`type`: PsiType?, notNull: Boolean): Type {
        val result = convertType(`type`)
        if (notNull) {
            return result.convertedToNotNull()
        }

        return result
    }

    private fun convertToNotNullableTypes(types: Array<out PsiType?>): List<Type>
            = types.map { convertType(it).convertedToNotNull() }

    public fun convertParameterList(parameters: Array<PsiParameter>): List<Parameter>
            = parameters.map { convertParameter(it) }

    public fun convertParameter(parameter: PsiParameter, forceNotNull: Boolean = false): Parameter {
        return Parameter(Identifier(parameter.getName()!!),
                         convertType(parameter.getType(),
                                     forceNotNull || isAnnotatedAsNotNull(parameter.getModifierList())), true)
    }

    public fun convertArguments(expression: PsiCallExpression): List<Expression> {
        val arguments = expression.getArgumentList()?.getExpressions() ?: array()
        val resolved = expression.resolveMethod()
        val expectedTypes = if (resolved != null)
            resolved.getParameterList().getParameters().map { it.getType() }
        else
            listOf()

        return if (arguments.size == expectedTypes.size())
            (0..expectedTypes.lastIndex).map { i -> convertExpression(arguments[i], expectedTypes[i]) }
        else
            arguments.map { convertExpression(it) }
    }

    public fun convertExpression(argument: PsiExpression?, expectedType: PsiType?): Expression {
        if (argument == null) return Identifier.Empty

        var expression = convertExpression(argument)
        val actualType = argument.getType()
        val isPrimitiveTypeOrNull = actualType == null || actualType is PsiPrimitiveType
        if (isPrimitiveTypeOrNull && expression.isNullable) {
            expression = BangBangExpression(expression)
        }
        else if (expectedType is PsiPrimitiveType && actualType is PsiClassType) {
            if (PsiPrimitiveType.getUnboxedType(actualType) == expectedType) {
                expression = BangBangExpression(expression)
            }
        }

        if (actualType != null) {
            if (isConversionNeeded(actualType, expectedType) && expression !is LiteralExpression) {
                val conversion = PRIMITIVE_TYPE_CONVERSIONS[expectedType?.getCanonicalText()]
                if (conversion != null) {
                    expression = MethodCallExpression.build(expression, conversion)
                }
            }

        }

        return expression
    }

    private fun createParametersFromFields(fields: List<Field>): List<Parameter> {
        return fields.map { Parameter(Identifier("_" + it.identifier.name), it.`type`, true) }
    }

    private fun createInitStatementsFromFields(fields: List<Field>): List<Statement> {
        val result = ArrayList<Statement>()
        for (field in fields) {
            val kotlinIdentifier = field.identifier.toKotlin()
            result.add(DummyStringExpression(kotlinIdentifier + " = " + "_" + kotlinIdentifier))
        }
        return result
    }

    private fun createPrimaryConstructorInvocation(s: String, fields: List<Field>, initializers: Map<String, String>): String {
        return s + "(" + fields.map { initializers[it.identifier.toKotlin()] }.makeString(", ") + ")"
    }

    public fun convertIdentifier(identifier: PsiIdentifier?): Identifier {
        if (identifier == null) return Identifier.Empty

        return Identifier(identifier.getText()!!)
    }

    public fun convertModifierList(modifierList: PsiModifierList?): Set<Modifier> {
        if (modifierList == null) return setOf()

        val modifiersSet = HashSet<Modifier>()

        if (modifierList.hasExplicitModifier(PsiModifier.ABSTRACT))
            modifiersSet.add(Modifier.ABSTRACT)

        if (modifierList.hasModifierProperty(PsiModifier.FINAL))
            modifiersSet.add(Modifier.FINAL)

        if (modifierList.hasModifierProperty(PsiModifier.STATIC))
            modifiersSet.add(Modifier.STATIC)

        if (modifierList.hasExplicitModifier(PsiModifier.PUBLIC))
            modifiersSet.add(Modifier.PUBLIC)

        if (modifierList.hasExplicitModifier(PsiModifier.PROTECTED))
            modifiersSet.add(Modifier.PROTECTED)

        if (modifierList.hasExplicitModifier(PsiModifier.PACKAGE_LOCAL))
            modifiersSet.add(Modifier.INTERNAL)

        if (modifierList.hasExplicitModifier(PsiModifier.PRIVATE))
            modifiersSet.add(Modifier.PRIVATE)

        return modifiersSet
    }

    private val TYPE_MAP: Map<String, String> = mapOf(
            JAVA_LANG_BYTE to "byte",
            JAVA_LANG_SHORT to "short",
            JAVA_LANG_INTEGER to "int",
            JAVA_LANG_LONG to "long",
            JAVA_LANG_FLOAT to "float",
            JAVA_LANG_DOUBLE to "double",
            JAVA_LANG_CHARACTER to "char"
    )

    private fun isConversionNeeded(actual: PsiType?, expected: PsiType?): Boolean {
        if (actual == null || expected == null) return false

        val expectedStr = expected.getCanonicalText()
        val actualStr = actual.getCanonicalText()
        if (expectedStr == actualStr) return false
        val o1 = expectedStr == TYPE_MAP[actualStr]
        val o2 = actualStr == TYPE_MAP[expectedStr]
        return o1 == o2
    }
}

val NOT_NULL_ANNOTATIONS: Set<String> = setOf("org.jetbrains.annotations.NotNull", "com.sun.istack.internal.NotNull", "javax.annotation.Nonnull")

val PRIMITIVE_TYPE_CONVERSIONS: Map<String, String> = mapOf(
        "byte" to BYTE.asString(),
        "short" to SHORT.asString(),
        "int" to INT.asString(),
        "long" to LONG.asString(),
        "float" to FLOAT.asString(),
        "double" to DOUBLE.asString(),
        "char" to CHAR.asString(),
        JAVA_LANG_BYTE to BYTE.asString(),
        JAVA_LANG_SHORT to SHORT.asString(),
        JAVA_LANG_INTEGER to INT.asString(),
        JAVA_LANG_LONG to LONG.asString(),
        JAVA_LANG_FLOAT to FLOAT.asString(),
        JAVA_LANG_DOUBLE to DOUBLE.asString(),
        JAVA_LANG_CHARACTER to CHAR.asString()
)
