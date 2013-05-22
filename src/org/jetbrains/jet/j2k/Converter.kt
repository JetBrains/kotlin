package org.jetbrains.jet.j2k

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.intellij.psi.*
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.ast.Class
import org.jetbrains.jet.j2k.ast.Enum
import org.jetbrains.jet.j2k.ast.types.ClassType
import org.jetbrains.jet.j2k.ast.types.EmptyType
import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.visitors.*
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import java.util.*
import com.intellij.psi.CommonClassNames.*
import org.jetbrains.jet.lang.types.expressions.OperatorConventions.*
import com.intellij.openapi.util.Pair
import java.text.MessageFormat
import com.intellij.psi.util.PsiUtil
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import com.intellij.openapi.project.Project
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import com.intellij.openapi.Disposable
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.util.QualifiedNamesUtil

public open class Converter() {

    private var classIdentifiersSet: MutableSet<String> = Sets.newHashSet()!!

    private val dispatcher: Dispatcher = Dispatcher(this)

    private val flags: MutableSet<J2KConverterFlags?>? = Sets.newHashSet()

    private val jetCoreEnvironment = JetCoreEnvironment(
                                                object : Disposable {
                                                    public override fun dispose() {
                                                    }
                                                }, CompilerConfiguration())


    private val project = jetCoreEnvironment.getProject();

    {
        KotlinBuiltIns.initialize(project, KotlinBuiltIns.InitializationMode.MULTI_THREADED)
    }

    private val javaToKotlinClassMap: JavaToKotlinClassMap = JavaToKotlinClassMap.getInstance()

    public open var methodReturnType: PsiType? = null
        private set

    public open fun addFlag(flag: J2KConverterFlags): Boolean {
        return flags?.add(flag)!!
    }

    public open fun hasFlag(flag: J2KConverterFlags): Boolean {
        return flags?.contains(flag)!!
    }

    public open fun setClassIdentifiers(identifiers: MutableSet<String>) {
        classIdentifiersSet = identifiers
    }

    public open fun getClassIdentifiers(): Set<String> {
        return Collections.unmodifiableSet(classIdentifiersSet)
    }

    public open fun clearClassIdentifiers(): Unit {
        classIdentifiersSet.clear()
    }

    public open fun elementToKotlin(element: PsiElement): String {
        val kElement = topElementToElement(element)
        return kElement?.toKotlin() ?: ""
    }

    public open fun topElementToElement(element: PsiElement?): Node? = when(element) {
        is PsiJavaFile -> fileToFile(element)
        is PsiClass -> classToClass(element)
        is PsiMethod -> methodToFunction(element)
        is PsiField -> fieldToField(element, element.getContainingClass())
        is PsiStatement -> statementToStatement(element)
        is PsiExpression -> expressionToExpression(element)
        is PsiComment -> Comment(element.getText()!!)
        else -> null
    }

    public open fun fileToFile(javaFile: PsiJavaFile): File {
        return fileToFile(javaFile, Collections.emptyList<String>())
    }

    public open fun fileToFileWithCompatibilityImport(javaFile: PsiJavaFile): File {
        return fileToFile(javaFile, Collections.singletonList("kotlin.compatibility.*"))
    }

    private fun fileToFile(javaFile: PsiJavaFile, additionalImports: List<String>): File {
        val importList: PsiImportList? = javaFile.getImportList()
        val imports: MutableList<Import> = (if (importList == null)
            arrayList()
        else
            ArrayList(importsToImportList(importList.getAllImportStatements()) filter {
                // If name is invalid, like with star imports, don't try to filter
                if (!QualifiedNamesUtil.isValidJavaFqName(it.name))
                    true
                else {
                    // If imported class has a kotlin analog, drop the import
                    val kotlinAnalogsForClass = javaToKotlinClassMap.mapPlatformClass(FqName(it.name))
                    kotlinAnalogsForClass.isEmpty()
                }
            }))
        for (i : String in additionalImports)
            imports.add(Import(i))

        val body: ArrayList<Node> = arrayList()
        for(element in javaFile.getChildren()) {
            if (element !is PsiImportStatementBase) {
                val node = topElementToElement(element)
                if (node != null) {
                    body.add(node)
                }
            }
        }
        return File(quoteKeywords(javaFile.getPackageName()), imports, body, createMainFunction(javaFile))
    }

