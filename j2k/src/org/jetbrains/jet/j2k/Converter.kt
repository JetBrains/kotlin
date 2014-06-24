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
import com.intellij.psi.util.PsiUtil

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

    val typeConverter: TypeConverter get() = state.typeConverter
    val methodReturnType: PsiType? get() = state.methodReturnType

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

        val primaryConstructor = convertedMembers.values().filterIsInstance(javaClass<PrimaryConstructor>()).firstOrNull()
        val secondaryConstructors = convertedMembers.values().filterIsInstance(javaClass<SecondaryConstructor>())

        val useClassObject = shouldGenerateClassObject(psiClass, convertedMembers)

        val normalMembers = ArrayList<Member>()
        val classObjectMembers = ArrayList<Member>()
        for ((psiMember, member) in convertedMembers) {
            if (member is Constructor) continue
            if (useClassObject && psiMember !is PsiClass && psiMember.hasModifierProperty(PsiModifier.STATIC)) {
                classObjectMembers.add(member)
            }
            else {
                normalMembers.add(member)
            }
        }

        val lBrace = LBrace().assignPrototype(psiClass.getLBrace())
        val rBrace = RBrace().assignPrototype(psiClass.getRBrace())
        return ClassBody(primaryConstructor, secondaryConstructors, normalMembers, classObjectMembers, lBrace, rBrace)
    }

    // do not convert private static methods into class object if possible
    private fun shouldGenerateClassObject(psiClass: PsiClass, convertedMembers: Map<PsiMember, Member>): Boolean {
        if (psiClass.isEnum()) return false
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
                if (psiClass.getPrimaryConstructor() == null && psiClass.getConstructors().size > 1) {
                    classBody = generateArtificialPrimaryConstructor(name, classBody)
                }

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

    private fun generateArtificialPrimaryConstructor(className: Identifier, classBody: ClassBody): ClassBody {
        assert(classBody.primaryConstructor == null)

        val finalOrWithEmptyInitializerFields = classBody.normalMembers.filterIsInstance(javaClass<Field>()).filter { it.isVal || it.initializer.isEmpty }
        val initializers = HashMap<Field, Expression>()
        for (constructor in classBody.secondaryConstructors) {
            for (field in finalOrWithEmptyInitializerFields) {
                initializers.put(field, getDefaultInitializer(field))
            }

            val newStatements = ArrayList<Statement>()
            for (statement in constructor.block!!.statements) {
                var keepStatement = true
                if (statement is AssignmentExpression) {
                    val assignee = statement.left
                    if (assignee is QualifiedExpression && (assignee.qualifier as? Identifier)?.name == SecondaryConstructor.tempValIdentifier.name) {
                        val name = (assignee.identifier as Identifier).name
                        for (field in finalOrWithEmptyInitializerFields) {
                            if (name == field.identifier.name) {
                                initializers.put(field, statement.right)
                                keepStatement = false
                            }

                        }
                    }

                }

                if (keepStatement) {
                    newStatements.add(statement)
                }

            }

            val initializer = MethodCallExpression.buildNotNull(null, className.name, finalOrWithEmptyInitializerFields.map { initializers[it]!! })
            val localVar = LocalVariable(SecondaryConstructor.tempValIdentifier,
                                         Annotations.Empty,
                                         Modifiers.Empty,
                                         { ClassType(className, listOf(), Nullability.NotNull, settings) },
                                         initializer,
                                         true,
                                         settings)
            newStatements.add(0, DeclarationStatement(listOf(localVar)))
            constructor.block = Block(newStatements, LBrace(), RBrace())
        }

        //TODO: comments?
        val parameters = finalOrWithEmptyInitializerFields.map { field ->
            val varValModifier = if (field.isVal) Parameter.VarValModifier.Val else Parameter.VarValModifier.Var
            Parameter(field.identifier, field.`type`, varValModifier, field.annotations, field.modifiers.filter { it in ACCESS_MODIFIERS })
        }

        val primaryConstructor = PrimaryConstructor(this, Annotations.Empty, Modifiers(listOf(Modifier.PRIVATE)), ParameterList(parameters), Block.Empty)
        val updatedMembers = classBody.normalMembers.filter { !finalOrWithEmptyInitializerFields.contains(it) }
        return ClassBody(primaryConstructor, classBody.secondaryConstructors, updatedMembers, classBody.classObjectMembers, classBody.lBrace, classBody.rBrace)
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
            val initializer = field.getInitializer()
            val convertedType = typeConverter.convertVariableType(field)
            val isVal = isVal(field)
            val omitType = !settings.specifyFieldTypeByDefault &&
                    initializer != null &&
                    (modifiers.isPrivate && (isVal || convertedType == typeConverter.convertExpressionType(initializer)) ||
                            modifiers.isInternal && convertedType == typeConverter.convertExpressionType(initializer))
            Field(name,
                  annotations,
                  modifiers,
                  convertedType,
                  convertExpression(initializer, field.getType()),
                  isVal,
                  !omitType,
                  field.hasWriteAccesses(field.getContainingClass()))
        }
        return converted.assignPrototype(field)
    }

    private fun isVal(field: PsiField): Boolean {
        if (field.hasModifierProperty(PsiModifier.FINAL)) return true
        if (!field.hasModifierProperty(PsiModifier.PRIVATE)) return false
        val containingClass = field.getContainingClass() ?: return false
        val writes = findVariableUsages(field, containingClass).filter { PsiUtil.isAccessedForWriting(it) }
        if (writes.size == 0) return true
        if (writes.size > 1) return false
        val write = writes.single()
        val parent = write.getParent()
        if (parent is PsiAssignmentExpression &&
                parent.getOperationSign().getTokenType() == JavaTokenType.EQ &&
                isQualifierEmptyOrThis(write)) {
            val constructor = write.getContainingConstructor()
            return constructor != null &&
                    constructor.getContainingClass() == containingClass &&
                    parent.getParent() is PsiExpressionStatement &&
                    parent.getParent()?.getParent() == constructor.getBody()
        }
        return false
    }

    private fun convertMethod(method: PsiMethod, membersToRemove: MutableSet<PsiMember>): Function {
        return withMethodReturnType(method.getReturnType()).doConvertMethod(method, membersToRemove).assignPrototype(method)
    }

    private fun doConvertMethod(method: PsiMethod, membersToRemove: MutableSet<PsiMember>): Function {
        val returnType = typeConverter.convertMethodReturnType(method)

        val annotations = convertAnnotations(method) + convertThrows(method)
        var modifiers = convertModifiers(method)

        if (method.isConstructor()) {
            if (method.isPrimaryConstructor()) {
                return convertPrimaryConstructor(method, annotations, modifiers, membersToRemove)
            }
            else {
                val params = convertParameterList(method.getParameterList())
                return SecondaryConstructor(this, annotations, modifiers, params, convertBlock(method.getBody()))
            }
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
            val block = convertBlock(method.getBody())
            return Function(this, method.declarationIdentifier(), annotations, modifiers, returnType, typeParameterList, params, block, containingClass?.isInterface() ?: false)
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

    private fun convertPrimaryConstructor(constructor: PsiMethod,
                                          annotations: Annotations,
                                          modifiers: Modifiers,
                                          membersToRemove: MutableSet<PsiMember>): PrimaryConstructor {
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

            withExpressionVisitor { ExpressionVisitor(it, usageReplacementMap) }.convertBlock(body, false, { !statementsToRemove.contains(it) })
        }
        else {
            Block.Empty
        }

        val parameterList = ParameterList(params.map { parameter ->
            if (!parameterToField.containsKey(parameter)) {
                convertParameter(parameter)
            }
            else {
                val (field, `type`) = parameterToField[parameter]!!
                Parameter(field.declarationIdentifier(),
                          `type`,
                          if (isVal(field)) Parameter.VarValModifier.Val else Parameter.VarValModifier.Var,
                          convertAnnotations(parameter) + convertAnnotations(field),
                          convertModifiers(field).filter { it in ACCESS_MODIFIERS }).assignPrototypes(listOf(parameter, field), inheritBlankLinesBefore = false)
            }
        })
        return PrimaryConstructor(this, annotations, modifiers, parameterList, block).assignPrototype(constructor)
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
            = TypeElement(if (element == null) Type.Empty else typeConverter.convertType(element.getType())).assignPrototype(element)

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

    fun convertExpression(argument: PsiExpression?, expectedType: PsiType?): Expression {
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
                    expression = MethodCallExpression.buildNotNull(expression, conversion)
                }
            }

        }

        return expression.assignPrototype(argument)
    }

    fun convertIdentifier(identifier: PsiIdentifier?): Identifier {
        if (identifier == null) return Identifier.Empty

        return Identifier(identifier.getText()!!).assignPrototype(identifier)
    }

    fun convertModifiers(owner: PsiModifierListOwner): Modifiers
            = Modifiers(MODIFIERS_MAP.filter { owner.hasModifierProperty(it.first) }.map { it.second }).assignPrototype(owner.getModifierList(), false)

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
        return Annotations(list, newLines)
    }

    private fun convertAnnotation(annotation: PsiAnnotation, brackets: Boolean): Annotation? {
        val qualifiedName = annotation.getQualifiedName()
        if (qualifiedName == CommonClassNames.JAVA_LANG_DEPRECATED && annotation.getParameterList().getAttributes().isEmpty()) {
            return Annotation(Identifier("deprecated"), listOf(null to LiteralExpression("\"\"")), brackets).assignPrototype(annotation) //TODO: insert comment
        }

        val nameRef = annotation.getNameReferenceElement()
        val name = Identifier((nameRef ?: return null).getText()!!).assignPrototype(nameRef)
        val annotationClass = nameRef!!.resolve() as? PsiClass
        val lastMethod = annotationClass?.getMethods()?.lastOrNull()
        val arguments = annotation.getParameterList().getAttributes().flatMap {
            val method = annotationClass?.findMethodsByName(it.getName() ?: "value", false)?.firstOrNull()
            val expectedType = method?.getReturnType()

            val attrName = it.getName()?.let { Identifier(it) }
            val value = it.getValue()

            val isVarArg = method == lastMethod /* converted to vararg in Kotlin */
            val attrValues = convertAttributeValue(value, expectedType, isVarArg, it.getName() == null)

            attrValues.map { attrName to it }
        }
        return Annotation(name, arguments, brackets).assignPrototype(annotation)
    }

    private fun convertAttributeValue(value: PsiAnnotationMemberValue?, expectedType: PsiType?, isVararg: Boolean, isUnnamed: Boolean): List<Expression> {
        return when (value) {
            is PsiExpression -> listOf(convertExpression(value as? PsiExpression, expectedType))

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
                        listOf(if (isVararg) StarExpression(array) else array)
                    }
                    else {
                        listOf(DummyStringExpression(value.getText()!!))
                    }
                }
            }

            else -> listOf(DummyStringExpression(value?.getText() ?: ""))
        }
    }

    private fun convertThrows(method: PsiMethod): Annotations {
        val throwsList = method.getThrowsList()
        val types = throwsList.getReferencedTypes()
        if (types.isEmpty()) return Annotations.Empty
        val annotation = Annotation(Identifier("throws"),
                                    types.map { null to MethodCallExpression.buildNotNull(null, "javaClass", listOf(), listOf(typeConverter.convertType(it, Nullability.NotNull))) },
                                    false)
        return Annotations(listOf(annotation.assignPrototype(throwsList)),
                           true)
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
