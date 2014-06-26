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
import org.jetbrains.jet.j2k.visitors.*
import java.util.*
import com.intellij.psi.CommonClassNames.*
import org.jetbrains.jet.lang.types.expressions.OperatorConventions.*
import com.intellij.openapi.project.Project

public trait ConversionScope {
    public fun contains(element: PsiElement): Boolean
}

public class FilesConversionScope(val files: Collection<PsiJavaFile>) : ConversionScope {
    override fun contains(element: PsiElement) = files.any { element.getContainingFile() == it }
}

public class Converter private(val project: Project, val settings: ConverterSettings, val conversionScope: ConversionScope, val state: Converter.State) {
    private class State(val typeConverter: TypeConverter,
                        val methodReturnType: PsiType?,
                        val expressionVisitorFactory: (Converter) -> ExpressionVisitor,
                        val statementVisitorFactory: (Converter) -> StatementVisitor)
    val typeConverter: TypeConverter = state.typeConverter
    val methodReturnType: PsiType? = state.methodReturnType

    private val constructorConverter = ConstructorConverter(this)
    private val expressionVisitor = state.expressionVisitorFactory(this)
    private val statementVisitor = state.statementVisitorFactory(this)

    class object {
        public fun create(project: Project, settings: ConverterSettings, conversionScope: ConversionScope): Converter
                = Converter(project, settings, conversionScope, State(TypeConverter(settings, conversionScope), null, { ExpressionVisitor(it) }, { StatementVisitor(it) }))
    }

    fun withMethodReturnType(methodReturnType: PsiType?): Converter
            = Converter(project, settings, conversionScope, State(typeConverter, methodReturnType, state.expressionVisitorFactory, state.statementVisitorFactory))

    fun withExpressionVisitor(factory: (Converter) -> ExpressionVisitor): Converter
            = Converter(project, settings, conversionScope, State(typeConverter, state.methodReturnType, factory, state.statementVisitorFactory))

    fun withStatementVisitor(factory: (Converter) -> StatementVisitor): Converter
            = Converter(project, settings, conversionScope, State(typeConverter, state.methodReturnType, state.expressionVisitorFactory, factory))

    public fun elementToKotlin(element: PsiElement): String {
        val converted = convertTopElement(element) ?: return ""
        val builder = CodeBuilder(element)
        builder.append(converted)
        return builder.result
    }

    private fun convertTopElement(element: PsiElement?): Element? = when (element) {
        is PsiJavaFile -> convertFile(element)
        is PsiClass -> convertClass(element)
        is PsiMethod -> convertMethod(element, HashSet())
        is PsiField -> convertField(element)
        is PsiStatement -> convertStatement(element)
        is PsiExpression -> convertExpression(element)
        is PsiImportList -> convertImportList(element)
        is PsiImportStatementBase -> convertImport(element, false)
        is PsiAnnotation -> convertAnnotation(element, false)
        is PsiPackageStatement -> PackageStatement(quoteKeywords(element.getPackageName() ?: "")).assignPrototype(element)
        else -> null
    }

    fun convertFile(javaFile: PsiJavaFile): File {
        var convertedChildren = javaFile.getChildren().map {
            if (it is PsiImportList) {
                val importList = convertImportList(it)
                typeConverter.importList = importList
                importList
            }
            else {
                convertTopElement(it)
            }
        }.filterNotNull()

        typeConverter.importList = null
        if (typeConverter.importsToAdd.isNotEmpty()) {
            val importList = convertedChildren.filterIsInstance(javaClass<ImportList>()).first()
            val newImportList = ImportList(importList.imports + typeConverter.importsToAdd).assignPrototypesFrom(importList)
            convertedChildren = convertedChildren.map { if (it == importList) newImportList else it }
        }

        return File(convertedChildren, createMainFunction(javaFile)).assignPrototype(javaFile)
    }

    fun convertAnonymousClassBody(anonymousClass: PsiAnonymousClass): AnonymousClassBody {
        return AnonymousClassBody(convertBody(anonymousClass), anonymousClass.getBaseClassType().resolve()?.isInterface() ?: false).assignPrototype(anonymousClass)
    }