    public open fun anonymousClassToAnonymousClass(anonymousClass: PsiAnonymousClass): AnonymousClass {
        return AnonymousClass(this, getMembers(anonymousClass))
    }

    private fun getMembers(psiClass: PsiClass): MutableList<Node> {
        val members = ArrayList<Node>()
        val lbraceOffset = psiClass.getLBrace()?.getTextRange()?.getStartOffset() ?: 0
        for (e : PsiElement? in psiClass.getChildren()) {
            val isDocComment = e?.getTextRange()?.getStartOffset() ?: 0 < lbraceOffset
            if (isDocComment) continue
            val converted = memberToMember(e, psiClass)
            if (converted != null) members.add(converted)
        }
        return members
    }

    private fun getDocComments(element: PsiElement): List<Node> {
        val comments = ArrayList<Node>()
        val textOffset = element.getTextOffset()
        for (e : PsiElement? in element.getChildren()) {
            if (e is PsiComment && e.getTextRange()?.getStartOffset() ?: 0 < textOffset) {
                comments.add(Comment(e.getText()!!));
            }
        }
        return comments
    }

    private fun memberToMember(e: PsiElement?, containingClass: PsiClass): Node? = when(e) {
        is PsiMethod -> methodToFunction(e, true)
        is PsiField -> fieldToField(e, containingClass)
        is PsiClass -> classToClass(e)
        is PsiClassInitializer -> initializerToInitializer(e)
        is PsiComment -> Comment(e.getText()!!)
        else -> null
    }

    private fun classToClass(psiClass: PsiClass): Class {
        val modifiers: Set<Modifier> = modifiersListToModifiersSet(psiClass.getModifierList())
        val fields: List<Field> = fieldsToFieldList(psiClass.getFields(), psiClass)
        val typeParameters: List<Element> = elementsToElementList(psiClass.getTypeParameters())
        val implementsTypes: List<Type> = typesToNotNullableTypeList(psiClass.getImplementsListTypes())
        val extendsTypes: List<Type> = typesToNotNullableTypeList(psiClass.getExtendsListTypes())
        val name: Identifier = Identifier(psiClass.getName()!!)
        val baseClassParams= ArrayList<Expression>()
        val members = getMembers(psiClass)
        val docComments = getDocComments(psiClass)
        val visitor: SuperVisitor = SuperVisitor()
        psiClass.accept(visitor)
        val resolvedSuperCallParameters = visitor.resolvedSuperCallParameters
        if (resolvedSuperCallParameters.size() == 1) {
            val psiExpressionList = resolvedSuperCallParameters.iterator().next()
            baseClassParams.addAll(expressionsToExpressionList(psiExpressionList.getExpressions()))
        }

        if (!psiClass.isEnum() && !psiClass.isInterface() && psiClass.getConstructors().size > 1 &&
        getPrimaryConstructorForThisCase(psiClass) == null) {
            val finalOrWithEmptyInitializer: List<Field> = getFinalOrWithEmptyInitializer(fields)
            val initializers = HashMap<String, String>()
            for (m in members) {
                if (m is Constructor) {
                    if (!m.isPrimary) {
                        for (fo in finalOrWithEmptyInitializer){
                            val init: String = getDefaultInitializer(fo)
                            initializers.put(fo.identifier.toKotlin(), init)
                        }
                        val newStatements = ArrayList<Element>()
                        for (s in m.block!!.statements) {
                            var isRemoved: Boolean = false
                            if (s is AssignmentExpression) {
                                val assignee = s.left
                                if (assignee is CallChainExpression) {
                                    for (fo : Field in finalOrWithEmptyInitializer) {
                                        val id: String = fo.identifier.toKotlin()
                                        if (assignee.identifier.toKotlin().endsWith("." + id)) {
                                            initializers.put(id, s.right.toKotlin())
                                            isRemoved = true
                                        }

                                    }
                                }

                            }

                            if (!isRemoved) {
                                newStatements.add(s)
                            }

                        }
                        newStatements.add(0, DummyStringExpression("val __ = " + createPrimaryConstructorInvocation(name.toKotlin(), finalOrWithEmptyInitializer, initializers)))
                        m.block = Block(newStatements)
                    }
                }
            }
            members.add(Constructor(Identifier.EMPTY_IDENTIFIER, arrayList(), Collections.emptySet<Modifier>(),
                    ClassType(name, Collections.emptyList<Element>(), false),
                    Collections.emptyList<Element>(),
                    ParameterList(createParametersFromFields(finalOrWithEmptyInitializer)),
                    Block(createInitStatementsFromFields(finalOrWithEmptyInitializer)),
                    true))
        }

        if (psiClass.isInterface()) {
            return Trait(this, name, docComments, modifiers, typeParameters, extendsTypes, Collections.emptyList<Expression>(), implementsTypes, members)
        }

        if (psiClass.isEnum()) {
            return Enum(this, name, docComments, modifiers, typeParameters, Collections.emptyList<Type>(), Collections.emptyList<Expression>(), implementsTypes, members)
        }

        return Class(this, name, docComments, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, members)
    }

