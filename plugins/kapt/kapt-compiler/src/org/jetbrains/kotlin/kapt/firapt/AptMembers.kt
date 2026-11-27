/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.codegen.state.isMethodWithDeclarationSiteWildcardsFqName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.FirJavaTypeParameter
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resultOrNull
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.suppressWildcardsMode
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.getChildren
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * The members that the JVM backend generates for a Kotlin class or file, built from its FIR declarations: fields, accessors and
 * methods with their JVM names, flags and types.
 */
@OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
internal class AptKotlinMembers(private val env: AptEnv) {
    class Members(val fields: List<AptVariableElement>, val methods: List<AptExecutableElement>)

    private val session get() = env.session
    private val typeContext get() = env.typeMapper.typeContext

    private class Owner(
        val element: AptTypeElement,
        val klass: FirRegularClass?,
        val fields: MutableList<AptVariableElement> = mutableListOf(),
        val methods: MutableList<AptExecutableElement> = mutableListOf(),
    ) {
        val isFacade: Boolean get() = klass == null
        val isInterface: Boolean get() = klass?.classKind == ClassKind.INTERFACE || klass?.classKind == ClassKind.ANNOTATION_CLASS
        val isAnnotation: Boolean get() = klass?.classKind == ClassKind.ANNOTATION_CLASS
        val isObject: Boolean get() = klass?.classKind == ClassKind.OBJECT && klass.isCompanion.not()
        val isCompanion: Boolean get() = klass?.isCompanion == true
    }

    private class Parameter(val type: ConeKotlinType, val name: String?, val annotations: List<FirAnnotation>)

    fun facadeMembers(element: AptFacadeElement): Members {
        val owner = Owner(element, null)
        for (declaration in element.file.declarations) {
            when (declaration) {
                is FirNamedFunction -> function(declaration, owner)
                is FirProperty -> property(declaration, owner)
                else -> {}
            }
        }
        return Members(owner.fields, owner.methods)
    }

    fun classMembers(element: AptKotlinClassElement): Members {
        val klass = element.klass
        val owner = Owner(element, klass)
        if (klass.classKind == ClassKind.ENUM_CLASS) {
            for (entry in klass.declarations.filterIsInstance<FirEnumEntry>()) {
                owner.fields += AptVariableElement(
                    env, element, entry.name.asString(), ElementKind.ENUM_CONSTANT, setOf(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),
                    { element.rawType }, { declarationAnnotations(env, entry, entry.annotations, element.isSource) },
                )
            }
        }
        for (declaration in klass.declarations) {
            when (declaration) {
                is FirConstructor -> constructor(declaration, owner)
                is FirNamedFunction -> function(declaration, owner)
                is FirProperty -> property(declaration, owner)
                else -> {}
            }
        }
        if (klass.isData && !klass.isValue) dataClassMembers(owner)
        if (owner.isObject) {
            owner.fields.add(0, syntheticField(element, JvmAbi.INSTANCE_FIELD, setOf(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL), element))
        }
        val companion = klass.companionObjectSymbol?.fir
        if (companion != null) {
            val companionElement = env.typeElement(companion.symbol.classId)
            if (companionElement != null && !(owner.isInterface && companion.visibility == Visibilities.Private)) {
                val modifiers = mutableSetOf(Modifier.STATIC, Modifier.FINAL)
                if (owner.isInterface) modifiers += Modifier.PUBLIC else modifiers.addVisibility(companion.visibility)
                owner.fields += syntheticField(element, companion.symbol.classId.shortClassName.asString(), modifiers, companionElement)
            }
            if (companionElement != null) companionMembersInOuter(companion, Owner(companionElement, companion), owner)
        }
        return Members(owner.fields, owner.methods)
    }

    private fun syntheticField(owner: AptTypeElement, name: String, modifiers: Set<Modifier>, type: AptTypeElement): AptVariableElement =
        AptVariableElement(env, owner, name, ElementKind.FIELD, modifiers, { type.rawType }, { listOf(notNull()) })