    private fun convertBody(psiClass: PsiClass): ClassBody {
        val membersToRemove = HashSet<PsiMember>()
        val convertedMembers = LinkedHashMap<PsiMember, Member>()
        for (element in psiClass.getChildren()) {
            if (element is PsiMember) {
                val converted = convertMember(element, membersToRemove)
                if (!converted.isEmpty) {
                    convertedMembers.put(element, converted)
                }
            }
        }

        for (member in membersToRemove) {
            convertedMembers.remove(member)
        }

        val useClassObject = shouldGenerateClassObject(psiClass, convertedMembers)

        val members = ArrayList<Member>()
        val classObjectMembers = ArrayList<Member>()
        var primaryConstructorSignature: PrimaryConstructorSignature? = null
        for ((psiMember, member) in convertedMembers) {
            if (member is PrimaryConstructor) {
                assert(primaryConstructorSignature == null)
                primaryConstructorSignature = member.signature()
                val initializer = member.initializer()
                if (initializer != null) {
                    members.add(initializer)
                }
            }
            else if (useClassObject && psiMember !is PsiClass && psiMember.hasModifierProperty(PsiModifier.STATIC) ||
                    member is FactoryFunction) {
                classObjectMembers.add(member)
            }
            else {
                members.add(member)
            }
        }

        val lBrace = LBrace().assignPrototype(psiClass.getLBrace())
        val rBrace = RBrace().assignPrototype(psiClass.getRBrace())
        return ClassBody(primaryConstructorSignature, members, classObjectMembers, lBrace, rBrace)
    }

    // do not convert private static methods into class object if possible
    private fun shouldGenerateClassObject(psiClass: PsiClass, convertedMembers: Map<PsiMember, Member>): Boolean {
        if (psiClass.isEnum()) return false
        if (convertedMembers.values().any { it is FactoryFunction }) return true
        val members = convertedMembers.keySet().filter { !it.isConstructor() }
        val classObjectMembers = members.filter { it !is PsiClass && it.hasModifierProperty(PsiModifier.STATIC) }
        val nestedClasses = members.filterIsInstance(javaClass<PsiClass>()).filter { it.hasModifierProperty(PsiModifier.STATIC) }
        if (classObjectMembers.all { it is PsiMethod && it.hasModifierProperty(PsiModifier.PRIVATE) }) {
            return nestedClasses.any { nestedClass -> classObjectMembers.any { findMethodCalls(it as PsiMethod, nestedClass).isNotEmpty() } }
        }
        else {
            return true
        }
    }

    private fun convertMember(member: PsiMember, membersToRemove: MutableSet<PsiMember>): Member = when (member) {
        is PsiMethod -> convertMethod(member, membersToRemove)
        is PsiField -> convertField(member)
        is PsiClass -> convertClass(member)
        is PsiClassInitializer -> convertInitializer(member)
        else -> throw IllegalArgumentException("Unknown member: $member")
    }

    private fun convertClass(psiClass: PsiClass): Class {
        val annotations = convertAnnotations(psiClass)
        var modifiers = convertModifiers(psiClass)
        val typeParameters = convertTypeParameterList(psiClass.getTypeParameterList())
        val implementsTypes = convertToNotNullableTypes(psiClass.getImplementsListTypes())
        val extendsTypes = convertToNotNullableTypes(psiClass.getExtendsListTypes())
        val name = psiClass.declarationIdentifier()
        var classBody = convertBody(psiClass)

        return when {
            psiClass.isInterface() -> Trait(name, annotations, modifiers, typeParameters, extendsTypes, listOf(), implementsTypes, classBody)

            psiClass.isEnum() -> Enum(name, annotations, modifiers, typeParameters, listOf(), listOf(), implementsTypes, classBody)

            else -> {
                classBody = constructorConverter.postProcessConstructors(classBody, psiClass)

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

                if (settings.openByDefault && !psiClass.hasModifierProperty(PsiModifier.FINAL)) {
                    modifiers = modifiers.with(Modifier.OPEN)
                }

                if (psiClass.getContainingClass() != null && !psiClass.hasModifierProperty(PsiModifier.STATIC)) {
                    modifiers = modifiers.with(Modifier.INNER)
                }

                Class(name, annotations, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, classBody)
            }
        }.assignPrototype(psiClass)
    }