    private fun initializerToInitializer(i: PsiClassInitializer): Initializer {
        return Initializer(blockToBlock(i.getBody(), true), modifiersListToModifiersSet(i.getModifierList()))
    }

    private fun fieldsToFieldList(fields: Array<PsiField>, psiClass: PsiClass): List<Field> {
        return fields.map { fieldToField(it, psiClass) }
    }

    private fun fieldToField(field: PsiField, psiClass: PsiClass?): Field {
        val modifiers = modifiersListToModifiersSet(field.getModifierList())
        val docComments = getDocComments(field)
        if (field is PsiEnumConstant) {
            return EnumConstant(Identifier(field.getName()!!),
                    docComments,
                    modifiers,
                    typeToType(field.getType()),
                    elementToElement(field.getArgumentList()))
        }

        var kType = typeToType(field.getType(), isAnnotatedAsNotNull(field.getModifierList()))
        if (field.hasModifierProperty(PsiModifier.FINAL) && isDefinitelyNotNull(field.getInitializer())) {
            kType = kType.convertedToNotNull();
        }

        return Field(Identifier(field.getName()!!),
                docComments,
                modifiers,
                kType,
                expressionToExpression(field.getInitializer(), field.getType()),
                countWritingAccesses(field, psiClass))
    }

    private fun methodToFunction(method: PsiMethod): Function {
        return methodToFunction(method, true)
    }

    private fun methodToFunction(method: PsiMethod, notEmpty: Boolean): Function {
        if (isOverrideObjectDirect(method)) {
            dispatcher.expressionVisitor = ExpressionVisitorForDirectObjectInheritors(this)
        }
        else {
            dispatcher.expressionVisitor = ExpressionVisitor(this)
        }
        methodReturnType = method.getReturnType()
        val identifier: Identifier = Identifier(method.getName())
        val returnType: Type = typeToType(method.getReturnType(), isAnnotatedAsNotNull(method.getModifierList()))
        val body: Block = (if (hasFlag(J2KConverterFlags.SKIP_BODIES))
            Block.EMPTY_BLOCK
        else
            blockToBlock(method.getBody(), notEmpty))

        val params: Element = createFunctionParameters(method)
        val typeParameters = elementsToElementList(method.getTypeParameters())
        val modifiers = modifiersListToModifiersSet(method.getModifierList())
        val docComments = getDocComments(method)
        if (isOverrideAnyMethodExceptMethodsFromObject(method)) {
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
            val isPrimary: Boolean = isConstructorPrimary(method)
            return Constructor(identifier, docComments, modifiers, returnType, typeParameters, params,
                    Block(removeEmpty(body.statements), false), isPrimary)
        }

        return Function(identifier, docComments, modifiers, returnType, typeParameters, params, body)
    }