    /** The fields of the companion's properties and the static proxies of its `@JvmStatic` members, which the outer class gets. */
    private fun companionMembersInOuter(companion: FirRegularClass, companionOwner: Owner, outer: Owner) {
        for (declaration in companion.declarations) {
            when (declaration) {
                is FirProperty -> {
                    if (declaration.isHidden() || !declaration.hasField(companionOwner)) continue
                    val isJvmField = declaration.hasJvmField()
                    if (!outer.isInterface || isJvmField) {
                        outer.fields += field(declaration, outer, isStatic = true)
                    } else if (declaration.isConst && declaration.visibility != Visibilities.Private && declaration.visibility != Visibilities.Protected) {
                        outer.fields += field(declaration, outer, isStatic = true)
                    }
                    if (declaration.hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic) && !isJvmField && !declaration.isConst) {
                        declaration.getter?.takeIf { it.isGenerated() }?.let { accessor(declaration, it, outer, isStatic = true, proxyOf = companionOwner) }
                        declaration.setter?.takeIf { declaration.isVar && it.isGenerated() }
                            ?.let { accessor(declaration, it, outer, isStatic = true, proxyOf = companionOwner) }
                    }
                }
                is FirNamedFunction -> if (declaration.hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic)) {
                    function(declaration, outer, staticProxyOf = companionOwner)
                }
                else -> {}
            }
        }
    }

    // ---------------------------------------------------------------- constructors

    private fun constructor(constructor: FirConstructor, owner: Owner) {
        val klass = owner.klass ?: return
        if (owner.isInterface || klass.isValue) return
        val modifiers = mutableSetOf<Modifier>()
        when {
            klass.classKind == ClassKind.ENUM_CLASS || klass.classKind == ClassKind.OBJECT || klass.modality == Modality.SEALED -> modifiers += Modifier.PRIVATE
            // Constructors taking value classes are hidden behind a synthetic one, javac sees only the private one.
            constructor.valueParameters.any { it.returnTypeRef.coneType.requiresMangling() } -> modifiers += Modifier.PRIVATE
            else -> modifiers.addVisibility(constructor.visibility)
        }
        val parameters = constructor.valueParameters.map { it.toParameter() }
        method(
            constructor, owner, "<init>", ElementKind.CONSTRUCTOR, modifiers, constructor.isVarargs(), emptyList(), parameters,
            returnType = null, annotations = constructor.annotations,
        )
        if (constructor.isPrimary && constructor.valueParameters.isNotEmpty() && constructor.valueParameters.all { it.defaultValue != null } &&
            klass.classKind == ClassKind.CLASS && !klass.isInner && !klass.isLocal && klass.modality != Modality.SEALED &&
            constructor.visibility != Visibilities.Private && !constructor.hasAnnotation(JVM_OVERLOADS) &&
            klass.declarations.none { it is FirConstructor && it.valueParameters.isEmpty() }
        ) {
            method(
                constructor, owner, "<init>", ElementKind.CONSTRUCTOR, mutableSetOf<Modifier>().apply { addVisibility(constructor.visibility) },
                false, emptyList(), emptyList(), returnType = null, annotations = constructor.annotations,
            )
        }
        if (constructor.hasAnnotation(JVM_OVERLOADS)) {
            for (kept in overloads(constructor)) {
                method(
                    constructor, owner, "<init>", ElementKind.CONSTRUCTOR, modifiers, kept.lastOrNull()?.isVararg == true, emptyList(),
                    kept.map { it.toParameter() }, returnType = null, annotations = constructor.annotations,
                )
            }
        }
    }

    /** The parameter lists of the `@JvmOverloads` wrappers: without the last parameters with default values, one at a time. */
    private fun overloads(function: FirFunction): List<List<FirValueParameter>> {
        val valueParameters = function.valueParameters
        val defaults = valueParameters.count { it.defaultValue != null }
        return (1..defaults).map { skip ->
            var toSkip = skip
            valueParameters.asReversed().filter { parameter ->
                if (toSkip > 0 && parameter.defaultValue != null) {
                    toSkip--
                    false
                } else true
            }.asReversed()
        }
    }

    // ---------------------------------------------------------------- functions

    private fun function(function: FirNamedFunction, owner: Owner, staticProxyOf: Owner? = null) {
        if (function.isLocal || function.isHidden()) return
        if (function.valueParameters.any { it.returnTypeRef.coneType.requiresMangling() } ||
            function.receiverParameter?.typeRef?.coneType?.requiresMangling() == true ||
            (!owner.isFacade && function.returnTypeRef.coneType.isValueClassType()) || owner.klass?.isValue == true
        ) return
        val source = staticProxyOf ?: owner
        val name = function.jvmNameFromAnnotation() ?: mangleIfInternal(function.name.asString(), function, source)
        val isStatic = staticProxyOf != null || owner.isFacade || (function.hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic) && owner.isObject) ||
                function.isEnumGenerated()
        val modifiers = if (staticProxyOf != null) staticProxyModifiers(methodModifiers(function, source, isStatic = false), owner)
        else methodModifiers(function, source, isStatic)
        val parameters = receiverParameters(function, function.hasBody(source)) + function.valueParameters.map { it.toParameter() } +
                listOfNotNull(continuationParameter(function))
        val returnType = if (function.isSuspend) session.builtinTypes.nullableAnyType.coneType else function.returnTypeRef.coneType
        method(
            function, owner, name, ElementKind.METHOD, modifiers, function.isVarargs(), function.typeParameters.filterIsInstance<FirTypeParameter>(),
            parameters, returnType.takeUnless { function.returnsVoid() }, annotations = function.annotations,
            nullability = !function.isEnumGenerated(),
        )
        if (function.hasAnnotation(JVM_OVERLOADS)) {
            for (kept in overloads(function)) {
                val wrapperModifiers = modifiers.toMutableSet().apply {
                    remove(Modifier.ABSTRACT)
                    if (!isStatic && !owner.isInterface) add(Modifier.FINAL)
                }
                method(
                    function, owner, name, ElementKind.METHOD, wrapperModifiers, kept.lastOrNull()?.isVararg == true,
                    function.typeParameters.filterIsInstance<FirTypeParameter>(),
                    receiverParameters(function, true) + kept.map { it.toParameter() } + listOfNotNull(continuationParameter(function)),
                    returnType.takeUnless { function.returnsVoid() }, annotations = function.annotations,
                )
            }
        }
    }

    private fun FirValueParameter.toParameter() = Parameter(returnTypeRef.coneType, name.asString(), annotations)

    /** Only receivers of functions with bodies are named after the function, abstract ones get a generated name. */
    private fun receiverParameters(callable: FirCallableDeclaration, hasBody: Boolean): List<Parameter> = buildList {
        for (parameter in callable.contextParameters) add(Parameter(parameter.returnTypeRef.coneType, parameter.name.asString(), parameter.annotations))
        callable.receiverParameter?.let { receiver ->
            val name = if (hasBody) callable.symbol.callableId?.callableName?.let { "\$this\$" + it.asString() } else null
            add(Parameter(receiver.typeRef.coneType, name, receiver.annotations))
        }
    }

    private fun continuationParameter(function: FirNamedFunction): Parameter? {
        if (!function.isSuspend) return null
        val continuationType = StandardClassIds.Continuation.constructClassLikeType(arrayOf(function.returnTypeRef.coneType))
        return Parameter(continuationType, "\$completion", emptyList())
    }

    private fun FirNamedFunction.returnsVoid(): Boolean =
        !isSuspend && returnTypeRef.coneType.fullyExpandedType(session).let { it.isUnit && !it.isMarkedNullable }

    /** `equals`, `hashCode` and `toString` that FIR synthesizes for a data class. */
    private fun dataClassMembers(owner: Owner) {
        val klass = owner.klass ?: return
        val scope = klass.unsubstitutedScope(session, env.scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        for (name in listOf(OperatorNameConventions.TO_STRING, OperatorNameConventions.HASH_CODE, OperatorNameConventions.EQUALS)) {
            scope.processFunctionsByName(name) { symbol ->
                val function = symbol.fir
                if (function.origin !is FirDeclarationOrigin.Synthetic || symbol.containingClassLookupTag() != klass.symbol.toLookupTag()) {
                    return@processFunctionsByName
                }
                method(
                    function, owner, name.asString(), ElementKind.METHOD, setOf(Modifier.PUBLIC), false, emptyList(),
                    function.valueParameters.map { it.toParameter() }, function.returnTypeRef.coneType, annotations = emptyList(),
                )
            }
        }
    }

    // ---------------------------------------------------------------- properties

    private fun FirProperty.hasField(owner: Owner): Boolean =
        !owner.isAnnotation && (delegate != null || (hasBackingField && !isExpect))

    private fun property(property: FirProperty, owner: Owner) {
        if (property.isLocal || property.isHidden()) return
        if (property.returnTypeRef.coneType.isValueClassType() || owner.klass?.isValue == true) return
        val isJvmField = property.hasJvmField()
        if (property.hasField(owner)) {
            val movesOut = owner.isCompanion && (owner.klass?.let { outerKindOf(it) } != ClassKind.INTERFACE || isJvmField)
            if (!movesOut) owner.fields += field(property, owner, isStatic = owner.isFacade || owner.isCompanion || owner.isObject)
        }
        if (isJvmField || property.isConst) return
        val isJvmStatic = property.hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic)
        val isStatic = owner.isFacade || (isJvmStatic && owner.isObject) || property.isEnumGenerated()
        property.getter?.takeIf { it.isGenerated() }?.let { accessor(property, it, owner, isStatic) }
        property.setter?.takeIf { property.isVar && it.isGenerated() }?.let { accessor(property, it, owner, isStatic) }
    }

    private fun outerKindOf(companion: FirRegularClass): ClassKind? =
        companion.symbol.classId.outerClassId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol }?.classKind

    /** A default accessor that is private is not generated. */
    private fun FirPropertyAccessor.isGenerated(): Boolean =
        !(isDefaultAccessor() && visibility == Visibilities.Private) && !hasAnnotation(JVM_SYNTHETIC)

    private fun FirPropertyAccessor.isDefaultAccessor(): Boolean =
        source?.kind is KtFakeSourceElementKind.DefaultAccessor || body == null && !hasBodyInSource()

    private fun FirProperty.hasJvmField(): Boolean =
        hasAnnotation(JvmStandardClassIds.Annotations.JvmField) || backingField?.hasAnnotation(JvmStandardClassIds.Annotations.JvmField) == true

    private fun field(property: FirProperty, owner: Owner, isStatic: Boolean): AptVariableElement {
        val isDelegate = property.delegate != null
        val name = if (isDelegate) property.name.asString() + JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX else property.name.asString()
        val type = if (isDelegate) property.delegate!!.resolvedType else property.returnTypeRef.coneType
        val visibility = when {
            property.isConst || property.hasJvmField() -> property.visibility
            property.isLateInit -> property.setter?.visibility ?: property.visibility
            else -> Visibilities.Private
        }
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addVisibility(visibility)
        if (isStatic) modifiers += Modifier.STATIC
        if (!property.isVar || isDelegate) modifiers += Modifier.FINAL
        if (property.hasAnnotation(JvmStandardClassIds.Annotations.JvmVolatile) ||
            property.backingField?.hasAnnotation(JvmStandardClassIds.Annotations.JvmVolatile) == true
        ) modifiers += Modifier.VOLATILE
        if (property.hasAnnotation(TRANSIENT) || property.backingField?.hasAnnotation(TRANSIENT) == true) modifiers += Modifier.TRANSIENT
        val mode = if (property.isVar && !isDelegate) parameterMode(property, type) else returnMode(property, type)
        val element = owner.element
        val mappedType: TypeMirror by lazy { env.typeOf(type, mode, element::resolveTypeVariable) }
        return AptVariableElement(
            env, element, name, ElementKind.FIELD, modifiers, { mappedType },
            {
                val annotations = declarationAnnotations(env, property, property.backingField?.annotations.orEmpty(), element.isSource)
                if (property.isLateInit) annotations else annotations.plusNullability(nullability(type, mappedType))
            },
            constantValue = if (property.isConst) constantValue(property) else null,
        )
    }

    private fun constantValue(property: FirProperty): Any? {
        val result = FirExpressionEvaluator.evaluatePropertyInitializer(property, session)
        return result?.resultOrNull<FirLiteralExpression>()?.toAsmConstant()
    }

    private fun accessor(property: FirProperty, accessor: FirPropertyAccessor, owner: Owner, isStatic: Boolean, proxyOf: Owner? = null) {
        val source = proxyOf ?: owner
        val name = accessorJvmName(property, accessor, source)
        val modifiers = if (proxyOf != null) staticProxyModifiers(methodModifiers(accessor, source, false, modalitySource = property), owner)
        else methodModifiers(accessor, source, isStatic, modalitySource = property)
        val parameters = receiverParameters(property, !property.isAbstract).toMutableList()
        if (accessor.isSetter) {
            val parameter = accessor.valueParameters.single()
            parameters += Parameter(
                property.returnTypeRef.coneType, parameter.name.asString().takeUnless { accessor.isDefaultAccessor() }, parameter.annotations,
            )
        }
        val defaultValue = if (source.isAnnotation) {
            source.klass?.primaryConstructorIfAny(session)?.fir?.valueParameters?.firstOrNull { it.name == property.name }?.defaultValue
        } else null
        method(
            accessor, owner, name, ElementKind.METHOD, modifiers, false, property.typeParameters.filterIsInstance<FirTypeParameter>(),
            parameters, property.returnTypeRef.coneType.takeIf { accessor.isGetter }, annotations = accessor.annotations,
            modeSource = property, defaultValue = defaultValue, isAnnotationMethod = source.isAnnotation && !isStatic,
        )
    }

    private fun accessorJvmName(property: FirProperty, accessor: FirPropertyAccessor, owner: Owner): String {
        accessor.jvmNameFromAnnotation()?.let { return it }
        val propertyName = property.name.asString()
        if (owner.isAnnotation) return propertyName
        val name = if (accessor.isGetter) JvmAbi.getterName(propertyName) else JvmAbi.setterName(propertyName)
        return if (property.hasAnnotation(StandardClassIds.Annotations.PublishedApi)) name else mangleIfInternal(name, accessor, owner)
    }

    // ---------------------------------------------------------------- methods

    private fun method(
        declaration: FirCallableDeclaration,
        owner: Owner,
        name: String,
        kind: ElementKind,
        modifiers: Set<Modifier>,
        isVarArgs: Boolean,
        typeParameters: List<FirTypeParameter>,
        parameters: List<Parameter>,
        returnType: ConeKotlinType?,
        annotations: List<FirAnnotation>,
        modeSource: FirCallableDeclaration = declaration,
        nullability: Boolean = true,
        defaultValue: FirExpression? = null,
        isAnnotationMethod: Boolean = false,
    ) {
        if (owner.element.isSource && !(kind == ElementKind.CONSTRUCTOR || SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name))) return
        val element = owner.element
        val withNullability = nullability && Modifier.PRIVATE !in modifiers && !isAnnotationMethod
        lateinit var method: AptExecutableElement
        method = AptExecutableElement(env, element, name, kind, modifiers, isVarArgs) {
            val mirrors = declarationAnnotations(env, declaration, annotations, element.isSource)
            if (withNullability && returnType != null) mirrors.plusNullability(nullability(returnType, method.returnType)) else mirrors
        }
        method.typeParameterElements = kotlinTypeParameters(env, method, typeParameters, method::resolveTypeVariable)
        method.parameterElements = parameters.mapIndexed { index, parameter ->
            val name = parameter.name?.takeUnless { it.startsWith("<") && it.endsWith(">") } ?: "p$index"
            val mappedType: TypeMirror by lazy { env.typeOf(parameter.type, parameterMode(modeSource, parameter.type), method::resolveTypeVariable) }
            AptParameterElement(env, method, name, { mappedType }) {
                val mirrors = declarationAnnotations(env, null, parameter.annotations, element.isSource)
                if (withNullability) mirrors.plusNullability(nullability(parameter.type, mappedType)) else mirrors
            }
        }
        if (returnType != null) {
            method.returnTypeProvider = { env.typeOf(returnType, returnMode(modeSource, returnType, isAnnotationMethod), method::resolveTypeVariable) }
        }
        method.thrownTypesProvider = { thrownTypes(declaration) }
        if (defaultValue != null) {
            method.defaultValueProvider = { annotationValue(env, defaultValue, method.returnType) as AnnotationValue? }
        }
        owner.methods += method
    }

    private fun thrownTypes(declaration: FirCallableDeclaration): List<TypeMirror> {
        val throws = declaration.annotations.firstOrNull {
            val classId = it.classIdOrNull(env)
            classId == JvmStandardClassIds.Annotations.Throws || classId == JvmStandardClassIds.Annotations.ThrowsAlias
        } ?: return emptyList()
        val argument = throws.argumentMapping.mapping.values.firstOrNull() ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val values = annotationValue(env, argument, AptArrayType(env.objectType))?.value as? List<AptAnnotationValue> ?: return emptyList()
        return values.mapNotNull { it.value as? TypeMirror }
    }

    /** The modifiers of a static proxy in [outer] of a companion member with [modifiers]. */
    private fun staticProxyModifiers(modifiers: Set<Modifier>, outer: Owner): Set<Modifier> = modifiers.toMutableSet().apply {
        remove(Modifier.ABSTRACT)
        remove(Modifier.DEFAULT)
        if (outer.isInterface) remove(Modifier.FINAL)
        add(Modifier.STATIC)
    }

    private fun methodModifiers(
        function: FirCallableDeclaration, owner: Owner, isStatic: Boolean, modalitySource: FirCallableDeclaration = function,
    ): Set<Modifier> {
        val result = mutableSetOf<Modifier>()
        result.addVisibility(function.visibility)
        val hasBody = if (owner.element.isSource) function.hasBody(owner) else modalitySource.modality != Modality.ABSTRACT
        val modality = modalitySource.modality ?: Modality.FINAL
        when {
            owner.isAnnotation -> if (!isStatic) result += Modifier.ABSTRACT
            function.isEnumGenerated() || modalitySource.isEnumGenerated() -> {}
            isStatic -> if (owner.isFacade) result += Modifier.FINAL else when (modality) {
                Modality.FINAL -> result += Modifier.FINAL
                Modality.ABSTRACT, Modality.SEALED -> result += Modifier.ABSTRACT
                Modality.OPEN -> {}
            }
            else -> when (modality) {
                Modality.FINAL -> if (!(owner.isInterface && hasBody)) result += Modifier.FINAL
                Modality.ABSTRACT -> result += Modifier.ABSTRACT
                else -> if (owner.isInterface && !hasBody) result += Modifier.ABSTRACT
            }
        }
        if (isStatic) result += Modifier.STATIC
        if (owner.klass?.classKind == ClassKind.INTERFACE && !isStatic && Modifier.ABSTRACT !in result && Modifier.PRIVATE !in result) {
            result += Modifier.DEFAULT
        }
        if (function is FirFunction && function.isExternal) result += Modifier.NATIVE
        if (function.hasAnnotation(STRICTFP)) result += Modifier.STRICTFP
        if (function.hasAnnotation(SYNCHRONIZED)) result += Modifier.SYNCHRONIZED
        return result
    }

    /** Without bodies in header mode, a body is found in the source. Library members have one unless they are abstract. */
    private fun FirCallableDeclaration.hasBody(owner: Owner): Boolean {
        if (!owner.element.isSource) return modality != Modality.ABSTRACT
        return this is FirFunction && (body != null || hasBodyInSource())
    }

    private fun FirFunction.hasBodyInSource(): Boolean =
        source?.let { source ->
            source.lighterASTNode.getChildren(source.treeStructure).any { it.tokenType == KtNodeTypes.BLOCK || it.tokenType == KtTokens.EQ }
        } == true

    private fun FirFunction.isVarargs(): Boolean = valueParameters.lastOrNull()?.isVararg == true

    private fun FirDeclaration.isEnumGenerated(): Boolean = source?.kind is KtFakeSourceElementKind.EnumGeneratedDeclaration

    /** Synthetic in class files, and so not in stubs. */
    private fun FirCallableDeclaration.isHidden(): Boolean =
        hasAnnotation(JVM_SYNTHETIC) || hasAnnotation(KAPT_IGNORED) || isDeprecatedHidden() ||
                this is FirNamedFunction && isInline && typeParameters.any { it.symbol.isReified }

    private fun FirCallableDeclaration.isDeprecatedHidden(): Boolean {
        val deprecated = annotations.firstOrNull { it.classIdOrNull(env) == StandardClassIds.Annotations.Deprecated } ?: return false
        val level = deprecated.argumentMapping.mapping[StandardClassIds.Annotations.ParameterNames.deprecatedLevel] ?: return false
        return (level as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedEnumEntrySymbol()?.name?.asString() ==
                DeprecationLevel.HIDDEN.name
    }

    private fun mangleIfInternal(name: String, declaration: FirCallableDeclaration, owner: Owner): String {
        if (owner.isFacade) return name
        if (declaration.visibility != Visibilities.Internal || declaration.hasAnnotation(StandardClassIds.Annotations.PublishedApi)) return name
        return name + "$" + NameUtils.sanitizeAsJavaIdentifier(env.moduleName)
    }

    private fun FirDeclaration.jvmNameFromAnnotation(): String? =
        annotations.firstOrNull { it.classIdOrNull(env) == JvmStandardClassIds.Annotations.JvmName }
            ?.let { (it.argumentMapping.mapping.values.firstOrNull() as? FirLiteralExpression)?.value as? String }

    private fun FirDeclaration.hasAnnotation(classId: ClassId): Boolean = annotations.any { it.classIdOrNull(env) == classId }

    private fun ConeKotlinType.isValueClassType(): Boolean {
        val symbol = fullyExpandedType(session).lowerBoundIfFlexible().toRegularClassSymbol(session) ?: return false
        return symbol.isValue && symbol.classId != StandardClassIds.Result
    }

    /** Value classes in a signature mangle the name, which makes it invalid in Java. */
    private fun ConeKotlinType.requiresMangling(): Boolean {
        var type = fullyExpandedType(session).lowerBoundIfFlexible()
        while (type is ConeTypeParameterType) {
            type = (type.lookupTag as ConeTypeParameterLookupTagImpl).typeParameterSymbol.resolvedBounds.firstOrNull()?.coneType?.fullyExpandedType(session)?.lowerBoundIfFlexible()
                ?: return false
        }
        return type.isValueClassType()
    }

    // ---------------------------------------------------------------- types

    private fun nullability(type: ConeKotlinType, mapped: TypeMirror): AptAnnotationMirror? {
        if (mapped.kind.isPrimitive || mapped.kind == TypeKind.VOID) return null
        val expanded = type.fullyExpandedType(session)
        if (expanded is ConeFlexibleType && expanded.lowerBound.isMarkedNullable != expanded.upperBound.isMarkedNullable) return null
        val nullable = expanded.isNullableInIr()
        if (expanded is ConeTypeParameterType && !expanded.isMarkedNullable && nullable) return null
        return if (nullable) AptAnnotationMirror.marker(env, NULLABLE) else notNull()
    }

    private fun notNull() = AptAnnotationMirror.marker(env, NOT_NULL)

    /** `IrType.isNullable()`: a type parameter is nullable if any of its bounds is. */
    private fun ConeKotlinType.isNullableInIr(): Boolean = when (val type = fullyExpandedType(session)) {
        is ConeFlexibleType -> type.upperBound.isNullableInIr()
        is ConeDefinitelyNotNullType -> false
        is ConeTypeParameterType -> type.isMarkedNullable || (type.lookupTag as ConeTypeParameterLookupTagImpl).typeParameterSymbol.resolvedBounds.any { it.coneType.isNullableInIr() }
        else -> type.isMarkedNullable
    }

    /** The backend does not add a nullability annotation that the declaration already has. */
    private fun List<AptAnnotationMirror>.plusNullability(annotation: AptAnnotationMirror?): List<AptAnnotationMirror> =
        if (annotation == null || any { it.annotationElement.internalName == annotation.annotationElement.internalName }) this else this + annotation

    @OptIn(org.jetbrains.kotlin.name.ClassIdBasedLocality::class)
    private fun ConeKotlinType.forMappingMode(): ConeKotlinType =
        if ((fullyExpandedType(session) as? ConeClassLikeType)?.lookupTag?.classId?.isLocal == true) forJvmSignature(session) else this

    private fun returnMode(declaration: FirCallableDeclaration, type: ConeKotlinType, isAnnotationMethod: Boolean = false): TypeMappingMode =
        with(typeContext) {
            val mappedType = type.forMappingMode()
            modeFromAnnotations(suppressWildcardsMode(declaration), mappedType, isAnnotationMethod)
                ?: getOptimalModeForReturnType(mappedType, isAnnotationMethod)
        }

    private fun parameterMode(declaration: FirCallableDeclaration, type: ConeKotlinType): TypeMappingMode = with(typeContext) {
        val mappedType = type.forMappingMode()
        modeFromAnnotations(suppressWildcardsMode(declaration), mappedType, isForAnnotationParameter = false)
            ?: if (mappedType.typeArguments.isNotEmpty() && declaration is FirNamedFunction &&
                declaration.symbol.callableId.asSingleFqName().isMethodWithDeclarationSiteWildcardsFqName
            ) TypeMappingMode.GENERIC_ARGUMENT
            else getOptimalModeForValueParameter(mappedType)
    }

    /** Same as `extractTypeMappingModeFromAnnotation` of the JVM backend: `@JvmSuppressWildcards` and `@JvmWildcard` decide the mode. */
    private fun TypeSystemCommonBackendContext.modeFromAnnotations(
        callableSuppressWildcardsMode: Boolean?, outerType: ConeKotlinType, isForAnnotationParameter: Boolean,
    ): TypeMappingMode? {
        val suppressWildcards = outerType.suppressWildcardsMode(this) ?: callableSuppressWildcardsMode ?: return null
        if (outerType.argumentsCount() == 0) return TypeMappingMode.DEFAULT
        return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
            skipDeclarationSiteWildcards = suppressWildcards,
            isForAnnotationParameter = isForAnnotationParameter,
            needInlineClassWrapping = !outerType.typeConstructor().isInlineClass(),
            mapTypeAliases = false,
        )
    }

    private fun suppressWildcardsMode(declaration: FirCallableDeclaration): Boolean? {
        suppressWildcards(declaration.annotations)?.let { return it }
        if (declaration is FirPropertyAccessor) suppressWildcards(declaration.propertySymbol.fir.annotations)?.let { return it }
        var classId = declaration.containingClassLookupTag()?.classId
        while (classId != null) {
            val klass = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: break
            suppressWildcards(klass.resolvedAnnotationsWithArguments)?.let { return it }
            classId = classId.outerClassId
        }
        return null
    }

    private fun suppressWildcards(annotations: List<FirAnnotation>): Boolean? {
        val annotation = annotations.firstOrNull { it.classIdOrNull(env) == JvmStandardClassIds.Annotations.JvmSuppressWildcards } ?: return null
        val argument = annotation.argumentMapping.mapping.values.firstOrNull() ?: return true
        return ((argument as? FirLiteralExpression)?.value as? Boolean) ?: true
    }

    private companion object {
        const val NOT_NULL = "org/jetbrains/annotations/NotNull"
        const val NULLABLE = "org/jetbrains/annotations/Nullable"

        val JVM_OVERLOADS = ClassId.fromString("kotlin/jvm/JvmOverloads")
        val JVM_SYNTHETIC = ClassId.fromString("kotlin/jvm/JvmSynthetic")
        val STRICTFP = ClassId.fromString("kotlin/jvm/Strictfp")
        val SYNCHRONIZED = ClassId.fromString("kotlin/jvm/Synchronized")
        val TRANSIENT = ClassId.fromString("kotlin/jvm/Transient")
        val KAPT_IGNORED = ClassId.fromString("kotlin/jvm/internal/KaptIgnored")
    }
}