    private fun convertInitializer(initializer: PsiClassInitializer): Initializer {
        return Initializer(convertBlock(initializer.getBody()), convertModifiers(initializer)).assignPrototype(initializer)
    }

    private fun convertField(field: PsiField): Member {
        val annotations = convertAnnotations(field)
        val modifiers = convertModifiers(field)
        val name = field.declarationIdentifier()
        val converted = if (field is PsiEnumConstant) {
            EnumConstant(name,
                         annotations,
                         modifiers,
                         typeConverter.convertType(field.getType(), Nullability.NotNull),
                         convertElement(field.getArgumentList()))
        }
        else {
            val isVal = isVal(field)
            val typeToDeclare = variableTypeToDeclare(field,
                                                      settings.specifyFieldTypeByDefault || modifiers.isPublic || modifiers.isProtected,
                                                      isVal && modifiers.isPrivate)
            val initializer = convertExpression(field.getInitializer(), field.getType())
            Field(name,
                  annotations,
                  modifiers,
                  typeToDeclare ?: typeConverter.convertVariableType(field),
                  initializer,
                  isVal,
                  typeToDeclare != null,
                  initializer.isEmpty && shouldGenerateDefaultInitializer(field))
        }
        return converted.assignPrototype(field)
    }

    fun variableTypeToDeclare(variable: PsiVariable, specifyAlways: Boolean, canChangeType: Boolean): Type? {
        fun convertType() = typeConverter.convertVariableType(variable)

        if (specifyAlways) return convertType()

        val initializer = variable.getInitializer()
        if (initializer == null) return convertType()
        if (initializer is PsiLiteralExpression && initializer.getType() == PsiType.NULL) return convertType()

        if (canChangeType) return null

        val convertedType = convertType()
        var initializerType = convertedExpressionType(initializer, variable.getType())
        if (initializerType is ErrorType) return null // do not add explicit type when initializer is not resolved, let user add it if really needed
        return if (convertedType == initializerType) null else convertedType
    }

    private fun convertMethod(method: PsiMethod, membersToRemove: MutableSet<PsiMember>): Member {
        return withMethodReturnType(method.getReturnType()).doConvertMethod(method, membersToRemove).assignPrototype(method)
    }

    private fun doConvertMethod(method: PsiMethod, membersToRemove: MutableSet<PsiMember>): Member {
        val returnType = typeConverter.convertMethodReturnType(method)

        val annotations = (convertAnnotations(method) + convertThrows(method)).assignNoPrototype()
        var modifiers = convertModifiers(method)

        val statementsToInsert = ArrayList<Statement>()
        for (parameter in method.getParameterList().getParameters()) {
            if (parameter.hasWriteAccesses(method)) {
                val variable = LocalVariable(parameter.declarationIdentifier(),
                                             Annotations.Empty,
                                             Modifiers.Empty,
                                             null,
                                             parameter.declarationIdentifier(),
                                             false).assignNoPrototype()
                statementsToInsert.add(DeclarationStatement(listOf(variable)).assignNoPrototype())
            }
        }
        val postProcessBody: (Block) -> Block = { body ->
            if (statementsToInsert.isEmpty()) {
                body
            }
            else {
                Block(statementsToInsert + body.statements, body.lBrace, body.rBrace).assignPrototypesFrom(body)
            }
        }

        if (method.isConstructor()) {
            return constructorConverter.convertConstructor(method, annotations, modifiers, membersToRemove, postProcessBody)
        }
        else {
            val isOverride = isOverride(method)
            if (isOverride) {
                modifiers = modifiers.with(Modifier.OVERRIDE)
            }

            val containingClass = method.getContainingClass()

            if (settings.openByDefault) {
                val isEffectivelyFinal = method.hasModifierProperty(PsiModifier.FINAL) ||
                        containingClass != null && (containingClass.hasModifierProperty(PsiModifier.FINAL) || containingClass.isEnum())
                if (!isEffectivelyFinal && !modifiers.contains(Modifier.ABSTRACT) && !modifiers.isPrivate) {
                    modifiers = modifiers.with(Modifier.OPEN)
                }
            }

            var params = convertParameterList(method.getParameterList())
            val typeParameterList = convertTypeParameterList(method.getTypeParameterList())
            var body = postProcessBody(convertBlock(method.getBody()))
            return Function(method.declarationIdentifier(), annotations, modifiers, returnType, typeParameterList, params, body, containingClass?.isInterface() ?: false)
        }
    }