    private fun createFunctionParameters(method: PsiMethod): ParameterList {
        val result = ArrayList<Parameter>()
        for (parameter : PsiParameter? in method.getParameterList().getParameters()) {
            result.add(Parameter(Identifier(parameter?.getName()!!),
                    typeToType(parameter?.getType(),
                            isAnnotatedAsNotNull(parameter?.getModifierList())),
                    isReadOnly(parameter, method.getBody())))
        }
        return ParameterList(result)
    }

    private fun isOverrideAnyMethodExceptMethodsFromObject(method: PsiMethod): Boolean {
        var counter: Boolean = normalCase(method)
        if (counter)
        {
            return true
        }

        if (isInheritFromObject(method))
        {
            return caseForObject(method)
        }

        return false
    }

    private fun caseForObject(method: PsiMethod): Boolean {
        val containing: PsiClass? = method.getContainingClass()
        if (containing != null) {
            for (s : PsiClassType? in containing.getSuperTypes()) {
                val canonicalText: String? = s?.getCanonicalText()
                if (canonicalText != JAVA_LANG_OBJECT && !getClassIdentifiers().contains(canonicalText)) {
                    return true
                }
            }
        }

        return false
    }
    public open fun blockToBlock(block: PsiCodeBlock?, notEmpty: Boolean): Block {
        if (block == null)
            return Block.EMPTY_BLOCK

        return Block(removeEmpty(statementsToStatementList(block.getChildren())), notEmpty)
    }

    public open fun blockToBlock(block: PsiCodeBlock?): Block {
        return blockToBlock(block, true)
    }

    public open fun statementsToStatementList(statements: Array<PsiElement>): List<Element> {
        return statements.filterNot { it is PsiWhiteSpace }.map { statementToStatement(it) }
    }

    public open fun statementsToStatementList(statements: List<PsiElement>): List<Element> {
        return statements.filterNot { it is PsiWhiteSpace }.map { statementToStatement(it) }
    }

    public open fun statementToStatement(s: PsiElement?): Element {
        if (s == null)
            return Statement.EMPTY_STATEMENT

        val statementVisitor: StatementVisitor = StatementVisitor(this)
        s.accept(statementVisitor)
        return statementVisitor.getResult()
    }

    public open fun expressionsToExpressionList(expressions: Array<PsiExpression>): List<Expression> {
        val result = ArrayList<Expression>()
        for (e : PsiExpression? in expressions)
            result.add(expressionToExpression(e))
        return result
    }

    public open fun expressionToExpression(e: PsiExpression?): Expression {
        if (e == null)
            return Expression.EMPTY_EXPRESSION

        val expressionVisitor: ExpressionVisitor = dispatcher.expressionVisitor
        e.accept(expressionVisitor)
        return expressionVisitor.getResult()
    }

    public open fun elementToElement(e: PsiElement?): Element {
        if (e == null)
            return Element.EMPTY_ELEMENT

        val elementVisitor: ElementVisitor = ElementVisitor(this)
        e.accept(elementVisitor)
        return elementVisitor.getResult()
    }

    public open fun elementsToElementList(elements: Array<out PsiElement?>): List<Element> {
        val result = ArrayList<Element>()
        for(element in elements) {
            result.add(elementToElement(element))
        }
        return result
    }

    public open fun typeElementToTypeElement(element: PsiTypeElement?): TypeElement {
        return TypeElement(if (element == null)
            EmptyType()
        else
            typeToType(element.getType()))
    }

