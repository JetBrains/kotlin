/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.lombok.k2.*
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Builder
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Singular
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.k2.java.*
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.lombok.utils.capitalize
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class BuilderGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private const val TO_BUILDER = "toBuilder"
    }

    private val lombokService: LombokService
        get() = session.lombokService

    private val builderClassCache: FirCache<FirClassSymbol<*>, FirJavaClass?, Nothing?> =
        session.firCachesFactory.createCache(::createBuilderClass)

    private val functionsCache: FirCache<FirClassSymbol<*>, Map<Name, List<FirJavaMethod>>?, Nothing?> =
        session.firCachesFactory.createCache(::createFunctions)

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        return functionsCache.getValue(classSymbol)?.keys.orEmpty()
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        val name = builderClassCache.getValue(classSymbol)?.name ?: return emptySet()
        return setOf(name)
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        return functionsCache.getValue(classSymbol)?.get(callableId.callableName).orEmpty().map { it.symbol }
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        if (!owner.isSuitableJavaClass()) return null
        return builderClassCache.getValue(owner)?.symbol
    }

    private fun createFunctions(classSymbol: FirClassSymbol<*>): Map<Name, List<FirJavaMethod>>? {
        val builder = lombokService.getBuilder(classSymbol) ?: return null
        val functions = mutableListOf<FirJavaMethod>()
        val classId = classSymbol.classId
        val builderClassName = builder.builderClassName.replace("*", classId.shortClassName.asString())
        val builderClassId = classId.createNestedClassId(Name.identifier(builderClassName))

        val builderType = builderClassId.constructClassLikeType(emptyArray(), isNullable = false)
        val visibility = builder.visibility.toVisibility()
        functions += classSymbol.createJavaMethod(
            Name.identifier(builder.builderMethodName),
            valueParameters = emptyList(),
            returnTypeRef = builderType.toFirResolvedTypeRef(),
            visibility = visibility,
            modality = Modality.FINAL,
            dispatchReceiverType = null,
            isStatic = true
        )

        if (builder.requiresToBuilder) {
            functions += classSymbol.createJavaMethod(
                Name.identifier(TO_BUILDER),
                valueParameters = emptyList(),
                returnTypeRef = builderType.toFirResolvedTypeRef(),
                visibility = visibility,
                modality = Modality.FINAL,
            )
        }

        return functions.groupBy { it.name }
    }

    @OptIn(SymbolInternals::class)
    private fun createBuilderClass(classSymbol: FirClassSymbol<*>): FirJavaClass? {
        val javaClass = classSymbol.fir as? FirJavaClass ?: return null
        val builder = lombokService.getBuilder(classSymbol) ?: return null
        val builderName = Name.identifier(builder.builderClassName.replace("*", classSymbol.name.asString()))
        val visibility = builder.visibility.toVisibility()
        val builderClass = classSymbol.createJavaClass(
            session,
            builderName,
            visibility,
            Modality.FINAL,
            isStatic = true,
            superTypeRefs = listOf(session.builtinTypes.anyType)
        )?.apply {
            declarations += symbol.createDefaultJavaConstructor(visibility)
            declarations += symbol.createJavaMethod(
                Name.identifier(builder.buildMethodName),
                valueParameters = emptyList(),
                returnTypeRef = classSymbol.defaultType().toFirResolvedTypeRef(),
                visibility = visibility,
                modality = Modality.FINAL
            )
            val fields = javaClass.declarations.filterIsInstance<FirJavaField>()
            for (field in fields) {
                when (val singular = lombokService.getSingular(field.symbol)) {
                    null -> createSetterMethod(builder, field, symbol, declarations)
                    else -> createMethodsForSingularFields(builder, singular, field, symbol, declarations)
                }
            }

        } ?: return null


        return builderClass
    }

    private fun createSetterMethod(
        builder: Builder,
        field: FirJavaField,
        builderClassSymbol: FirRegularClassSymbol,
        destination: MutableList<FirDeclaration>
    ) {
        val fieldName = field.name
        val setterName = fieldName.toMethodName(builder)
        destination += builderClassSymbol.createJavaMethod(
            name = setterName,
            valueParameters = listOf(ConeLombokValueParameter(fieldName, field.returnTypeRef)),
            returnTypeRef = builderClassSymbol.defaultType().toFirResolvedTypeRef(),
            modality = Modality.FINAL,
            visibility = builder.visibility.toVisibility()
        )
    }

    private fun createMethodsForSingularFields(
        builder: Builder,
        singular: Singular,
        field: FirJavaField,
        builderClassSymbol: FirRegularClassSymbol,
        destination: MutableList<FirDeclaration>
    ) {
        val fieldJavaTypeRef = field.returnTypeRef as? FirJavaTypeRef ?: return
        val javaClassifierType = fieldJavaTypeRef.type as? JavaClassifierType ?: return
        val typeName = (javaClassifierType.classifier as? JavaClass)?.fqName?.asString() ?: return

        val nameInSingularForm = (singular.singularName ?: field.name.identifier.singularForm)?.let(Name::identifier) ?: return

        val addMultipleParameterType: FirTypeRef
        val valueParameters: List<ConeLombokValueParameter>

        val fallbackParameterType = DummyJavaClassType.ObjectType.takeIf { javaClassifierType.isRaw }

        when (typeName) {
            in LombokNames.SUPPORTED_COLLECTIONS -> {
                val parameterType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                valueParameters = listOf(
                    ConeLombokValueParameter(nameInSingularForm, parameterType.toRef())
                )

                val baseType = when (typeName) {
                    in LombokNames.SUPPORTED_GUAVA_COLLECTIONS -> JavaClasses.Iterable
                    else -> JavaClasses.Collection
                }

                addMultipleParameterType = DummyJavaClassType(baseType, typeArguments = listOf(parameterType))
                    .withProperNullability(singular.allowNull)
                    .toRef()
            }

            in LombokNames.SUPPORTED_MAPS -> {
                val keyType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                val valueType = javaClassifierType.parameterType(1) ?: fallbackParameterType ?: return
                valueParameters = listOf(
                    ConeLombokValueParameter(Name.identifier("key"), keyType.toRef()),
                    ConeLombokValueParameter(Name.identifier("value"), valueType.toRef()),
                )

                addMultipleParameterType = DummyJavaClassType(JavaClasses.Map, typeArguments = listOf(keyType, valueType))
                    .withProperNullability(singular.allowNull)
                    .toRef()
            }

            in LombokNames.SUPPORTED_TABLES -> {
                val rowKeyType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                val columnKeyType = javaClassifierType.parameterType(1) ?: fallbackParameterType ?: return
                val valueType = javaClassifierType.parameterType(2) ?: fallbackParameterType ?: return

                valueParameters = listOf(
                    ConeLombokValueParameter(Name.identifier("rowKey"), rowKeyType.toRef()),
                    ConeLombokValueParameter(Name.identifier("columnKey"), columnKeyType.toRef()),
                    ConeLombokValueParameter(Name.identifier("value"), valueType.toRef()),
                )

                addMultipleParameterType = DummyJavaClassType(
                    JavaClasses.Table,
                    typeArguments = listOf(rowKeyType, columnKeyType, valueType)
                ).withProperNullability(singular.allowNull).toRef()
            }

            else -> return
        }

        val builderType = builderClassSymbol.defaultType().toFirResolvedTypeRef()
        val visibility = builder.visibility.toVisibility()

        destination += builderClassSymbol.createJavaMethod(
            name = nameInSingularForm.toMethodName(builder),
            valueParameters,
            returnTypeRef = builderType,
            modality = Modality.FINAL,
            visibility = visibility
        )

        destination += builderClassSymbol.createJavaMethod(
            name = field.name.toMethodName(builder),
            valueParameters = listOf(ConeLombokValueParameter(field.name, addMultipleParameterType)),
            returnTypeRef = builderType,
            modality = Modality.FINAL,
            visibility = visibility
        )

        destination += builderClassSymbol.createJavaMethod(
            name = Name.identifier("clear${field.name.identifier.capitalize()}"),
            valueParameters = listOf(),
            returnTypeRef = builderType,
            modality = Modality.FINAL,
            visibility = visibility
        )
    }

    private fun Name.toMethodName(builder: Builder): Name {
        val prefix = builder.setterPrefix
        return if (prefix.isNullOrBlank()) {
            this
        } else {
            Name.identifier("${prefix}${identifier.capitalize()}")
        }
    }

    private val String.singularForm: String?
        get() = StringUtil.unpluralize(this)

    private fun JavaClassifierType.parameterType(index: Int): JavaType? {
        return typeArguments.getOrNull(index)
    }

    private fun JavaType.withProperNullability(allowNull: Boolean): JavaType {
        return if (allowNull) makeNullable() else makeNotNullable()
    }
}