    /**
     * Overrides of methods from Object should not be marked as overrides in Kotlin unless the class itself has java ancestors
     */
    private fun isOverride(method: PsiMethod): Boolean {
        val superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures()

        val overridesMethodNotFromObject = superSignatures.any {
            it.getMethod().getContainingClass()?.getQualifiedName() != JAVA_LANG_OBJECT
        }
        if (overridesMethodNotFromObject) return true

        val overridesMethodFromObject = superSignatures.any {
            it.getMethod().getContainingClass()?.getQualifiedName() == JAVA_LANG_OBJECT
        }
        if (overridesMethodFromObject) {
            when (method.getName()) {
                "equals", "hashCode", "toString" -> return true // these methods from java.lang.Object exist in kotlin.Any

                else -> {
                    val containing = method.getContainingClass()
                    if (containing != null) {
                        val hasOtherJavaSuperclasses = containing.getSuperTypes().any {
                            //TODO: correctly check for kotlin class
                            val `class` = it.resolve()
                            `class` != null && `class`.getQualifiedName() != JAVA_LANG_OBJECT && !conversionScope.contains(`class`)
                        }
                        if (hasOtherJavaSuperclasses) return true
                    }
                }
            }
        }

        return false
    }

    fun convertBlock(block: PsiCodeBlock?, notEmpty: Boolean = true, statementFilter: (PsiStatement) -> Boolean = { true }): Block {
        if (block == null) return Block.Empty

        val lBrace = LBrace().assignPrototype(block.getLBrace())
        val rBrace = RBrace().assignPrototype(block.getRBrace())
        return Block(block.getStatements().filter(statementFilter).map { convertStatement(it) }, lBrace, rBrace, notEmpty).assignPrototype(block)
    }

    fun convertStatement(statement: PsiStatement?): Statement {
        if (statement == null) return Statement.Empty

        statementVisitor.reset()
        statement.accept(statementVisitor)
        return statementVisitor.result.assignPrototype(statement)
    }

    fun convertExpressions(expressions: Array<PsiExpression>): List<Expression>
            = expressions.map { convertExpression(it) }

    fun convertExpression(expression: PsiExpression?): Expression {
        if (expression == null) return Expression.Empty

        expressionVisitor.reset()
        expression.accept(expressionVisitor)
        return expressionVisitor.result.assignPrototype(expression)
    }

    fun convertElement(element: PsiElement?): Element {
        if (element == null) return Element.Empty

        val elementVisitor = ElementVisitor(this)
        element.accept(elementVisitor)
        return elementVisitor.result.assignPrototype(element)
    }

    fun convertTypeElement(element: PsiTypeElement?): TypeElement
            = TypeElement(if (element == null) ErrorType().assignNoPrototype() else typeConverter.convertType(element.getType())).assignPrototype(element)

    private fun convertToNotNullableTypes(types: Array<out PsiType?>): List<Type>
            = types.map { typeConverter.convertType(it, Nullability.NotNull) }

    fun convertParameterList(parameterList: PsiParameterList): ParameterList
            = ParameterList(parameterList.getParameters().map { convertParameter(it) }).assignPrototype(parameterList)

    fun convertParameter(parameter: PsiParameter,
                         nullability: Nullability = Nullability.Default,
                         varValModifier: Parameter.VarValModifier = Parameter.VarValModifier.None,
                         modifiers: Modifiers = Modifiers.Empty): Parameter {
        var `type` = typeConverter.convertVariableType(parameter)
        when (nullability) {
            Nullability.NotNull -> `type` = `type`.toNotNullType()
            Nullability.Nullable -> `type` = `type`.toNullableType()
        }
        return Parameter(parameter.declarationIdentifier(), `type`, varValModifier, convertAnnotations(parameter), modifiers).assignPrototype(parameter)
    }