    public open fun typeToType(`type`: PsiType?): Type {
        if (`type` == null)
            return EmptyType()

        val typeVisitor: TypeVisitor = TypeVisitor(this)
        `type`.accept<Type>(typeVisitor)
        return typeVisitor.getResult()
    }

    public open fun typesToTypeList(types: Array<PsiType>): List<Type> {
        return types.map { typeToType(it) }
    }

    public open fun typeToType(`type`: PsiType?, notNull: Boolean): Type {
        val result: Type = typeToType(`type`)
        if (notNull) {
            return result.convertedToNotNull()
        }

        return result
    }

    private fun typesToNotNullableTypeList(types: Array<out PsiType?>): List<Type> {
        val result = ArrayList<Type>()
        for(aType in types) {
            result.add(typeToType(aType).convertedToNotNull())
        }
        return result
    }

    public open fun parametersToParameterList(parameters: Array<PsiParameter>): List<Parameter?> {
        return parameters.map { parameterToParameter(it) }
    }

    public open fun parameterToParameter(parameter: PsiParameter, forceNotNull: Boolean = false): Parameter {
        return Parameter(Identifier(parameter.getName()!!),
                typeToType(parameter.getType(),
                        forceNotNull || isAnnotatedAsNotNull(parameter.getModifierList())), true)
    }

    public open fun argumentsToExpressionList(expression: PsiCallExpression): List<Expression> {
        val argumentList: PsiExpressionList? = expression.getArgumentList()
        val arguments: Array<PsiExpression> = (if (argumentList != null)
            argumentList.getExpressions()
        else
            PsiExpression.EMPTY_ARRAY)
        val result = ArrayList<Expression>()
        val resolved: PsiMethod? = expression.resolveMethod()
        val expectedTypes = ArrayList<PsiType?>()
        if (resolved != null) {
            for (p : PsiParameter? in resolved.getParameterList().getParameters())
                expectedTypes.add(p?.getType())
        }

        if (arguments.size == expectedTypes.size()) {
            for (i in 0..expectedTypes.size() - 1) result.add(expressionToExpression(arguments[i], expectedTypes.get(i)))
        }
        else {
            for (argument : PsiExpression? in arguments) {
                result.add(expressionToExpression(argument))
            }
        }
        return result
    }

    public open fun expressionToExpression(argument: PsiExpression?, expectedType: PsiType?): Expression {
        if (argument == null)
            return Identifier.EMPTY_IDENTIFIER

        var expression: Expression = expressionToExpression(argument)
        val actualType: PsiType? = argument.getType()
        val isPrimitiveTypeOrNull: Boolean = actualType == null || actualType is PsiPrimitiveType
        if (isPrimitiveTypeOrNull && expression.isNullable()) {
            expression = BangBangExpression(expression)
        }
        else if (expectedType is PsiPrimitiveType && actualType is PsiClassType) {
            if (PsiPrimitiveType.getUnboxedType(actualType) == expectedType) {
                expression = BangBangExpression(expression)
            }
        }

        if (actualType != null) {
            if (isConversionNeeded(actualType, expectedType) && !(expression is LiteralExpression))
            {
                val conversion: String? = PRIMITIVE_TYPE_CONVERSIONS.get(expectedType?.getCanonicalText())
                if (conversion != null) {
                    expression = MethodCallExpression.build(expression, conversion)
                }
            }

        }

        return expression
    }

