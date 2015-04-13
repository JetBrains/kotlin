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
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.j2k.ast.Annotation
import org.jetbrains.kotlin.j2k.ast.Class
import org.jetbrains.kotlin.j2k.ast.Enum
import org.jetbrains.kotlin.j2k.ast.Function
import org.jetbrains.kotlin.j2k.ast.Object
import org.jetbrains.kotlin.j2k.usageProcessing.FieldToPropertyProcessing
import org.jetbrains.kotlin.j2k.usageProcessing.UsageProcessing
import org.jetbrains.kotlin.j2k.usageProcessing.UsageProcessingExpressionConverter
import org.jetbrains.kotlin.types.expressions.OperatorConventions.*
import java.util.ArrayList
import java.util.HashMap

class Converter private(
        private val elementToConvert: PsiElement,
        val settings: ConverterSettings,
        val inConversionScope: (PsiElement) -> Boolean,
        val referenceSearcher: ReferenceSearcher,
        val resolverForConverter: ResolverForConverter,
        private val commonState: Converter.CommonState,
        private val personalState: Converter.PersonalState
) {

    // state which is shared between all converter's based on this one
    private class CommonState(val usageProcessingsCollector: (UsageProcessing) -> Unit) {
        val deferredElements = ArrayList<DeferredElement<*>>()
        val postUnfoldActions = ArrayList<() -> Unit>()
    }

    // state which may differ in different converter's
    public class PersonalState(val specialContext: PsiElement?)

    public val project: Project = elementToConvert.getProject()
    public val typeConverter: TypeConverter = TypeConverter(this)
    public val annotationConverter: AnnotationConverter = AnnotationConverter(this)

    public val specialContext: PsiElement? = personalState.specialContext

    companion object {
        public fun create(elementToConvert: PsiElement, settings: ConverterSettings, inConversionScope: (PsiElement) -> Boolean,
                          referenceSearcher: ReferenceSearcher, resolverForConverter: ResolverForConverter,
                          usageProcessingsCollector: (UsageProcessing) -> Unit): Converter {
            return Converter(elementToConvert, settings, inConversionScope,
                             CachingReferenceSearcher(referenceSearcher),
                             resolverForConverter, CommonState(usageProcessingsCollector), PersonalState(null))
        }
    }

    public fun withSpecialContext(context: PsiElement): Converter = withState(PersonalState(context))

    private fun withState(state: PersonalState): Converter
            = Converter(elementToConvert, settings, inConversionScope, referenceSearcher, resolverForConverter, commonState, state)

    private fun createDefaultCodeConverter() = CodeConverter(this, DefaultExpressionConverter(), DefaultStatementConverter(), null)

    public data class IntermediateResult(
            val codeGenerator: (Map<PsiElement, Collection<UsageProcessing>>) -> String,
            val parseContext: ParseContext
    )

    public fun convert(): IntermediateResult? {
        val element = convertTopElement(elementToConvert) ?: return null
        val parseContext = when (elementToConvert) {
            is PsiStatement, is PsiExpression -> ParseContext.CODE_BLOCK
            else -> ParseContext.TOP_LEVEL
        }
        return IntermediateResult(
                { usageProcessings ->
                    unfoldDeferredElements(usageProcessings)

                    val builder = CodeBuilder(elementToConvert)
                    builder.append(element)
                    builder.result
                },
                parseContext)
    }

    private fun convertTopElement(element: PsiElement): Element? = when (element) {
        is PsiJavaFile -> convertFile(element)
        is PsiClass -> convertClass(element)
        is PsiMethod -> convertMethod(element, null, null, null, false)
        is PsiField -> convertField(element, null)
        is PsiStatement -> createDefaultCodeConverter().convertStatement(element)
        is PsiExpression -> createDefaultCodeConverter().convertExpression(element)
        is PsiImportList -> convertImportList(element)
        is PsiImportStatementBase -> convertImport(element, false)
        is PsiAnnotation -> annotationConverter.convertAnnotation(element, false, false)
        is PsiPackageStatement -> PackageStatement(quoteKeywords(element.getPackageName() ?: "")).assignPrototype(element)
        else -> null
    }

    private fun unfoldDeferredElements(usageProcessings: Map<PsiElement, Collection<UsageProcessing>>) {
        val codeConverter = createDefaultCodeConverter().withSpecialExpressionConverter(UsageProcessingExpressionConverter(usageProcessings))

        // we use loop with index because new deferred elements can be added during unfolding
        var i = 0
        while (i < commonState.deferredElements.size()) {
            val deferredElement = commonState.deferredElements[i++]
            deferredElement.unfold(codeConverter.withConverter(this.withState(deferredElement.converterState)))
        }

        commonState.postUnfoldActions.forEach { it() }
    }

    public fun<TResult : Element> deferredElement(generator: (CodeConverter) -> TResult): DeferredElement<TResult> {
        val element = DeferredElement(generator, personalState)
        commonState.deferredElements.add(element)
        return element
    }

    public fun addUsageProcessing(processing: UsageProcessing) {
        commonState.usageProcessingsCollector(processing)
    }

    public fun addPostUnfoldDeferredElementsAction(action: () -> Unit) {
        commonState.postUnfoldActions.add(action)
    }

    private fun convertFile(javaFile: PsiJavaFile): File {
        var convertedChildren = javaFile.getChildren().map { convertTopElement(it) }.filterNotNull()
        return File(convertedChildren, createMainFunction(javaFile)).assignPrototype(javaFile)
    }

    private fun createMainFunction(file: PsiJavaFile): String? {
        for (`class` in file.getClasses()) {
            val mainMethod = PsiMethodUtil.findMainMethod(`class`)
            if (mainMethod != null) {
                return "fun main(args: Array<String>) = ${`class`.getName()}.${mainMethod.getName()}(args)"
            }
        }
        return null
    }

    public fun convertAnnotations(owner: PsiModifierListOwner): Annotations
            = annotationConverter.convertAnnotations(owner)

    public fun convertClass(psiClass: PsiClass): Class {
        if (psiClass.isAnnotationType()) {
            return convertAnnotationType(psiClass)
        }

        val annotations = convertAnnotations(psiClass)
        var modifiers = convertModifiers(psiClass)
        val typeParameters = convertTypeParameterList(psiClass.getTypeParameterList())
        val extendsTypes = convertToNotNullableTypes(psiClass.getExtendsListTypes())
        val implementsTypes = convertToNotNullableTypes(psiClass.getImplementsListTypes())
        val name = psiClass.declarationIdentifier()

        return when {
            psiClass.isInterface() -> {
                val classBody = ClassBodyConverter(psiClass, this, isOpenClass = false, isObject = false).convertBody()
                Trait(name, annotations, modifiers, typeParameters, extendsTypes, implementsTypes, classBody)
            }

            psiClass.isEnum() -> {
                val classBody = ClassBodyConverter(psiClass, this, isOpenClass = false, isObject = false).convertBody()
                Enum(name, annotations, modifiers, typeParameters, implementsTypes, classBody)
            }

            else -> {
                if (shouldConvertIntoObject(psiClass)) {
                    val classBody = ClassBodyConverter(psiClass, this, isOpenClass = false, isObject = true).convertBody()
                    Object(name, annotations, modifiers.without(Modifier.ABSTRACT), classBody)
                }
                else {
                    if (psiClass.getContainingClass() != null && !psiClass.hasModifierProperty(PsiModifier.STATIC)) {
                        modifiers = modifiers.with(Modifier.INNER)
                    }

                    if (needOpenModifier(psiClass)) {
                        modifiers = modifiers.with(Modifier.OPEN)
                    }

                    val classBody = ClassBodyConverter(psiClass, this, isOpenClass = modifiers.contains(Modifier.OPEN) || modifiers.contains(Modifier.ABSTRACT), isObject = false).convertBody()
                    Class(name, annotations, modifiers, typeParameters, extendsTypes, classBody.baseClassParams, implementsTypes, classBody)
                }
            }
        }.assignPrototype(psiClass)
    }

    private fun needOpenModifier(psiClass: PsiClass): Boolean {
        return if (psiClass.hasModifierProperty(PsiModifier.FINAL) || psiClass.hasModifierProperty(PsiModifier.ABSTRACT))
            false
        else
            settings.openByDefault || referenceSearcher.hasInheritors(psiClass)
    }

    private fun shouldConvertIntoObject(psiClass: PsiClass): Boolean {
        val methods = psiClass.getMethods()
        val fields = psiClass.getFields()
        val classes = psiClass.getInnerClasses()
        if (methods.isEmpty() && fields.isEmpty()) return false
        fun isStatic(member: PsiMember) = member.hasModifierProperty(PsiModifier.STATIC)
        if (!methods.all { isStatic(it) || it.isConstructor() } || !fields.all(::isStatic) || !classes.all(::isStatic)) return false

        val constructors = psiClass.getConstructors()
        if (constructors.size() > 1) return false
        val constructor = constructors.singleOrNull()
        if (constructor != null) {
            if (!constructor.hasModifierProperty(PsiModifier.PRIVATE)) return false
            if (constructor.getParameterList().getParameters().isNotEmpty()) return false
            if (constructor.getBody()?.getStatements()?.isNotEmpty() ?: false) return false
            if (constructor.getModifierList().getAnnotations().isNotEmpty()) return false
        }

        if (psiClass.getExtendsListTypes().isNotEmpty() || psiClass.getImplementsListTypes().isNotEmpty()) return false
        if (psiClass.getTypeParameters().isNotEmpty()) return false

        if (referenceSearcher.hasInheritors(psiClass)) return false

        return true
    }

    private fun convertAnnotationType(psiClass: PsiClass): Class {
        val paramModifiers = Modifiers(listOf(Modifier.PUBLIC)).assignNoPrototype()
        val noBlankLinesInheritance = CommentsAndSpacesInheritance(blankLinesBefore = false)
        val annotationMethods = psiClass.getMethods().filterIsInstance<PsiAnnotationMethod>()
        val (methodsNamedValue, otherMethods) = annotationMethods.partition { it.getName() == "value" }

        fun createParameter(type: Type, method: PsiAnnotationMethod): Parameter {
            type.assignPrototype(method.getReturnTypeElement(), noBlankLinesInheritance)

            return Parameter(method.declarationIdentifier(),
                      type,
                      Parameter.VarValModifier.Val,
                      convertAnnotations(method),
                      paramModifiers,
                      annotationConverter.convertAnnotationMethodDefault(method)).assignPrototype(method, noBlankLinesInheritance)
        }

        val parameters =
                // Argument named `value` comes first if it exists
                // Convert it as vararg if it's array
                methodsNamedValue.
                map { method ->
                    val returnType = method.getReturnType()
                    val typeConverted = if (returnType is PsiArrayType)
                        VarArgType(typeConverter.convertType(returnType.getComponentType(), Nullability.NotNull))
                    else
                        typeConverter.convertType(returnType, Nullability.NotNull)

                    createParameter(typeConverted, method)
                } +
                otherMethods
                .map { method ->
                    val typeConverted = typeConverter.convertType(method.getReturnType(), Nullability.NotNull)
                    createParameter(typeConverted, method)
                }
        val parameterList = ParameterList(parameters).assignNoPrototype()
        val constructorSignature = if (parameterList.parameters.isNotEmpty())
            PrimaryConstructorSignature(Annotations.Empty, Modifiers.Empty, parameterList).assignNoPrototype()
        else
            null

        // to convert fields and nested types - they are not allowed in Kotlin but we convert them and let user refactor code
        var classBody = ClassBodyConverter(psiClass, this, isOpenClass = false, isObject = false).convertBody()
        classBody = ClassBody(constructorSignature, classBody.baseClassParams, classBody.members,
                              classBody.companionObjectMembers, classBody.lBrace, classBody.rBrace)

        val annotationAnnotation = Annotation(Identifier("annotation").assignNoPrototype(), listOf(), false, false).assignNoPrototype()
        return Class(psiClass.declarationIdentifier(),
                     convertAnnotations(psiClass) + Annotations(listOf(annotationAnnotation)),
                     convertModifiers(psiClass).without(Modifier.ABSTRACT),
                     TypeParameterList.Empty,
                     listOf(),
                     null,
                     listOf(),
                     classBody).assignPrototype(psiClass)
    }

    public fun convertInitializer(initializer: PsiClassInitializer): Initializer {
        return Initializer(deferredElement { codeConverter -> codeConverter.convertBlock(initializer.getBody()) },
                           convertModifiers(initializer)).assignPrototype(initializer)
    }

    public fun convertField(field: PsiField, correction: FieldCorrectionInfo?): Member {
        val annotations = convertAnnotations(field)

        var modifiers = convertModifiers(field)
        if (correction != null) {
            modifiers = modifiers.without(modifiers.accessModifier()).with(correction.access)
        }

        val name = correction?.identifier ?: field.declarationIdentifier()
        val converted = if (field is PsiEnumConstant) {
            val argumentList = field.getArgumentList()
            EnumConstant(name,
                         annotations,
                         modifiers,
                         typeConverter.convertType(field.getType(), Nullability.NotNull),
                         deferredElement { codeConverter -> ExpressionList(codeConverter.convertExpressions(argumentList?.getExpressions() ?: array<PsiExpression>())).assignPrototype(argumentList) })
        }
        else {
            val isVal = isVal(referenceSearcher, field)
            val typeToDeclare = variableTypeToDeclare(field,
                                                      settings.specifyFieldTypeByDefault || modifiers.isPublic || modifiers.isProtected,
                                                      isVal && modifiers.isPrivate)
            val propertyType = typeToDeclare ?: typeConverter.convertVariableType(field)

            addUsageProcessing(FieldToPropertyProcessing(field, correction?.name ?: field.getName(), propertyType.isNullable))

            Property(name,
                  annotations,
                  modifiers,
                  propertyType,
                  deferredElement { codeConverter -> codeConverter.convertExpression(field.getInitializer(), field.getType()) },
                  isVal,
                  typeToDeclare != null,
                  shouldGenerateDefaultInitializer(referenceSearcher, field),
                  if (correction != null) correction.setterAccess else modifiers.accessModifier())
        }
        return converted.assignPrototype(field)
    }

    public fun variableTypeToDeclare(variable: PsiVariable, specifyAlways: Boolean, canChangeType: Boolean): Type? {
        fun convertType() = typeConverter.convertVariableType(variable)

        if (specifyAlways) return convertType()

        val initializer = variable.getInitializer()
        if (initializer == null) return convertType()
        if (initializer is PsiLiteralExpression && initializer.getType() == PsiType.NULL) return convertType()

        if (canChangeType) return null

        val convertedType = convertType()
        var initializerType = createDefaultCodeConverter().convertedExpressionType(initializer, variable.getType())
        if (initializerType is ErrorType) return null // do not add explicit type when initializer is not resolved, let user add it if really needed
        return if (convertedType == initializerType) null else convertedType
    }

    public fun convertMethod(
            method: PsiMethod,
            membersToRemove: MutableSet<PsiMember>?,
            constructorConverter: ConstructorConverter?,
            overloadReducer: OverloadReducer?,
            isInOpenClass: Boolean
    ): FunctionLike? {
        val returnType = typeConverter.convertMethodReturnType(method)

        val annotations = convertAnnotations(method) + convertThrows(method)

        var modifiers = convertModifiers(method)
        if (needOpenModifier(method, isInOpenClass, modifiers)) {
            modifiers = modifiers.with(Modifier.OPEN)
        }

        val statementsToInsert = ArrayList<Statement>()
        for (parameter in method.getParameterList().getParameters()) {
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

        val function = if (method.isConstructor() && constructorConverter != null) {
            constructorConverter.convertConstructor(method, annotations, modifiers, membersToRemove!!, postProcessBody)
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

            var params = convertParameterList(method, overloadReducer)

            val typeParameterList = convertTypeParameterList(method.getTypeParameterList())
            var body = deferredElement { codeConverter: CodeConverter ->
                val body = codeConverter.withMethodReturnType(method.getReturnType()).convertBlock(method.getBody())
                postProcessBody(body)
            }
            Function(method.declarationIdentifier(), annotations, modifiers, returnType, typeParameterList, params, body, containingClass?.isInterface() ?: false)
        }

        if (function == null) return null

        if (function.parameterList.parameters.any { it.defaultValue != null }) {
            function.annotations += Annotations(
                    listOf(Annotation(Identifier("overloads").assignNoPrototype(),
                                      listOf(),
                                      brackets = function is PrimaryConstructor,
                                      newLineAfter = false).assignNoPrototype())).assignNoPrototype()
        }

        return function.assignPrototype(method)
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
                            val klass = it.resolve()
                            klass != null && klass.getQualifiedName() != JAVA_LANG_OBJECT && !inConversionScope(klass)
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

    public fun convertCodeReferenceElement(element: PsiJavaCodeReferenceElement, hasExternalQualifier: Boolean, typeArgsConverted: List<Element>? = null): ReferenceElement {
        val typeArgs = typeArgsConverted ?: typeConverter.convertTypes(element.getTypeParameters())

        if (element.isQualified()) {
            var result = Identifier.toKotlin(element.getReferenceName()!!)
            var qualifier = element.getQualifier()
            while (qualifier != null) {
                val codeRefElement = qualifier as PsiJavaCodeReferenceElement
                result = Identifier.toKotlin(codeRefElement.getReferenceName()!!) + "." + result
                qualifier = codeRefElement.getQualifier()
            }
            return ReferenceElement(Identifier(result).assignNoPrototype(), typeArgs).assignPrototype(element)
        }
        else {
            if (!hasExternalQualifier) {
                // references to nested classes may need correction
                val targetClass = element.resolve() as? PsiClass
                if (targetClass != null) {
                    val identifier = constructNestedClassReferenceIdentifier(targetClass, specialContext ?: element)
                    if (identifier != null) {
                        return ReferenceElement(identifier, typeArgs).assignPrototype(element)
                    }
                }
            }

            return ReferenceElement(Identifier(element.getReferenceName()!!).assignNoPrototype(), typeArgs).assignPrototype(element)
        }
    }

    private fun constructNestedClassReferenceIdentifier(psiClass: PsiClass, context: PsiElement): Identifier? {
        val outerClass = psiClass.getContainingClass()
        if (outerClass != null
                && !PsiTreeUtil.isAncestor(outerClass, context, true)
                && !psiClass.isImported(context.getContainingFile() as PsiJavaFile)) {
            val qualifier = constructNestedClassReferenceIdentifier(outerClass, context)?.name ?: outerClass.getName()!!
            return Identifier(Identifier.toKotlin(qualifier) + "." + Identifier.toKotlin(psiClass.getName()!!)).assignNoPrototype()
        }
        return null
    }

    public fun convertTypeElement(element: PsiTypeElement?): TypeElement
            = TypeElement(if (element == null) ErrorType().assignNoPrototype() else typeConverter.convertType(element.getType())).assignPrototype(element)

    private fun convertToNotNullableTypes(types: Array<out PsiType?>): List<Type>
            = types.map { typeConverter.convertType(it, Nullability.NotNull) }

    public fun convertParameter(
            parameter: PsiParameter,
            nullability: Nullability = Nullability.Default,
            varValModifier: Parameter.VarValModifier = Parameter.VarValModifier.None,
            modifiers: Modifiers = Modifiers.Empty,
            defaultValue: DeferredElement<Expression>? = null
    ): Parameter {
        var type = typeConverter.convertVariableType(parameter)
        when (nullability) {
            Nullability.NotNull -> type = type.toNotNullType()
            Nullability.Nullable -> type = type.toNullableType()
        }
        return Parameter(parameter.declarationIdentifier(), type, varValModifier,
                         convertAnnotations(parameter), modifiers, defaultValue).assignPrototype(parameter)
    }

    public fun convertIdentifier(identifier: PsiIdentifier?): Identifier {
        if (identifier == null) return Identifier.Empty

        return Identifier(identifier.getText()!!).assignPrototype(identifier)
    }

    public fun convertModifiers(owner: PsiModifierListOwner): Modifiers {
        return Modifiers(MODIFIERS_MAP.filter { owner.hasModifierProperty(it.first) }.map { it.second })
                .assignPrototype(owner.getModifierList(), CommentsAndSpacesInheritance(blankLinesBefore = false))
    }

    private val MODIFIERS_MAP = listOf(
            PsiModifier.ABSTRACT to Modifier.ABSTRACT,
            PsiModifier.PUBLIC to Modifier.PUBLIC,
            PsiModifier.PROTECTED to Modifier.PROTECTED,
            PsiModifier.PRIVATE to Modifier.PRIVATE
    )

    private fun convertThrows(method: PsiMethod): Annotations {
        val throwsList = method.getThrowsList()
        val types = throwsList.getReferencedTypes()
        val refElements = throwsList.getReferenceElements()
        assert(types.size() == refElements.size())
        if (types.isEmpty()) return Annotations.Empty
        val arguments = types.indices.map { index ->
            val convertedType = typeConverter.convertType(types[index], Nullability.NotNull)
            null to deferredElement<Expression> { ClassLiteralExpression(convertedType.assignPrototype(refElements[index])) }
        }
        val annotation = Annotation(Identifier("throws").assignNoPrototype(), arguments, false, true)
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