/** The members of a Java class as FIR declares them, with the Java types its declarations keep. */
@OptIn(DirectDeclarationsAccess::class)
internal class AptJavaMembers(private val env: AptEnv) {
    fun members(owner: AptJavaClassElement): Pair<List<AptVariableElement>, List<AptExecutableElement>> {
        val klass = owner.klass
        val fields = mutableListOf<AptVariableElement>()
        val methods = mutableListOf<AptExecutableElement>()
        val isInterface = klass.classKind == ClassKind.INTERFACE || klass.classKind == ClassKind.ANNOTATION_CLASS
        val annotationDefaults: Map<String, FirExpression> by lazy {
            if (klass.classKind != ClassKind.ANNOTATION_CLASS) return@lazy emptyMap()
            klass.declarations.filterIsInstance<FirJavaConstructor>().flatMap { it.valueParameters }
                .mapNotNull { parameter -> parameter.defaultValue?.let { parameter.name.asString() to it } }.toMap()
        }
        for (declaration in klass.declarations) {
            when (declaration) {
                is FirEnumEntry -> fields += AptVariableElement(
                    env, owner, declaration.name.asString(), ElementKind.ENUM_CONSTANT, setOf(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),
                    { owner.rawType }, { declaration.annotations.mapNotNull { AptAnnotationMirror.of(env, it) } },
                )
                is FirJavaField -> {
                    val modifiers = mutableSetOf<Modifier>()
                    modifiers.addVisibility(declaration.visibility)
                    if (declaration.isStatic) modifiers += Modifier.STATIC
                    if (!declaration.isVar) modifiers += Modifier.FINAL
                    val type: TypeMirror by lazy { javaType(declaration.returnTypeRef, owner::resolveTypeVariable) }
                    val constant = if (declaration.isStatic && !declaration.isVar) {
                        (declaration.initializer as? FirLiteralExpression)?.toAsmConstant()
                    } else null
                    fields += AptVariableElement(
                        env, owner, declaration.name.asString(), ElementKind.FIELD, modifiers, { type },
                        { declaration.annotations.mapNotNull { AptAnnotationMirror.of(env, it) } },
                        constantValue = constant?.let { coerceConstant(it, type.kind) },
                    )
                }
                is FirJavaConstructor -> {
                    if (isInterface) continue
                    val modifiers = mutableSetOf<Modifier>().apply { addVisibility(declaration.visibility) }
                    methods += executable(owner, declaration, "<init>", ElementKind.CONSTRUCTOR, modifiers, null, emptyList())
                }
                is FirJavaMethod -> {
                    val modifiers = mutableSetOf<Modifier>()
                    modifiers.addVisibility(declaration.visibility)
                    if (declaration.isStatic) modifiers += Modifier.STATIC
                    when (declaration.modality) {
                        Modality.ABSTRACT -> modifiers += Modifier.ABSTRACT
                        Modality.FINAL -> if (!isInterface) modifiers += Modifier.FINAL
                        else -> {}
                    }
                    if (isInterface && !declaration.isStatic && Modifier.ABSTRACT !in modifiers && Modifier.PRIVATE !in modifiers) {
                        modifiers += Modifier.DEFAULT
                    }
                    if (declaration.isExternal) modifiers += Modifier.NATIVE
                    methods += executable(
                        owner, declaration, declaration.name.asString(), ElementKind.METHOD, modifiers, declaration.returnTypeRef,
                        declaration.typeParameters.filterIsInstance<FirJavaTypeParameter>(),
                        annotationDefaults[declaration.name.asString()],
                    )
                }
                else -> {}
            }
        }
        return fields to methods
    }