    fun convertExpression(expression: PsiExpression?, expectedType: PsiType?): Expression {
        if (expression == null) return Identifier.Empty

        var convertedExpression = convertExpression(expression)
        if (expectedType == null || expectedType == PsiType.VOID) return convertedExpression

        val actualType = expression.getType()
        if (actualType == null) return convertedExpression

        if (convertedExpression.isNullable &&
                (actualType is PsiPrimitiveType || actualType is PsiClassType && expectedType is PsiPrimitiveType)) {
            convertedExpression = BangBangExpression(convertedExpression).assignNoPrototype()
        }

        if (canConvertType(actualType, expectedType) && convertedExpression !is LiteralExpression) {
            val conversion = PRIMITIVE_TYPE_CONVERSIONS[expectedType.getCanonicalText()]
            if (conversion != null) {
                convertedExpression = MethodCallExpression.buildNotNull(convertedExpression, conversion)
            }
        }

        return convertedExpression.assignPrototype(expression)
    }

    fun convertedExpressionType(expression: PsiExpression, expectedType: PsiType): Type {
        var convertedExpression = convertExpression(expression)
        val actualType = expression.getType()
        if (actualType == null) return ErrorType()
        var resultType = typeConverter.convertType(actualType, if (convertedExpression.isNullable) Nullability.Nullable else Nullability.NotNull)

        if (actualType is PsiPrimitiveType && resultType.isNullable ||
                expectedType is PsiPrimitiveType && actualType is PsiClassType) {
            resultType = resultType.toNotNullType()
        }

        if (canConvertType(actualType, expectedType) && convertedExpression !is LiteralExpression) {
            val conversion = PRIMITIVE_TYPE_CONVERSIONS[expectedType.getCanonicalText()]
            if (conversion != null) {
                resultType = typeConverter.convertType(expectedType, Nullability.NotNull)
            }
        }

        return resultType
    }

    private fun canConvertType(actual: PsiType, expected: PsiType): Boolean {
        val expectedStr = expected.getCanonicalText()
        val actualStr = actual.getCanonicalText()
        if (expectedStr == actualStr) return false
        val o1 = expectedStr == typeConversionMap[actualStr]
        val o2 = actualStr == typeConversionMap[expectedStr]
        return o1 == o2
    }

    private val typeConversionMap: Map<String, String> = mapOf(
            JAVA_LANG_BYTE to "byte",
            JAVA_LANG_SHORT to "short",
            JAVA_LANG_INTEGER to "int",
            JAVA_LANG_LONG to "long",
            JAVA_LANG_FLOAT to "float",
            JAVA_LANG_DOUBLE to "double",
            JAVA_LANG_CHARACTER to "char"
    )

    fun convertIdentifier(identifier: PsiIdentifier?): Identifier {
        if (identifier == null) return Identifier.Empty

        return Identifier(identifier.getText()!!).assignPrototype(identifier)
    }

    fun convertModifiers(owner: PsiModifierListOwner): Modifiers {
        return Modifiers(MODIFIERS_MAP.filter { owner.hasModifierProperty(it.first) }.map { it.second })
                .assignPrototype(owner.getModifierList(), CommentsAndSpacesInheritance(blankLinesBefore = false))
    }

    private val MODIFIERS_MAP = listOf(
            PsiModifier.ABSTRACT to Modifier.ABSTRACT,
            PsiModifier.PUBLIC to Modifier.PUBLIC,
            PsiModifier.PROTECTED to Modifier.PROTECTED,
            PsiModifier.PRIVATE to Modifier.PRIVATE
    )

    fun convertAnnotations(owner: PsiModifierListOwner): Annotations {
        val modifierList = owner.getModifierList()
        val annotations = modifierList?.getAnnotations()?.filter { it.getQualifiedName() !in ANNOTATIONS_TO_REMOVE }
        if (annotations == null || annotations.isEmpty()) return Annotations.Empty

        val newLines = run {
            if (!modifierList!!.isInSingleLine()) {
                true
            }
            else {
                var child: PsiElement? = modifierList
                while (true) {
                    child = child!!.getNextSibling()
                    if (child == null || child!!.getTextLength() != 0) break
                }
                if (child is PsiWhiteSpace) !child!!.isInSingleLine() else false
            }
        }

        val list = annotations.map { convertAnnotation(it, owner is PsiLocalVariable) }.filterNotNull() //TODO: brackets are also needed for local classes
        return Annotations(list, newLines).assignNoPrototype()
    }

