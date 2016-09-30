/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.CommonClassNames.*
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.j2k.ast.Annotation
import org.jetbrains.kotlin.j2k.ast.Enum
import org.jetbrains.kotlin.j2k.ast.Function
import org.jetbrains.kotlin.j2k.usageProcessing.FieldToPropertyProcessing
import org.jetbrains.kotlin.j2k.usageProcessing.UsageProcessing
import org.jetbrains.kotlin.j2k.usageProcessing.UsageProcessingExpressionConverter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.expressions.OperatorConventions.*
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.lang.IllegalArgumentException
import java.util.*

class Converter private constructor(
        private val elementToConvert: PsiElement,
        val settings: ConverterSettings,
        val inConversionScope: (PsiElement) -> Boolean,
        val services: JavaToKotlinConverterServices,
        private val commonState: Converter.CommonState,
        private val personalState: Converter.PersonalState
) {

    // state which is shared between all converter's based on this one
    private class CommonState(val usageProcessingsCollector: (UsageProcessing) -> Unit) {
        val deferredElements = ArrayList<DeferredElement<*>>()
        val postUnfoldActions = ArrayList<() -> Unit>()
    }

    // state which may differ in different converter's
    class PersonalState(val specialContext: PsiElement?)

    val project: Project = elementToConvert.project
    val typeConverter: TypeConverter = TypeConverter(this)
    val annotationConverter: AnnotationConverter = AnnotationConverter(this)

    val specialContext: PsiElement? = personalState.specialContext

    val referenceSearcher: ReferenceSearcher = CachingReferenceSearcher(services.referenceSearcher)

    val propertyDetectionCache = PropertyDetectionCache(this)

    companion object {
        fun create(elementToConvert: PsiElement, settings: ConverterSettings, services: JavaToKotlinConverterServices,
                   inConversionScope: (PsiElement) -> Boolean, usageProcessingsCollector: (UsageProcessing) -> Unit): Converter {
            return Converter(elementToConvert, settings, inConversionScope,
                             services, CommonState(usageProcessingsCollector), PersonalState(null))
        }
    }

    fun withSpecialContext(context: PsiElement): Converter = withState(PersonalState(context))

    private fun withState(state: PersonalState): Converter
            = Converter(elementToConvert, settings, inConversionScope, services, commonState, state)

    private fun createDefaultCodeConverter() = CodeConverter(this, DefaultExpressionConverter(), DefaultStatementConverter(), null)

    data class IntermediateResult(
            val codeGenerator: (Map<PsiElement, Collection<UsageProcessing>>) -> Result,
            val parseContext: ParseContext
    )

    data class Result(
            val text: String,
            val importsToAdd: Set<FqName>
    )

    fun convert(): IntermediateResult? {
        val element = convertTopElement(elementToConvert) ?: return null
        val parseContext = when (elementToConvert) {
            is PsiStatement, is PsiExpression -> ParseContext.CODE_BLOCK
            else -> ParseContext.TOP_LEVEL
        }
        return IntermediateResult(
                { usageProcessings ->
                    unfoldDeferredElements(usageProcessings)

                    val builder = CodeBuilder(elementToConvert, services.docCommentConverter)
                    builder.append(element)
                    Result(builder.resultText, builder.importsToAdd)
                },
                parseContext)
    }

    private fun convertTopElement(element: PsiElement): Element? = when (element) {
        is PsiJavaFile -> convertFile(element)
        is PsiClass -> convertClass(element)
        is PsiMethod -> convertMethod(element, null, null, null, ClassKind.FINAL_CLASS)
        is PsiField -> convertProperty(PropertyInfo.fromFieldWithNoAccessors(element, this), ClassKind.FINAL_CLASS)
        is PsiStatement -> createDefaultCodeConverter().convertStatement(element)
        is PsiExpression -> createDefaultCodeConverter().convertExpression(element)
        is PsiImportList -> convertImportList(element)
        is PsiImportStatementBase -> convertImport(element, dumpConversion = true).singleOrNull()
        is PsiAnnotation -> annotationConverter.convertAnnotation(element, newLineAfter = false)
        is PsiPackageStatement -> PackageStatement(quoteKeywords(element.packageName ?: "")).assignPrototype(element)
        is PsiJavaCodeReferenceElement -> {
            if (element.parent is PsiReferenceList) {
                val factory = JavaPsiFacade.getInstance(project).elementFactory
                typeConverter.convertType(factory.createType(element), Nullability.NotNull)
            }
            else null
        }
        else -> null
    }

    private fun unfoldDeferredElements(usageProcessings: Map<PsiElement, Collection<UsageProcessing>>) {
        val codeConverter = createDefaultCodeConverter().withSpecialExpressionConverter(UsageProcessingExpressionConverter(usageProcessings))

        // we use loop with index because new deferred elements can be added during unfolding
        var i = 0
        while (i < commonState.deferredElements.size) {
            val deferredElement = commonState.deferredElements[i++]
            deferredElement.unfold(codeConverter.withConverter(this.withState(deferredElement.converterState)))
        }

        commonState.postUnfoldActions.forEach { it() }
    }

    fun <TResult : Element> deferredElement(generator: (CodeConverter) -> TResult): DeferredElement<TResult> {
        val element = DeferredElement(generator, personalState)
        commonState.deferredElements.add(element)
        return element
    }

    fun addUsageProcessing(processing: UsageProcessing) {
        commonState.usageProcessingsCollector(processing)
    }

    fun addPostUnfoldDeferredElementsAction(action: () -> Unit) {
        commonState.postUnfoldActions.add(action)
    }

    private fun convertFile(javaFile: PsiJavaFile): File {
        val convertedChildren = javaFile.children.mapNotNull { convertTopElement(it) }
        return File(convertedChildren).assignPrototype(javaFile)
    }

    fun convertAnnotations(owner: PsiModifierListOwner): Annotations
            = annotationConverter.convertAnnotations(owner)

    fun convertClass(psiClass: PsiClass): Class {
        if (psiClass.isAnnotationType) {
            return convertAnnotationType(psiClass)
        }

        val annotations = convertAnnotations(psiClass)
        var modifiers = convertModifiers(psiClass, false)
        val typeParameters = convertTypeParameterList(psiClass.typeParameterList)
        val extendsTypes = convertToNotNullableTypes(psiClass.extendsListTypes)
        val implementsTypes = convertToNotNullableTypes(psiClass.implementsListTypes)
        val name = psiClass.declarationIdentifier()

        return when {
            psiClass.isInterface -> {
                val classBody = ClassBodyConverter(psiClass, ClassKind.INTERFACE, this).convertBody()
                Interface(name, annotations, modifiers, typeParameters, extendsTypes, implementsTypes, classBody)
            }

            psiClass.isEnum -> {
                modifiers = modifiers.without(Modifier.ABSTRACT)
                val hasInheritors = psiClass.fields.any { it is PsiEnumConstant && it.initializingClass != null }
                val classBody = ClassBodyConverter(psiClass, if (hasInheritors) ClassKind.OPEN_ENUM else ClassKind.FINAL_ENUM, this).convertBody()
                Enum(name, annotations, modifiers, typeParameters, implementsTypes, classBody)
            }

            else -> {
                if (shouldConvertIntoObject(psiClass)) {
                    val classBody = ClassBodyConverter(psiClass, ClassKind.OBJECT, this).convertBody()
                    Object(name, annotations, modifiers.without(Modifier.ABSTRACT), classBody)
                }
                else {
                    if (psiClass.containingClass != null && !psiClass.hasModifierProperty(PsiModifier.STATIC)) {
                        modifiers = modifiers.with(Modifier.INNER)
                    }

                    if (needOpenModifier(psiClass)) {
                        modifiers = modifiers.with(Modifier.OPEN)
                    }

                    val isOpen = modifiers.contains(Modifier.OPEN) || modifiers.contains(Modifier.ABSTRACT)
                    val classBody = ClassBodyConverter(psiClass, if (isOpen) ClassKind.OPEN_CLASS else ClassKind.FINAL_CLASS, this).convertBody()
                    Class(name, annotations, modifiers, typeParameters, extendsTypes, classBody.baseClassParams, implementsTypes, classBody)
                }
            }
        }.assignPrototype(psiClass)
    }

    fun needOpenModifier(psiClass: PsiClass): Boolean {
        return if (psiClass.hasModifierProperty(PsiModifier.FINAL) || psiClass.hasModifierProperty(PsiModifier.ABSTRACT))
            false
        else
            settings.openByDefault || referenceSearcher.hasInheritors(psiClass)
    }

    private fun shouldConvertIntoObject(psiClass: PsiClass): Boolean {
        val methods = psiClass.methods
        val fields = psiClass.fields
        val classes = psiClass.innerClasses
        if (methods.isEmpty() && fields.isEmpty()) return false
        fun isStatic(member: PsiMember) = member.hasModifierProperty(PsiModifier.STATIC)
        if (!methods.all { isStatic(it) || it.isConstructor } || !fields.all(::isStatic) || !classes.all(::isStatic)) return false

        val constructors = psiClass.constructors
        if (constructors.size > 1) return false
        val constructor = constructors.singleOrNull()
        if (constructor != null) {
            if (!constructor.hasModifierProperty(PsiModifier.PRIVATE)) return false
            if (constructor.parameterList.parameters.isNotEmpty()) return false
            if (constructor.body?.statements?.isNotEmpty() ?: false) return false
            if (constructor.modifierList.annotations.isNotEmpty()) return false
        }

        if (psiClass.extendsListTypes.isNotEmpty() || psiClass.implementsListTypes.isNotEmpty()) return false
        if (psiClass.typeParameters.isNotEmpty()) return false

        if (referenceSearcher.hasInheritors(psiClass)) return false

        return true
    }

    private fun convertAnnotationType(psiClass: PsiClass): Class {
        val paramModifiers = Modifiers(listOf(Modifier.PUBLIC)).assignNoPrototype()
        val annotationMethods = psiClass.methods.filterIsInstance<PsiAnnotationMethod>()
        val (methodsNamedValue, otherMethods) = annotationMethods.partition { it.name == "value" }

        fun createParameter(type: Type, method: PsiAnnotationMethod): FunctionParameter {
            type.assignPrototype(method.returnTypeElement, CommentsAndSpacesInheritance.NO_SPACES)

            return FunctionParameter(method.declarationIdentifier(),
                                     type,
                                     FunctionParameter.VarValModifier.Val,
                                     convertAnnotations(method),
                                     paramModifiers,
                                     annotationConverter.convertAnnotationMethodDefault(method)).assignPrototype(method, CommentsAndSpacesInheritance.NO_SPACES)
        }

        fun convertType(psiType: PsiType?): Type {
            return typeConverter.convertType(psiType, Nullability.NotNull, inAnnotationType = true)
        }

        val parameters =
                // Argument named `value` comes first if it exists
                // Convert it as vararg if it's array
                methodsNamedValue.map { method ->
                    val returnType = method.returnType
                    val typeConverted = if (returnType is PsiArrayType)
                        VarArgType(convertType(returnType.componentType))
                    else
                        convertType(returnType)

                    createParameter(typeConverted, method)
                } +
                otherMethods.map { method -> createParameter(convertType(method.returnType), method) }

        val parameterList = ParameterList.withNoPrototype(parameters)
        val constructorSignature = if (parameterList.parameters.isNotEmpty())
            PrimaryConstructorSignature(Annotations.Empty, Modifiers.Empty, parameterList).assignNoPrototype()
        else
            null

        // to convert fields and nested types - they are not allowed in Kotlin but we convert them and let user refactor code
        var classBody = ClassBodyConverter(psiClass, ClassKind.ANNOTATION_CLASS, this).convertBody()
        classBody = ClassBody(constructorSignature, classBody.baseClassParams, classBody.members,
                              classBody.companionObjectMembers, classBody.lBrace, classBody.rBrace, classBody.classKind)

        return Class(psiClass.declarationIdentifier(),
                     convertAnnotations(psiClass),
                     convertModifiers(psiClass, false).with(Modifier.ANNOTATION).without(Modifier.ABSTRACT),
                     TypeParameterList.Empty,
                     listOf(),
                     null,
                     listOf(),
                     classBody).assignPrototype(psiClass)
    }

    fun convertInitializer(initializer: PsiClassInitializer): Initializer {
        return Initializer(deferredElement { codeConverter -> codeConverter.convertBlock(initializer.body) },
                           convertModifiers(initializer, false)).assignPrototype(initializer)
    }

    fun convertProperty(propertyInfo: PropertyInfo, classKind: ClassKind): Member {
        val field = propertyInfo.field
        val getMethod = propertyInfo.getMethod
        val setMethod = propertyInfo.setMethod

        //TODO: annotations from getter/setter?
        val annotations = field?.let { convertAnnotations(it) + specialAnnotationPropertyCases(it) } ?: Annotations.Empty

        val modifiers = propertyInfo.modifiers

        val name = propertyInfo.identifier
        if (field is PsiEnumConstant) {
            assert(getMethod == null && setMethod == null)
            val argumentList = field.argumentList
            val params = if (argumentList != null && argumentList.expressions.isNotEmpty()) {
                deferredElement { codeConverter -> codeConverter.convertArgumentList(argumentList) }
            }
            else {
                null
            }
            val body = field.initializingClass?.let { convertAnonymousClassBody(it) }
            return EnumConstant(name, annotations, modifiers, params, body)
                    .assignPrototype(field, CommentsAndSpacesInheritance.LINE_BREAKS)
        }
        else {
            val setterParameter = setMethod?.parameterList?.parameters?.single()
            val nullability = combinedNullability(field, getMethod, setterParameter)
            val mutability = combinedMutability(field, getMethod, setterParameter)

            val propertyType = typeConverter.convertType(propertyInfo.psiType, nullability, mutability)

            val shouldDeclareType = settings.specifyFieldTypeByDefault
                                    || field == null
                                    || shouldDeclareVariableType(field, propertyType, !propertyInfo.isVar && modifiers.isPrivate)

            //TODO: usage processings for converting method's to property
            if (field != null) {
                addUsageProcessing(FieldToPropertyProcessing(field, propertyInfo.name, propertyType.isNullable,
                                                             replaceReadWithFieldReference = propertyInfo.getMethod != null && !propertyInfo.isGetMethodBodyFieldAccess,
                                                             replaceWriteWithFieldReference = propertyInfo.setMethod != null && !propertyInfo.isSetMethodBodyFieldAccess))
            }

            //TODO: doc-comments

            var getter: PropertyAccessor? = null
            if (propertyInfo.needExplicitGetter) {
                if (getMethod != null) {
                    val method = convertMethod(getMethod, null, null, null, classKind)!!
                    if (method.modifiers.contains(Modifier.EXTERNAL))
                        getter = PropertyAccessor(AccessorKind.GETTER, method.annotations, Modifiers(listOf(Modifier.EXTERNAL)).assignNoPrototype(), null, null)
                    else
                        getter = PropertyAccessor(AccessorKind.GETTER, method.annotations, Modifiers.Empty, method.parameterList, method.body)
                    getter.assignPrototype(getMethod, CommentsAndSpacesInheritance.NO_SPACES)
                }
                else if (propertyInfo.modifiers.contains(Modifier.OVERRIDE) && !(propertyInfo.superInfo?.isAbstract() ?: false)) {
                    val superExpression = SuperExpression(Identifier.Empty).assignNoPrototype()
                    val superAccess = QualifiedExpression(superExpression, propertyInfo.identifier, null).assignNoPrototype()
                    val returnStatement = ReturnStatement(superAccess).assignNoPrototype()
                    val body = Block.of(returnStatement).assignNoPrototype()
                    val parameterList = ParameterList.withNoPrototype(emptyList())
                    getter = PropertyAccessor(AccessorKind.GETTER, Annotations.Empty, Modifiers.Empty, parameterList, deferredElement { body })
                    getter.assignNoPrototype()
                }
                else {
                    //TODO: what else?
                    getter = PropertyAccessor(AccessorKind.GETTER, Annotations.Empty, Modifiers.Empty, null, null).assignNoPrototype()
                }
            }

            var setter: PropertyAccessor? = null
            if (propertyInfo.needExplicitSetter) {
                val accessorModifiers = Modifiers(propertyInfo.specialSetterAccess.singletonOrEmptyList()).assignNoPrototype()
                if (setMethod != null && !propertyInfo.isSetMethodBodyFieldAccess) {
                    val method = convertMethod(setMethod, null, null, null, classKind)!!
                    if (method.modifiers.contains(Modifier.EXTERNAL))
                        setter = PropertyAccessor(AccessorKind.SETTER, method.annotations, accessorModifiers.with(Modifier.EXTERNAL), null, null)
                    else {
                        val convertedParameter = method.parameterList!!.parameters.single() as FunctionParameter
                        val parameterAnnotations = convertedParameter.annotations
                        val parameterList = if (method.body != null || !parameterAnnotations.isEmpty) {
                            val parameter = FunctionParameter(convertedParameter.identifier, null, FunctionParameter.VarValModifier.None, parameterAnnotations, Modifiers.Empty)
                                    .assignPrototypesFrom(convertedParameter, CommentsAndSpacesInheritance.NO_SPACES)
                            ParameterList.withNoPrototype(listOf(parameter))
                        }
                        else {
                            null
                        }
                        setter = PropertyAccessor(AccessorKind.SETTER, method.annotations, accessorModifiers, parameterList, method.body)
                    }
                    setter.assignPrototype(setMethod, CommentsAndSpacesInheritance.NO_SPACES)
                }
                else if (propertyInfo.modifiers.contains(Modifier.OVERRIDE) && !(propertyInfo.superInfo?.isAbstract() ?: false)) {
                    val superExpression = SuperExpression(Identifier.Empty).assignNoPrototype()
                    val superAccess = QualifiedExpression(superExpression, propertyInfo.identifier, null).assignNoPrototype()
                    val valueIdentifier = Identifier.withNoPrototype("value", isNullable = false)
                    val assignment = AssignmentExpression(superAccess, valueIdentifier, Operator.EQ).assignNoPrototype()
                    val body = Block.of(assignment).assignNoPrototype()
                    val parameter = FunctionParameter(valueIdentifier, propertyType, FunctionParameter.VarValModifier.None, Annotations.Empty, Modifiers.Empty).assignNoPrototype()
                    val parameterList = ParameterList.withNoPrototype(listOf(parameter))
                    setter = PropertyAccessor(AccessorKind.SETTER, Annotations.Empty, accessorModifiers, parameterList, deferredElement { body })
                    setter.assignNoPrototype()
                }
                else {
                    setter = PropertyAccessor(AccessorKind.SETTER, Annotations.Empty, accessorModifiers, null, null).assignNoPrototype()
                }
            }

            val needInitializer = field != null && shouldGenerateDefaultInitializer(referenceSearcher, field)
            val property = Property(name,
                                    annotations,
                                    modifiers,
                                    propertyInfo.isVar,
                                    propertyType,
                                    shouldDeclareType,
                                    deferredElement { codeConverter -> field?.let { codeConverter.convertExpression(it.initializer, it.type) } ?: Expression.Empty },
                                    needInitializer,
                                    getter,
                                    setter,
                                    classKind == ClassKind.INTERFACE
            )

            val placementElement = field ?: getMethod ?: setMethod
            val prototypes = listOfNotNull<PsiElement>(field, getMethod, setMethod)
                    .map { PrototypeInfo(it, if (it == placementElement) CommentsAndSpacesInheritance.LINE_BREAKS else CommentsAndSpacesInheritance.NO_SPACES) }
            return property.assignPrototypes(*prototypes.toTypedArray())
        }
    }


    private fun specialAnnotationPropertyCases(field: PsiField): Annotations {
        val javaSerializableInterface = JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_IO_SERIALIZABLE, field.resolveScope)
        val output = mutableListOf<Annotation>()
        if (javaSerializableInterface != null &&
            field.name == "serialVersionUID" &&
            field.hasModifierProperty(PsiModifier.FINAL) &&
            field.hasModifierProperty(PsiModifier.STATIC) &&
            field.containingClass?.isInheritor(javaSerializableInterface, false) ?: false
        ) {
            output.add(Annotation(Identifier("JvmStatic").assignNoPrototype(),
                                  listOf(),
                                  newLineAfter = false).assignNoPrototype())
        }

        return Annotations(output)
    }

    private fun combinedNullability(vararg psiElements: PsiElement?): Nullability {
        val values = psiElements.filterNotNull().map {
            when (it) {
                is PsiVariable -> typeConverter.variableNullability(it)
                is PsiMethod -> typeConverter.methodNullability(it)
                else -> throw IllegalArgumentException()
            }
        }
        return when {
            values.contains(Nullability.Nullable) -> Nullability.Nullable
            values.contains(Nullability.Default) -> Nullability.Default
            else -> Nullability.NotNull
        }
    }

    private fun combinedMutability(vararg psiElements: PsiElement?): Mutability {
        val values = psiElements.filterNotNull().map {
            when (it) {
                is PsiVariable -> typeConverter.variableMutability(it)
                is PsiMethod -> typeConverter.methodMutability(it)
                else -> throw IllegalArgumentException()
            }
        }
        return when {
            values.contains(Mutability.Mutable) -> Mutability.Mutable
            values.contains(Mutability.Default) -> Mutability.Default
            else -> Mutability.NonMutable
        }
    }

    fun shouldDeclareVariableType(variable: PsiVariable, type: Type, canChangeType: Boolean): Boolean {
        assert(inConversionScope(variable))

        val initializer = variable.initializer
        if (initializer == null || initializer.isNullLiteral()) return true
        if (initializer.type is PsiPrimitiveType && type is PrimitiveType) {
            if (createDefaultCodeConverter().convertedExpressionType(initializer, variable.type) != type) {
                return true
            }
        }

        val initializerType = createDefaultCodeConverter().convertedExpressionType(initializer, variable.type)
        // do not add explicit type when initializer is not resolved, let user add it if really needed
        if (initializerType is ErrorType) return false

        if (shouldSpecifyTypeForAnonymousType(variable, initializerType)) return true

        if (canChangeType) return false

        return type != initializerType
    }

    // add explicit type when initializer has anonymous type,
    // the variable is private or local and
    // it has write accesses or is stored to another variable
    private fun shouldSpecifyTypeForAnonymousType(variable: PsiVariable, initializerType: Type): Boolean {
        if (initializerType !is ClassType || !initializerType.isAnonymous()) return false

        val scope: PsiElement? =
                when {
                    variable is PsiField && variable.hasModifierProperty(PsiModifier.PRIVATE) -> variable.containingClass
                    variable is PsiLocalVariable -> variable.getContainingMethod()
                    else -> null
                }

        if (scope == null) return false

        return variable.hasWriteAccesses(referenceSearcher, scope) ||
               variable.isInVariableInitializer(referenceSearcher, scope)
    }

    fun convertMethod(
            method: PsiMethod,
            fieldsToDrop: MutableSet<PsiField>?,
            constructorConverter: ConstructorConverter?,
            overloadReducer: OverloadReducer?,
            classKind: ClassKind
    ): FunctionLike? {
        val returnType = typeConverter.convertMethodReturnType(method)

        val annotations = convertAnnotations(method) + convertThrows(method)

        var modifiers = convertModifiers(method, classKind.isOpen())

        val statementsToInsert = ArrayList<Statement>()
        for (parameter in method.parameterList.parameters) {
            if (parameter.hasWriteAccesses(referenceSearcher, method)) {
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

        val function = if (method.isConstructor && constructorConverter != null) {
            constructorConverter.convertConstructor(method, annotations, modifiers, fieldsToDrop!!, postProcessBody)
        }
        else {
            val containingClass = method.containingClass

            if (settings.openByDefault) {
                val isEffectivelyFinal = method.hasModifierProperty(PsiModifier.FINAL) ||
                                         containingClass != null && (containingClass.hasModifierProperty(PsiModifier.FINAL) || containingClass.isEnum)
                if (!isEffectivelyFinal && !modifiers.contains(Modifier.ABSTRACT) && !modifiers.isPrivate) {
                    modifiers = modifiers.with(Modifier.OPEN)
                }
            }

            val params = convertParameterList(method, overloadReducer)

            val typeParameterList = convertTypeParameterList(method.typeParameterList)
            val body = deferredElement { codeConverter: CodeConverter ->
                val body = codeConverter.withMethodReturnType(method.returnType).convertBlock(method.body)
                postProcessBody(body)
            }
            Function(method.declarationIdentifier(), annotations, modifiers, returnType, typeParameterList, params, body, classKind == ClassKind.INTERFACE)
        }

        if (function == null) return null

        if (PsiMethodUtil.isMainMethod(method)) {
            function.annotations += Annotations(
                    listOf(Annotation(Identifier.withNoPrototype("JvmStatic"),
                                      listOf(),
                                      newLineAfter = false).assignNoPrototype())).assignNoPrototype()
        }

        if (function.parameterList!!.parameters.any { it is FunctionParameter && it.defaultValue != null } && !function.modifiers.isPrivate) {
            function.annotations += Annotations(
                    listOf(Annotation(Identifier.withNoPrototype("JvmOverloads"),
                                      listOf(),
                                      newLineAfter = false).assignNoPrototype())).assignNoPrototype()
        }

        return function.assignPrototype(method)
    }

    /**
     * Overrides of methods from Object should not be marked as overrides in Kotlin unless the class itself has java ancestors
     */
    private fun isOverride(method: PsiMethod): Boolean {
        val superSignatures = method.hierarchicalMethodSignature.superSignatures

        val overridesMethodNotFromObject = superSignatures.any {
            it.method.containingClass?.qualifiedName != JAVA_LANG_OBJECT
        }
        if (overridesMethodNotFromObject) return true

        val overridesMethodFromObject = superSignatures.any {
            it.method.containingClass?.qualifiedName == JAVA_LANG_OBJECT
        }
        if (overridesMethodFromObject) {
            when (method.name) {
                "equals", "hashCode", "toString" -> return true // these methods from java.lang.Object exist in kotlin.Any

                else -> {
                    val containing = method.containingClass
                    if (containing != null) {
                        val hasOtherJavaSuperclasses = containing.superTypes.any {
                            //TODO: correctly check for kotlin class
                            val klass = it.resolve()
                            klass != null && klass.qualifiedName != JAVA_LANG_OBJECT && !inConversionScope(klass)
                        }
                        if (hasOtherJavaSuperclasses) return true
                    }
                }
            }
        }

        return false
    }

    private fun needOpenModifier(method: PsiMethod, isInOpenClass: Boolean, modifiers: Modifiers): Boolean {
        if (!isInOpenClass) return false
        if (modifiers.contains(Modifier.OVERRIDE) || modifiers.contains(Modifier.ABSTRACT)) return false
        if (settings.openByDefault) {
            return !method.hasModifierProperty(PsiModifier.FINAL)
                   && !method.hasModifierProperty(PsiModifier.PRIVATE)
                   && !method.hasModifierProperty(PsiModifier.STATIC)
        }
        else {
            return referenceSearcher.hasOverrides(method)
        }
    }

    fun convertCodeReferenceElement(element: PsiJavaCodeReferenceElement, hasExternalQualifier: Boolean, typeArgsConverted: List<Element>? = null): ReferenceElement {
        val typeArgs = typeArgsConverted ?: typeConverter.convertTypes(element.typeParameters)

        val targetClass = element.resolve() as? PsiClass
        if (targetClass != null) {
            convertToKotlinAnalogIdentifier(targetClass.qualifiedName, Mutability.Default)
                    ?.let { return ReferenceElement(it, typeArgs).assignNoPrototype() }
        }

        if (element.isQualified) {
            var result = Identifier.toKotlin(element.referenceName!!)
            var qualifier = element.qualifier
            while (qualifier != null) {
                val codeRefElement = qualifier as PsiJavaCodeReferenceElement
                result = Identifier.toKotlin(codeRefElement.referenceName!!) + "." + result
                qualifier = codeRefElement.qualifier
            }
            return ReferenceElement(Identifier.withNoPrototype(result), typeArgs).assignPrototype(element)
        }
        else {
            if (!hasExternalQualifier) {
                // references to nested classes may need correction
                if (targetClass != null) {
                    val identifier = constructNestedClassReferenceIdentifier(targetClass, specialContext ?: element)
                    if (identifier != null) {
                        return ReferenceElement(identifier, typeArgs).assignPrototype(element)
                    }
                }
            }

            return ReferenceElement(Identifier.withNoPrototype(element.referenceName!!), typeArgs).assignPrototype(element)
        }
    }

    private fun constructNestedClassReferenceIdentifier(psiClass: PsiClass, context: PsiElement): Identifier? {
        val outerClass = psiClass.containingClass
        if (outerClass != null
            && !PsiTreeUtil.isAncestor(outerClass, context, true)
            && !psiClass.isImported(context.containingFile as PsiJavaFile)) {
            val qualifier = constructNestedClassReferenceIdentifier(outerClass, context)?.name ?: outerClass.name!!
            return Identifier.withNoPrototype(Identifier.toKotlin(qualifier) + "." + Identifier.toKotlin(psiClass.name!!))
        }
        return null
    }

    fun convertTypeElement(element: PsiTypeElement?, nullability: Nullability): Type
            = (if (element == null) ErrorType() else typeConverter.convertType(element.type, nullability)).assignPrototype(element)

    private fun convertToNotNullableTypes(types: Array<out PsiType?>): List<Type>
            = types.map { typeConverter.convertType(it, Nullability.NotNull) }

    fun convertParameter(
            parameter: PsiParameter,
            nullability: Nullability = Nullability.Default,
            varValModifier: FunctionParameter.VarValModifier = FunctionParameter.VarValModifier.None,
            modifiers: Modifiers = Modifiers.Empty,
            defaultValue: DeferredElement<Expression>? = null
    ): FunctionParameter {
        var type = typeConverter.convertVariableType(parameter)
        when (nullability) {
            Nullability.NotNull -> type = type.toNotNullType()
            Nullability.Nullable -> type = type.toNullableType()
        }
        return FunctionParameter(parameter.declarationIdentifier(), type, varValModifier,
                                 convertAnnotations(parameter), modifiers, defaultValue).assignPrototype(parameter, CommentsAndSpacesInheritance.LINE_BREAKS)
    }

    fun convertIdentifier(identifier: PsiIdentifier?): Identifier {
        if (identifier == null) return Identifier.Empty

        return Identifier(identifier.text!!).assignPrototype(identifier)
    }

    fun convertModifiers(owner: PsiModifierListOwner, isMethodInOpenClass: Boolean): Modifiers {
        var modifiers = Modifiers(MODIFIERS_MAP.filter { owner.hasModifierProperty(it.first) }.map { it.second })
                .assignPrototype(owner.modifierList, CommentsAndSpacesInheritance.NO_SPACES)

        if (owner is PsiMethod) {
            val isOverride = isOverride(owner)
            if (isOverride) {
                modifiers = modifiers.with(Modifier.OVERRIDE)
            }

            if (needOpenModifier(owner, isMethodInOpenClass, modifiers)) {
                modifiers = modifiers.with(Modifier.OPEN)
            }

            if (owner.hasModifierProperty(PsiModifier.NATIVE))
                modifiers = modifiers.with(Modifier.EXTERNAL)

            modifiers = modifiers.adaptForContainingClassVisibility(owner.containingClass).adaptProtectedVisibility(owner)
        }
        else if (owner is PsiField) {
            modifiers = modifiers.adaptForContainingClassVisibility(owner.containingClass).adaptProtectedVisibility(owner)
        }
        else if (owner is PsiClass && owner.scope is PsiMethod) {
            // Local class should not have visibility modifiers
            modifiers = modifiers.without(modifiers.accessModifier())
        }

        return modifiers
    }

    // to convert package local members in package local class into public member (when it's not override, open or abstract)
    private fun Modifiers.adaptForContainingClassVisibility(containingClass: PsiClass?): Modifiers {
        if (containingClass == null || !containingClass.hasModifierProperty(PsiModifier.PACKAGE_LOCAL)) return this
        if (!contains(Modifier.INTERNAL) || contains(Modifier.OVERRIDE) || contains(Modifier.OPEN) || contains(Modifier.ABSTRACT)) return this
        return without(Modifier.INTERNAL).with(Modifier.PUBLIC)
    }

    private fun Modifiers.adaptProtectedVisibility(member: PsiMember): Modifiers {
        if (!member.hasModifierProperty(PsiModifier.PROTECTED)) return this

        val originalClass = member.containingClass ?: return this
        // Search for usages only in Java because java-protected member cannot be used in Kotlin from same package
        val usages = referenceSearcher.findUsagesForExternalCodeProcessing(member, true, false)

        return if (usages.any { !allowProtected(it.element, member, originalClass) })
            without(Modifier.PROTECTED).with(Modifier.PUBLIC)
        else this
    }

    private fun allowProtected(element: PsiElement, member: PsiMember, originalClass: PsiClass): Boolean {
        if (element.parent is PsiNewExpression && member is PsiMethod && member.isConstructor) {
            // calls to for protected constructors are allowed only within same class or as super calls
            return element.parentsWithSelf.contains(originalClass)
        }

        return element.parentsWithSelf.filterIsInstance<PsiClass>().any { accessContainingClass ->
            if (!InheritanceUtil.isInheritorOrSelf(accessContainingClass, originalClass, true)) return@any false

            if (element !is PsiReferenceExpression) return@any true

            val qualifierExpression = element.qualifierExpression ?: return@any true

            // super.foo is allowed if 'foo' is protected
            if (qualifierExpression is PsiSuperExpression) return@any true

            val receiverType = qualifierExpression.type ?: return@any true
            val resolvedClass = PsiUtil.resolveGenericsClassInType(receiverType).element ?: return@any true

            // receiver type should be subtype of containing class
            InheritanceUtil.isInheritorOrSelf(resolvedClass, accessContainingClass, true)
        }
    }

    fun convertAnonymousClassBody(anonymousClass: PsiAnonymousClass): AnonymousClassBody {
        return AnonymousClassBody(ClassBodyConverter(anonymousClass, ClassKind.ANONYMOUS_OBJECT, this).convertBody(),
                                  anonymousClass.baseClassType.resolve()?.isInterface ?: false).assignPrototype(anonymousClass)
    }

    private val MODIFIERS_MAP = listOf(
            PsiModifier.ABSTRACT to Modifier.ABSTRACT,
            PsiModifier.PUBLIC to Modifier.PUBLIC,
            PsiModifier.PROTECTED to Modifier.PROTECTED,
            PsiModifier.PRIVATE to Modifier.PRIVATE,
            PsiModifier.PACKAGE_LOCAL to Modifier.INTERNAL
    )

    private fun convertThrows(method: PsiMethod): Annotations {
        val throwsList = method.throwsList
        val types = throwsList.referencedTypes
        val refElements = throwsList.referenceElements
        assert(types.size == refElements.size)
        if (types.isEmpty()) return Annotations.Empty
        val arguments = types.indices.map { index ->
            val convertedType = typeConverter.convertType(types[index], Nullability.NotNull)
            null to deferredElement<Expression> { ClassLiteralExpression(convertedType.assignPrototype(refElements[index])) }
        }
        val annotation = Annotation(Identifier.withNoPrototype("Throws"), arguments, newLineAfter = true)
        return Annotations(listOf(annotation.assignPrototype(throwsList))).assignPrototype(throwsList)
    }

    private class CachingReferenceSearcher(private val searcher: ReferenceSearcher) : ReferenceSearcher by searcher {
        private val hasInheritorsCached = HashMap<PsiClass, Boolean>()
        private val hasOverridesCached = HashMap<PsiMethod, Boolean>()

        override fun hasInheritors(`class`: PsiClass): Boolean {
            val cached = hasInheritorsCached[`class`]
            if (cached != null) return cached
            val result = searcher.hasInheritors(`class`)
            hasInheritorsCached[`class`] = result
            return result
        }

        override fun hasOverrides(method: PsiMethod): Boolean {
            val cached = hasOverridesCached[method]
            if (cached != null) return cached
            val result = searcher.hasOverrides(method)
            hasOverridesCached[method] = result
            return result
        }
    }
}

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