    private fun executable(
        owner: AptJavaClassElement, function: FirFunction, name: String, kind: ElementKind, modifiers: Set<Modifier>,
        returnTypeRef: FirTypeRef?, typeParameters: List<FirJavaTypeParameter>, defaultValue: FirExpression? = null,
    ): AptExecutableElement {
        val method = AptExecutableElement(env, owner, name, kind, modifiers, function.valueParameters.lastOrNull()?.isVararg == true) {
            function.annotations.mapNotNull { AptAnnotationMirror.of(env, it) }
        }
        method.typeParameterElements = typeParameters.map { parameter ->
            AptTypeParameterElement(env, method, parameter.name.asString()) {
                parameter.javaTypeParameter.upperBounds.map { env.typeOf(it, method::resolveTypeVariable) }
            }
        }
        method.parameterElements = function.valueParameters.mapIndexed { index, parameter ->
            val name = parameter.name.takeUnless { it.isSpecial }?.asString() ?: "arg$index"
            AptParameterElement(env, method, name, { javaType(parameter.returnTypeRef, method::resolveTypeVariable) }) {
                parameter.annotations.mapNotNull { AptAnnotationMirror.of(env, it) }
            }
        }
        if (returnTypeRef != null) method.returnTypeProvider = { javaType(returnTypeRef, method::resolveTypeVariable) }
        if (defaultValue != null) method.defaultValueProvider = { annotationValue(env, defaultValue, method.returnType) }
        return method
    }

    private fun javaType(typeRef: FirTypeRef, resolve: (String) -> TypeMirror): TypeMirror = when (typeRef) {
        is FirJavaTypeRef -> env.typeOf(typeRef.type, resolve)
        else -> typeRef.coneTypeOrNull?.let { env.typeOf(it, TypeMappingMode.DEFAULT, resolve) } ?: env.objectType
    }

    private fun coerceConstant(value: Any, kind: TypeKind): Any = when (kind) {
        TypeKind.BOOLEAN -> if (value is Number) value.toInt() != 0 else value
        TypeKind.CHAR -> if (value is Number) value.toInt().toChar() else value
        TypeKind.BYTE -> (value as? Number)?.toByte() ?: value
        TypeKind.SHORT -> (value as? Number)?.toShort() ?: value
        else -> value
    }
}