    class object {
        public val NOT_NULL_ANNOTATIONS: Set<String> = ImmutableSet.of<String>("org.jetbrains.annotations.NotNull", "com.sun.istack.internal.NotNull", "javax.annotation.Nonnull")!!
        public val PRIMITIVE_TYPE_CONVERSIONS: Map<String, String> = ImmutableMap.builder<String, String>()
                ?.put("byte", BYTE.asString())
                ?.put("short", SHORT.asString())
                ?.put("int", INT.asString())
                ?.put("long", LONG.asString())
                ?.put("float", FLOAT.asString())
                ?.put("double", DOUBLE.asString())
                ?.put("char", CHAR.asString())
                ?.put(JAVA_LANG_BYTE, BYTE.asString())
                ?.put(JAVA_LANG_SHORT, SHORT.asString())
                ?.put(JAVA_LANG_INTEGER, INT.asString())
                ?.put(JAVA_LANG_LONG, LONG.asString())
                ?.put(JAVA_LANG_FLOAT, FLOAT.asString())
                ?.put(JAVA_LANG_DOUBLE, DOUBLE.asString())
                ?.put(JAVA_LANG_CHARACTER, CHAR.asString())
                ?.build()!!

        private fun quoteKeywords(packageName: String): String {
            return packageName.split("\\.").map { Identifier(it).toKotlin() }.makeString(".")
        }

        private fun getFinalOrWithEmptyInitializer(fields: List<Field>): List<Field> {
            val result = ArrayList<Field>()
            for (f : Field in fields)
                if (f.isVal() || f.initializer.toKotlin().isEmpty()) {
                    result.add(f)
                }

            return result
        }

        private fun createParametersFromFields(fields: List<Field>): List<Parameter> {
            return fields.map { Parameter(Identifier("_" + it.identifier.name), it.`type`, true) }
        }

        private fun createInitStatementsFromFields(fields: List<Field>): List<Element> {
            val result = ArrayList<Element>()
            for (f : Field in fields) {
                val identifierToKotlin: String? = f.identifier.toKotlin()
                result.add(DummyStringExpression(identifierToKotlin + " = " + "_" + identifierToKotlin))
            }
            return result
        }

        private fun createPrimaryConstructorInvocation(s: String, fields: List<Field>, initializers: Map<String, String>): String {
            return s + "(" + fields.map { initializers[it.identifier.toKotlin()] }.makeString(", ") + ")"
        }

        public open fun getDefaultInitializer(f: Field): String {
            if (f.`type`.nullable) {
                return "null"
            }
            else {
                val typeToKotlin: String = f.`type`.toKotlin()
                if (typeToKotlin.equals("Boolean"))
                    return "false"

                if (typeToKotlin.equals("Char"))
                    return "' '"

                if (typeToKotlin.equals("Double"))
                    return "0." + OperatorConventions.DOUBLE + "()"

                if (typeToKotlin.equals("Float"))
                    return "0." + OperatorConventions.FLOAT + "()"

                return "0"
            }
        }

        private fun getPrimaryConstructorForThisCase(psiClass: PsiClass): PsiMethod? {
            val tv = ThisVisitor()
            psiClass.accept(tv)
            return tv.getPrimaryConstructor()
        }

        public open fun isConstructorPrimary(constructor: PsiMethod): Boolean {
            val parent = constructor.getParent()
            if (parent is PsiClass) {
                if (parent.getConstructors().size == 1) {
                    return true
                }
                else {
                    val c: PsiMethod? = getPrimaryConstructorForThisCase(parent)
                    if (c != null && c.hashCode() == constructor.hashCode()) {
                        return true
                    }

                }
            }

            return false
        }
        private fun removeEmpty(statements: List<Element>): List<Element> {
            return statements.filterNot { it == Statement.EMPTY_STATEMENT ||
                                          it == Expression.EMPTY_EXPRESSION ||
                                          it == Element.EMPTY_ELEMENT }
        }

        private fun isNotOpenMethod(method: PsiMethod): Boolean {
            val parent = method.getParent()
            if (parent is PsiClass) {
                val parentModifierList: PsiModifierList? = parent.getModifierList()
                if ((parentModifierList != null && parentModifierList.hasExplicitModifier(PsiModifier.FINAL)) || parent.isEnum()) {
                    return true
                }

            }

            return false
        }

