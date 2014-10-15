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
import java.util.*
import com.intellij.psi.CommonClassNames.*
import org.jetbrains.jet.lang.types.expressions.OperatorConventions.*
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiMethodUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException

public trait ConversionScope {
    public fun contains(element: PsiElement): Boolean
}

public class FilesConversionScope(val files: Collection<PsiJavaFile>) : ConversionScope {
    override fun contains(element: PsiElement) = files.any { element.getContainingFile() == it }
}

public trait PostProcessor {
    public val contextToAnalyzeIn: PsiElement
    public fun analyzeFile(file: JetFile): BindingContext
    public fun doAdditionalProcessing(file: JetFile)
}

public class Converter private(val project: Project,
                               val settings: ConverterSettings,
                               val conversionScope: ConversionScope,
                               val referenceSearcher: ReferenceSearcher,
                               private val postProcessor: PostProcessor?,
                               private val state: Converter.State) {
    private class State(val methodReturnType: PsiType?,
                        val expressionConverter: ExpressionConverter,
                        val statementConverter: StatementConverter,
                        val specialContext: PsiElement?,
                        val importList: ImportList?,
                        val importsToAdd: MutableCollection<String>?)

    val typeConverter: TypeConverter = TypeConverter(this)

    val methodReturnType: PsiType? = state.methodReturnType
    val specialContext: PsiElement? = state.specialContext
    val importNames: Set<String> = state.importList?.imports?.mapTo(HashSet<String>()) { it.name } ?: setOf()
    val importsToAdd: MutableCollection<String>? = state.importsToAdd

    val annotationConverter = AnnotationConverter(this)

    class object {
        private val LOG = Logger.getInstance("#org.jetbrains.jet.j2k.Converter")

        public fun create(project: Project, settings: ConverterSettings, conversionScope: ConversionScope,
                          referenceSearcher: ReferenceSearcher, postProcessor: PostProcessor?): Converter {
            val state = State(null, DefaultExpressionConverter(), DefaultStatementConverter(), null, null, null)
            return Converter(project, settings, conversionScope, referenceSearcher, postProcessor, state)
        }
    }

    fun withMethodReturnType(methodReturnType: PsiType?): Converter
            = Converter(project, settings, conversionScope, referenceSearcher, postProcessor,
                        State(methodReturnType, state.expressionConverter, state.statementConverter, state.specialContext, state.importList, state.importsToAdd))

    fun withExpressionConverter(factory: (prevConverter: ExpressionConverter) -> ExpressionConverter): Converter
            = Converter(project, settings, conversionScope, referenceSearcher, postProcessor,
                        State(state.methodReturnType, factory(state.expressionConverter), state.statementConverter, state.specialContext, state.importList, state.importsToAdd))

    fun withStatementConverter(factory: (prevConverter: StatementConverter) -> StatementConverter): Converter
            = Converter(project, settings, conversionScope, referenceSearcher, postProcessor,
                        State(state.methodReturnType, state.expressionConverter, factory(state.statementConverter), state.specialContext, state.importList, state.importsToAdd))

    fun withSpecialContext(context: PsiElement): Converter
            = Converter(project, settings, conversionScope, referenceSearcher, postProcessor,
                        State(state.methodReturnType, state.expressionConverter, state.statementConverter, context, state.importList, state.importsToAdd))

    private fun withImportList(importList: ImportList): Converter
            = Converter(project, settings, conversionScope, referenceSearcher, postProcessor,
                        State(state.methodReturnType, state.expressionConverter, state.statementConverter, state.specialContext, importList, state.importsToAdd))

    private fun withImportsToAdd(importsToAdd: MutableCollection<String>): Converter
            = Converter(project, settings, conversionScope, referenceSearcher, postProcessor,
                        State(state.methodReturnType, state.expressionConverter, state.statementConverter, state.specialContext, state.importList, importsToAdd))

    public fun elementToKotlin(element: PsiElement): String {
        try {
            val converted = convertTopElement(element) ?: return ""
            val builder = CodeBuilder(element)
            builder.append(converted)

            if (postProcessor != null) {
                try {
                    return AfterConversionPass(project, postProcessor).run(builder.result)
                }
                catch(e: ProcessCanceledException) {
                    throw e
                }
                catch(t: Throwable) {
                    LOG.error(t)
                    return builder.result
                }
            }
            else {
                return builder.result
            }
        }
        catch(e: ElementCreationStackTraceRequiredException) {
            // if we got this exception then we need to turn element creation stack traces on to get better diagnostic
            Element.saveCreationStacktraces = true
            try {
                return elementToKotlin(element)
            }
            finally {
                Element.saveCreationStacktraces = false
            }
        }
    }

    private fun convertTopElement(element: PsiElement?): Element? = when (element) {
        is PsiJavaFile -> convertFile(element)
        is PsiClass -> convertClass(element)
        is PsiMethod -> convertMethod(element, null, null)
        is PsiField -> convertField(element, null)
        is PsiStatement -> convertStatement(element)
        is PsiExpression -> convertExpression(element)
        is PsiImportList -> convertImportList(element)
        is PsiImportStatementBase -> convertImport(element, false)
        is PsiAnnotation -> annotationConverter.convertAnnotation(element, false, false)
        is PsiPackageStatement -> PackageStatement(quoteKeywords(element.getPackageName() ?: "")).assignPrototype(element)
        else -> null
    }

    private fun convertFile(javaFile: PsiJavaFile): File {
        val importsToAdd = LinkedHashSet<String>()
        var converter = this.withImportsToAdd(importsToAdd)
        var convertedChildren = javaFile.getChildren().map {
            if (it is PsiImportList) {
                val importList = convertImportList(it)
                converter = converter.withImportList(importList)
                importList
            }
            else {
                converter.convertTopElement(it)
            }
        }.filterNotNull()

        if (importsToAdd.isNotEmpty()) {
            val importList = convertedChildren.filterIsInstance(javaClass<ImportList>()).first()
            val newImportList = ImportList(importList.imports + importsToAdd.map { Import(it).assignNoPrototype() }).assignPrototypesFrom(importList)
            convertedChildren = convertedChildren.map { if (it == importList) newImportList else it }
        }

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

    fun convertAnonymousClassBody(anonymousClass: PsiAnonymousClass): AnonymousClassBody {
        return AnonymousClassBody(ClassBodyConverter(anonymousClass, this).convertBody(),
                                  anonymousClass.getBaseClassType().resolve()?.isInterface() ?: false).assignPrototype(anonymousClass)
    }

    fun convertAnnotations(owner: PsiModifierListOwner): Annotations
            = annotationConverter.convertAnnotations(owner)

    fun convertClass(psiClass: PsiClass): Class {
        if (psiClass.isAnnotationType()) {
            return convertAnnotationType(psiClass)
        }

        val annotations = annotationConverter.convertAnnotations(psiClass)
        var modifiers = convertModifiers(psiClass)
        val typeParameters = convertTypeParameterList(psiClass.getTypeParameterList())
        val implementsTypes = convertToNotNullableTypes(psiClass.getImplementsListTypes())
        val extendsTypes = convertToNotNullableTypes(psiClass.getExtendsListTypes())
        val name = psiClass.declarationIdentifier()

        var classBody = ClassBodyConverter(psiClass, this).convertBody()

        return when {
            psiClass.isInterface() -> Trait(name, annotations, modifiers, typeParameters, extendsTypes, listOf(), implementsTypes, classBody)

            psiClass.isEnum() -> Enum(name, annotations, modifiers, typeParameters, listOf(), listOf(), implementsTypes, classBody)

            else -> {
                if (settings.openByDefault && !psiClass.hasModifierProperty(PsiModifier.FINAL)) {
                    modifiers = modifiers.with(Modifier.OPEN)
                }

                if (psiClass.getContainingClass() != null && !psiClass.hasModifierProperty(PsiModifier.STATIC)) {
                    modifiers = modifiers.with(Modifier.INNER)
                }

                Class(name, annotations, modifiers, typeParameters, extendsTypes, classBody.baseClassParams, implementsTypes, classBody)
            }
        }.assignPrototype(psiClass)
    }

    private fun convertAnnotationType(psiClass: PsiClass): Class {
        val paramModifiers = Modifiers(listOf(Modifier.PUBLIC)).assignNoPrototype()
        val noBlankLinesInheritance = CommentsAndSpacesInheritance(blankLinesBefore = false)
        val annotationMethods = psiClass.getMethods().filterIsInstance(javaClass<PsiAnnotationMethod>())
        val parameters = annotationMethods
                .map { method ->
                    val returnType = method.getReturnType()
                    val typeConverted = if (method == annotationMethods.last && returnType is PsiArrayType)
                        VarArgType(typeConverter.convertType(returnType.getComponentType(), Nullability.NotNull))
                    else
                        typeConverter.convertType(returnType, Nullability.NotNull)
                    typeConverted.assignPrototype(method.getReturnTypeElement(), noBlankLinesInheritance)

                    Parameter(method.declarationIdentifier(),
                              typeConverted,
                              Parameter.VarValModifier.Val,
                              convertAnnotations(method),
                              paramModifiers,
                              annotationConverter.convertAnnotationMethodDefault(method)).assignPrototype(method, noBlankLinesInheritance)
                }
        val parameterList = ParameterList(parameters).assignNoPrototype()
        val constructorSignature = PrimaryConstructorSignature(Annotations.Empty, Modifiers.Empty, parameterList).assignNoPrototype()

        // to convert fields and nested types - they are not allowed in Kotlin but we convert them and let user refactor code
        var classBody = ClassBodyConverter(psiClass, this).convertBody()
        classBody = ClassBody(constructorSignature, classBody.baseClassParams, classBody.members, classBody.classObjectMembers, listOf(), classBody.lBrace, classBody.rBrace)

        val annotationAnnotation = Annotation(Identifier("annotation").assignNoPrototype(), listOf(), false, false).assignNoPrototype()
        return Class(psiClass.declarationIdentifier(),
                     (convertAnnotations(psiClass) + Annotations(listOf(annotationAnnotation))).assignNoPrototype(),
                     convertModifiers(psiClass).without(Modifier.ABSTRACT),
                     TypeParameterList.Empty,
                     listOf(),
                     listOf(),
                     listOf(),
                     classBody).assignPrototype(psiClass)
    }

    fun convertInitializer(initializer: PsiClassInitializer): Initializer {
        return Initializer(convertBlock(initializer.getBody()), convertModifiers(initializer)).assignPrototype(initializer)
    }

    fun convertField(field: PsiField, correction: FieldCorrectionInfo?): Member {
        val annotations = annotationConverter.convertAnnotations(field)

        var modifiers = convertModifiers(field)
        if (correction != null) {
            modifiers = modifiers.without(modifiers.accessModifier()).with(correction.access)
        }

        val name = correction?.name ?: field.declarationIdentifier()
        val converted = if (field is PsiEnumConstant) {
            val argumentList = field.getArgumentList()
            EnumConstant(name,
                         annotations,
                         modifiers,
                         typeConverter.convertType(field.getType(), Nullability.NotNull),
                         ExpressionList(convertExpressions(argumentList?.getExpressions() ?: array())).assignPrototype(argumentList))
        }
        else {
            val isVal = isVal(referenceSearcher, field)
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
                  initializer.isEmpty && shouldGenerateDefaultInitializer(referenceSearcher, field),
                  if (correction != null) correction.setterAccess else modifiers.accessModifier())
        }
        return converted.assignPrototype(field)
    }

    private fun variableTypeToDeclare(variable: PsiVariable, specifyAlways: Boolean, canChangeType: Boolean): Type? {
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

    fun convertMethod(method: PsiMethod, membersToRemove: MutableSet<PsiMember>?, constructorConverter: ConstructorConverter?): Member? {
        return withMethodReturnType(method.getReturnType()).doConvertMethod(method, membersToRemove, constructorConverter)?.assignPrototype(method)
    }

    private fun doConvertMethod(method: PsiMethod, membersToRemove: MutableSet<PsiMember>?, constructorConverter: ConstructorConverter?): Member? {
        val returnType = typeConverter.convertMethodReturnType(method)

        val annotations = (annotationConverter.convertAnnotations(method) + convertThrows(method)).assignNoPrototype()
        var modifiers = convertModifiers(method)

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

        if (method.isConstructor() && constructorConverter != null) {
            return constructorConverter.convertConstructor(method, annotations, modifiers, membersToRemove!!, postProcessBody)
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

        return state.statementConverter.convertStatement(statement, this).assignPrototype(statement)
    }

    fun convertExpressions(expressions: Array<PsiExpression>): List<Expression>
            = expressions.map { convertExpression(it) }

    fun convertExpression(expression: PsiExpression?): Expression {
        if (expression == null) return Expression.Empty

        return state.expressionConverter.convertExpression(expression, this).assignPrototype(expression)
    }

    fun convertLocalVariable(variable: PsiLocalVariable): LocalVariable {
        val isVal = variable.hasModifierProperty(PsiModifier.FINAL) ||
                variable.getInitializer() == null/* we do not know actually and prefer val until we have better analysis*/ ||
                !variable.hasWriteAccesses(referenceSearcher, variable.getContainingMethod())
        return LocalVariable(variable.declarationIdentifier(),
                             annotationConverter.convertAnnotations(variable),
                             convertModifiers(variable),
                             variableTypeToDeclare(variable, settings.specifyLocalVariableTypeByDefault, isVal),
                             convertExpression(variable.getInitializer(), variable.getType()),
                             isVal).assignPrototype(variable)
    }

    fun convertCodeReferenceElement(element: PsiJavaCodeReferenceElement, hasExternalQualifier: Boolean, typeArgsConverted: List<Element>? = null): ReferenceElement {
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

    fun convertTypeElement(element: PsiTypeElement?): TypeElement
            = TypeElement(if (element == null) ErrorType().assignNoPrototype() else typeConverter.convertType(element.getType())).assignPrototype(element)

    private fun convertToNotNullableTypes(types: Array<out PsiType?>): List<Type>
            = types.map { typeConverter.convertType(it, Nullability.NotNull) }

    fun convertParameterList(parameterList: PsiParameterList): ParameterList
            = ParameterList(parameterList.getParameters().map { convertParameter(it) }).assignPrototype(parameterList)

    fun convertParameter(parameter: PsiParameter,
                         nullability: Nullability = Nullability.Default,
                         varValModifier: Parameter.VarValModifier = Parameter.VarValModifier.None,
                         modifiers: Modifiers = Modifiers.Empty,
                         defaultValue: Expression? = null): Parameter {
        var type = typeConverter.convertVariableType(parameter)
        when (nullability) {
            Nullability.NotNull -> type = type.toNotNullType()
            Nullability.Nullable -> type = type.toNullableType()
        }
        return Parameter(parameter.declarationIdentifier(), type, varValModifier,
                         annotationConverter.convertAnnotations(parameter), modifiers, defaultValue).assignPrototype(parameter)
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

        if (needConversion(actualType, expectedType) && convertedExpression !is LiteralExpression) {
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

        if (needConversion(actualType, expectedType) && convertedExpression !is LiteralExpression) {
            val conversion = PRIMITIVE_TYPE_CONVERSIONS[expectedType.getCanonicalText()]
            if (conversion != null) {
                resultType = typeConverter.convertType(expectedType, Nullability.NotNull)
            }
        }

        return resultType
    }

    private fun needConversion(actual: PsiType, expected: PsiType): Boolean {
        val expectedStr = expected.getCanonicalText()
        val actualStr = actual.getCanonicalText()
        return expectedStr != actualStr &&
                expectedStr != typeConversionMap[actualStr] &&
                actualStr != typeConversionMap[expectedStr]
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
        val annotation = Annotation(Identifier("throws").assignNoPrototype(), arguments, false, true)
        return Annotations(listOf(annotation.assignPrototype(throwsList))).assignPrototype(throwsList)
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