fun JavaType.makeNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.Nullable)
fun JavaType.makeNotNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.NotNull)

fun FirClassSymbol<*>.createJavaMethod(
    name: Name,
    valueParameters: List<ConeLombokValueParameter>,
    returnTypeRef: FirTypeRef,
    visibility: Visibility,
    modality: Modality,
    dispatchReceiverType: ConeSimpleKotlinType? = this.defaultType(),
    isStatic: Boolean = false
): FirJavaMethod {
    return buildJavaMethod {
        moduleData = this@createJavaMethod.moduleData
        this.returnTypeRef = returnTypeRef
        this.dispatchReceiverType = dispatchReceiverType
        this.name = name
        symbol = FirNamedFunctionSymbol(CallableId(classId, name))
        status = FirResolvedDeclarationStatusImpl(visibility, modality, visibility.toEffectiveVisibility(this@createJavaMethod)).apply {
            this.isStatic = isStatic
        }
        isFromSource = true
        annotationBuilder = { emptyList() }
        for (valueParameter in valueParameters) {
            this.valueParameters += buildJavaValueParameter {
                moduleData = this@createJavaMethod.moduleData
                this.returnTypeRef = valueParameter.typeRef
                containingFunctionSymbol = this@buildJavaMethod.symbol
                this.name = valueParameter.name
                annotationBuilder = { emptyList() }
                isVararg = false
                isFromSource = true
            }
        }
    }.apply {
        if (isStatic) {
            containingClassForStaticMemberAttr = this@createJavaMethod.toLookupTag()
        }
    }
}