        private fun normalCase(method: PsiMethod): Boolean {
            var counter: Int = 0
            for (s : HierarchicalMethodSignature? in method.getHierarchicalMethodSignature().getSuperSignatures())
            {
                var containingClass: PsiClass? = s?.getMethod()?.getContainingClass()
                var qualifiedName: String? = (if (containingClass != null)
                    containingClass?.getQualifiedName()
                else
                    "")
                if (qualifiedName != null && !qualifiedName.equals(JAVA_LANG_OBJECT))
                {
                    counter++
                }

            }
            return counter > 0
        }

        private fun isInheritFromObject(method: PsiMethod): Boolean {
            var superSignatures: List<HierarchicalMethodSignature?> = method.getHierarchicalMethodSignature().getSuperSignatures()
            for (s : HierarchicalMethodSignature? in superSignatures) {
                var containingClass: PsiClass? = s?.getMethod()?.getContainingClass()
                var qualifiedName: String? = (if (containingClass != null)
                    containingClass?.getQualifiedName()
                else
                    "")
                if (qualifiedName == JAVA_LANG_OBJECT) {
                    return true
                }

            }
            return false
        }

        private fun isOverrideObjectDirect(method: PsiMethod): Boolean {
            var superSignatures: List<HierarchicalMethodSignature?>? = method.getHierarchicalMethodSignature().getSuperSignatures()
            if (superSignatures?.size()!! == 1)
            {
                val containingClass: PsiClass? = superSignatures?.get(0)?.getMethod()?.getContainingClass()
                val qualifiedName: String? = (if (containingClass != null)
                    containingClass.getQualifiedName()
                else
                    "")
                if (qualifiedName == JAVA_LANG_OBJECT) {
                    return true
                }

            }

            return false
        }
        private fun importsToImportList(imports: Array<PsiImportStatementBase>): List<Import> {
            val result = ArrayList<Import>()
            for (i : PsiImportStatementBase? in imports) {
                if (i == null) continue
                val anImport: Import = importToImport(i)
                val name: String = anImport.name
                if (!name.isEmpty() && !NOT_NULL_ANNOTATIONS.contains(name)) {
                    result.add(anImport)
                }

            }
            return result
        }

        private fun importToImport(i: PsiImportStatementBase): Import {
            val reference: PsiJavaCodeReferenceElement? = i.getImportReference()
            if (reference != null) {
                return Import(quoteKeywords(reference.getQualifiedName()!!) + ((if (i.isOnDemand())
                    ".*"
                else
                    "")))
            }

            return Import("")
        }

        public open fun identifierToIdentifier(identifier: PsiIdentifier?): Identifier {
            if (identifier == null)
                return Identifier.EMPTY_IDENTIFIER

            return Identifier(identifier.getText()!!)
        }

        public open fun modifiersListToModifiersSet(modifierList: PsiModifierList?): MutableSet<Modifier> {
            val modifiersSet: HashSet<Modifier> = hashSet()
            if (modifierList != null) {
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
            }

            return modifiersSet
        }

        private fun isConversionNeeded(actual: PsiType?, expected: PsiType?): Boolean {
            if (actual == null || expected == null) {
                return false
            }

            val typeMap = HashMap<String, String>()
            typeMap.put(JAVA_LANG_BYTE, "byte")
            typeMap.put(JAVA_LANG_SHORT, "short")
            typeMap.put(JAVA_LANG_INTEGER, "int")
            typeMap.put(JAVA_LANG_LONG, "long")
            typeMap.put(JAVA_LANG_FLOAT, "float")
            typeMap.put(JAVA_LANG_DOUBLE, "double")
            typeMap.put(JAVA_LANG_CHARACTER, "char")
            val expectedStr: String? = expected.getCanonicalText()
            val actualStr: String? = actual.getCanonicalText()
            val o1: Boolean = expectedStr == typeMap[actualStr]
            val o2: Boolean = actualStr == typeMap[expectedStr]
            return actualStr != expectedStr && (!(o1 xor o2))
        }
    }
}