    private fun convertAnnotation(annotation: PsiAnnotation, brackets: Boolean): Annotation? {
        val qualifiedName = annotation.getQualifiedName()
        if (qualifiedName == CommonClassNames.JAVA_LANG_DEPRECATED && annotation.getParameterList().getAttributes().isEmpty()) {
            return Annotation(Identifier("deprecated").assignNoPrototype(), listOf(null to LiteralExpression("\"\"").assignNoPrototype()), brackets).assignPrototype(annotation) //TODO: insert comment
        }

        val nameRef = annotation.getNameReferenceElement()
        val name = Identifier((nameRef ?: return null).getText()!!).assignPrototype(nameRef)
        val annotationClass = nameRef!!.resolve() as? PsiClass
        val lastMethod = annotationClass?.getMethods()?.lastOrNull()
        val arguments = annotation.getParameterList().getAttributes().flatMap {
            val method = annotationClass?.findMethodsByName(it.getName() ?: "value", false)?.firstOrNull()
            val expectedType = method?.getReturnType()

            val attrName = it.getName()?.let { Identifier(it).assignNoPrototype() }
            val value = it.getValue()

            val isVarArg = method == lastMethod /* converted to vararg in Kotlin */
            val attrValues = convertAttributeValue(value, expectedType, isVarArg, it.getName() == null)

            attrValues.map { attrName to it }
        }
        return Annotation(name, arguments, brackets).assignPrototype(annotation)
    }

    private fun convertAttributeValue(value: PsiAnnotationMemberValue?, expectedType: PsiType?, isVararg: Boolean, isUnnamed: Boolean): List<Expression> {
        return when (value) {
            is PsiExpression -> listOf(convertExpression(value as? PsiExpression, expectedType).assignPrototype(value))

            is PsiArrayInitializerMemberValue -> {
                val componentType = (expectedType as? PsiArrayType)?.getComponentType()
                val componentsConverted = value.getInitializers().map { convertAttributeValue(it, componentType, false, true).single() }
                if (isVararg && isUnnamed) {
                    componentsConverted
                }
                else {
                    val expectedTypeConverted = typeConverter.convertType(expectedType)
                    if (expectedTypeConverted is ArrayType) {
                        val array = createArrayInitializerExpression(expectedTypeConverted, componentsConverted, needExplicitType = false)
                        if (isVararg) {
                            listOf(StarExpression(array.assignNoPrototype()).assignPrototype(value))
                        }
                        else {
                            listOf(array.assignPrototype(value))
                        }
                    }
                    else {
                        listOf(DummyStringExpression(value.getText()!!).assignPrototype(value))
                    }
                }
            }

            else -> listOf(DummyStringExpression(value?.getText() ?: "").assignPrototype(value))
        }
    }

    private fun convertThrows(method: PsiMethod): Annotations {
        val throwsList = method.getThrowsList()
        val types = throwsList.getReferencedTypes()
        val refElements = throwsList.getReferenceElements()
        assert(types.size == refElements.size)
        if (types.isEmpty()) return Annotations.Empty
        val arguments = types.indices.map {
            val convertedType = typeConverter.convertType(types[it], Nullability.NotNull)
            null to MethodCallExpression.buildNotNull(null, "javaClass", listOf(), listOf(convertedType)).assignPrototype(refElements[it])
        }
        val annotation = Annotation(Identifier("throws").assignNoPrototype(), arguments, false)
        return Annotations(listOf(annotation.assignPrototype(throwsList)), true).assignPrototype(throwsList)
    }
}

val NOT_NULL_ANNOTATIONS: Set<String> = setOf("org.jetbrains.annotations.NotNull", "com.sun.istack.internal.NotNull", "javax.annotation.Nonnull")
val NULLABLE_ANNOTATIONS: Set<String> = setOf("org.jetbrains.annotations.Nullable", "com.sun.istack.internal.Nullable", "javax.annotation.Nullable")
val ANNOTATIONS_TO_REMOVE: Set<String> = HashSet(NOT_NULL_ANNOTATIONS + NULLABLE_ANNOTATIONS + listOf(CommonClassNames.JAVA_LANG_OVERRIDE))

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