fun FirClassSymbol<*>.createDefaultJavaConstructor(
    visibility: Visibility,
): FirJavaConstructor {
    val outerClassSymbol = this
    return buildJavaConstructor {
        moduleData = outerClassSymbol.moduleData
        isFromSource = true
        symbol = FirConstructorSymbol(classId)
        isInner = outerClassSymbol.rawStatus.isInner
        status = FirResolvedDeclarationStatusImpl(
            visibility,
            Modality.FINAL,
            visibility.toEffectiveVisibility(outerClassSymbol)
        ).apply {
            isExpect = false
            isActual = false
            isOverride = false
            isInner = this@buildJavaConstructor.isInner
        }
        this.visibility = visibility
        isPrimary = false
        returnTypeRef = buildResolvedTypeRef {
            type = outerClassSymbol.defaultType()
        }
        dispatchReceiverType = if (isInner) outerClassSymbol.defaultType() else null
        typeParameters += outerClassSymbol.typeParameterSymbols.map { buildConstructedClassTypeParameterRef { symbol = it } }
        annotationBuilder = { emptyList() }
    }
}

class ConeLombokValueParameter(val name: Name, val typeRef: FirTypeRef)

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.createJavaClass(
    session: FirSession,
    name: Name,
    visibility: Visibility,
    modality: Modality,
    isStatic: Boolean,
    superTypeRefs: List<FirTypeRef>,
): FirJavaClass? {
    val containingClass = this.fir as? FirJavaClass ?: return null
    val classId = containingClass.classId.createNestedClassId(name)
    return buildJavaClass {
        moduleData = containingClass.moduleData
        symbol = FirRegularClassSymbol(classId)
        this.name = name
        isFromSource = true
        this.visibility = visibility
        this.modality = modality
        this.isStatic = isStatic
        classKind = ClassKind.CLASS
        javaTypeParameterStack = containingClass.javaTypeParameterStack
        scopeProvider = JavaScopeProvider
        if (!isStatic) {
            typeParameters += containingClass.typeParameters.map {
                buildOuterClassTypeParameterRef { symbol = it.symbol }
            }
        }
        this.superTypeRefs += superTypeRefs
        val effectiveVisibility = containingClass.effectiveVisibility.lowerBound(
            visibility.toEffectiveVisibility(this@createJavaClass, forClass = true),
            session.typeContext
        )
        isTopLevel = false
        status = FirResolvedDeclarationStatusImpl(
            visibility,
            modality,
            effectiveVisibility
        ).apply {
            this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
            isCompanion = false
            isData = false
            isInline = false
            isFun = classKind == ClassKind.INTERFACE
        }
    }
}