public fun createMainFunction(file: PsiFile): String {
    val classNamesWithMains = ArrayList<Pair<String?, PsiMethod?>?>()
    for (c : PsiClass? in (file as PsiJavaFile).getClasses()) {
        var main: PsiMethod? = findMainMethod(c)
        if (main != null) {
            classNamesWithMains.add(Pair<String?, PsiMethod?>(c?.getName(), main))
        }
    }

    if (classNamesWithMains.size() > 0) {
        var className: String? = classNamesWithMains.get(0)?.getFirst()
        return MessageFormat.format("fun main(args : Array<String>) = {0}.main(args as Array<String?>?)", className)!!
    }

    return ""

}

private fun findMainMethod(aClass: PsiClass?): PsiMethod? {
    if (isMainClass(aClass)) {
        val mainMethods: Array<PsiMethod>? = aClass?.findMethodsByName("main", false)
        if (mainMethods != null) {
            return findMainMethod(mainMethods)
        }
    }
    return null
}

private fun isMainClass(psiClass: PsiClass?): Boolean {
    if (psiClass == null || psiClass is PsiAnonymousClass)
        return false

    if (psiClass.isInterface())
        return false

    return psiClass.getContainingClass() == null || psiClass.hasModifierProperty(PsiModifier.STATIC)

}

private fun findMainMethod(mainMethods: Array<PsiMethod>): PsiMethod? {
    return mainMethods.find { isMainMethod(it) }
}

public fun isMainMethod(method: PsiMethod?): Boolean {
    if (method == null || method.getContainingClass() == null)
        return false

    if (PsiType.VOID != method.getReturnType())
        return false

    if (!method.hasModifierProperty(PsiModifier.STATIC))
        return false

    if (!method.hasModifierProperty(PsiModifier.PUBLIC))
        return false

    val parameters: Array<PsiParameter>? = method.getParameterList().getParameters()
    if (parameters?.size!! != 1)
        return false

    val `type`: PsiType? = parameters!![0].getType()
    if (`type` !is PsiArrayType)
        return false

    val componentType: PsiType? = `type`.getComponentType()
    return componentType?.equalsToText("java.lang.String")!!
}

public fun countWritingAccesses(element: PsiElement?, container: PsiElement?): Int {
    var counter: Int = 0
    if (container != null) {
        val visitor: ReferenceCollector = ReferenceCollector()
        container.accept(visitor)
        for (e : PsiReferenceExpression in visitor.getCollectedReferences())
            if (e.isReferenceTo(element) && PsiUtil.isAccessedForWriting(e)) {
                counter++
            }
    }

    return counter
}

open class ReferenceCollector(): JavaRecursiveElementVisitor() {
    private val myCollectedReferences = ArrayList<PsiReferenceExpression>()

    public open fun getCollectedReferences(): List<PsiReferenceExpression> {
        return myCollectedReferences
    }

    public override fun visitReferenceExpression(expression: PsiReferenceExpression?): Unit {
        super.visitReferenceExpression(expression)
        if (expression != null) {
            myCollectedReferences.add(expression)
        }
    }
}

public fun isReadOnly(element: PsiElement?, container: PsiElement?): Boolean {
    return countWritingAccesses(element, container) == 0
}

public fun isAnnotatedAsNotNull(modifierList: PsiModifierList?): Boolean {
    if (modifierList != null) {
        val annotations: Array<PsiAnnotation> = modifierList.getAnnotations()
        for (a : PsiAnnotation in annotations) {
            val qualifiedName: String? = a.getQualifiedName()
            if (qualifiedName != null && Converter.NOT_NULL_ANNOTATIONS.contains(qualifiedName)) {
                return true
            }
        }
    }
    return false
}

public fun isDefinitelyNotNull(element: PsiElement?): Boolean = when(element) {
    is PsiLiteralExpression -> element.getValue() != null
    is PsiNewExpression -> true
    else -> false
}
