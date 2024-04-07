/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isSingleFieldValueClass
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.annotations.TRANSIENT_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.RuntimeVersions
import org.jetbrains.kotlinx.serialization.compiler.fir.*
import org.jetbrains.kotlinx.serialization.compiler.fir.checkers.FirSerializationErrors.EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR
import org.jetbrains.kotlinx.serialization.compiler.fir.checkers.FirSerializationErrors.EXTERNAL_SERIALIZER_USELESS
import org.jetbrains.kotlinx.serialization.compiler.fir.services.dependencySerializationInfoProvider
import org.jetbrains.kotlinx.serialization.compiler.fir.services.findTypeSerializerOrContextUnchecked
import org.jetbrains.kotlinx.serialization.compiler.fir.services.serializablePropertiesProvider
import org.jetbrains.kotlinx.serialization.compiler.fir.services.versionReader
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds

object FirSerializationPluginClassChecker : FirClassChecker(MppCheckerKind.Common) {
    private val JAVA_SERIALIZABLE_ID = ClassId.topLevel(FqName("java.io.Serializable"))
    private const val TOO_LOW = "too low"
    private const val UNKNOWN = "unknown"
    private const val NA = "N/A"

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        with(context) {
            val classSymbol = declaration.symbol
            checkMetaSerializableApplicable(classSymbol, reporter)
            checkInheritableSerialInfoNotRepeatable(classSymbol, reporter)
            checkEnum(classSymbol, reporter)
            checkExternalSerializer(classSymbol, reporter)
            if (!canBeSerializedInternally(classSymbol, reporter)) return
            if (classSymbol !is FirRegularClassSymbol) return

            val properties = buildSerializableProperties(classSymbol, reporter) ?: return
            checkCorrectTransientAnnotationIsUsed(classSymbol, properties.serializableProperties, reporter)
            checkTransients(classSymbol, reporter)
            analyzePropertiesSerializers(classSymbol, properties.serializableProperties, reporter)
            checkInheritedAnnotations(classSymbol, reporter)

            checkVersions(classSymbol, reporter)
        }
    }

    private fun CheckerContext.checkMetaSerializableApplicable(classSymbol: FirClassSymbol<out FirClass>, reporter: DiagnosticReporter) {
        if (classSymbol.classKind != ClassKind.ANNOTATION_CLASS) return
        if (!classSymbol.classId.isNestedClass) return
        val anno = classSymbol.resolvedAnnotationsWithClassIds
            .find { it.toAnnotationClassId(session) == SerializationAnnotations.metaSerializableAnnotationClassId }
            ?: return
        reporter.reportOn(anno.source, FirSerializationErrors.META_SERIALIZABLE_NOT_APPLICABLE, this)
    }

    private fun CheckerContext.checkInheritableSerialInfoNotRepeatable(
        classSymbol: FirClassSymbol<out FirClass>,
        reporter: DiagnosticReporter,
    ) {
        if (classSymbol.classKind != ClassKind.ANNOTATION_CLASS) return
        if (!session.annotationPlatformSupport.symbolContainsRepeatableAnnotation(classSymbol, session)) return
        val anno = classSymbol.resolvedAnnotationsWithClassIds
            .find { it.toAnnotationClassId(session) == SerializationAnnotations.inheritableSerialInfoClassId }
            ?: return
        reporter.reportOn(anno.source, FirSerializationErrors.INHERITABLE_SERIALINFO_CANT_BE_REPEATABLE, this)
    }

    private fun CheckerContext.checkExternalSerializer(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        val serializableKType = classSymbol.getSerializerForClass(session) ?: return
        val serializableClassSymbol = serializableKType.toRegularClassSymbol(session) ?: return

        val declarations = classSymbol.declarationSymbols

        val parametersCount = serializableKType.typeArguments.size
        if (parametersCount > 0) {
            val hasSuitableConstructor = declarations.filterIsInstance<FirConstructorSymbol>().any { constructor ->
                constructor.valueParameterSymbols.size == parametersCount
                        && constructor.valueParameterSymbols.all { param -> param.resolvedReturnType.isKSerializer }
            }

            if (!hasSuitableConstructor) {
                reporter.reportOn(
                    classSymbol.source,
                    EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR,
                    classSymbol,
                    serializableKType,
                    parametersCount.toString(),
                    this
                )
            }
        }

        val descriptorOverridden = declarations.filterIsInstance<FirPropertySymbol>().singleOrNull {
            it.name == SerialEntityNames.SERIAL_DESC_FIELD_NAME
                    && it.isOverride
                    && it.origin == FirDeclarationOrigin.Source
        } != null
        val serializeOverridden = declarations.filterIsInstance<FirFunctionSymbol<*>>().singleOrNull {
            it.name == SerialEntityNames.SAVE_NAME
                    && it.valueParameterSymbols.size == 2
                    && it.isOverride
                    && it.origin == FirDeclarationOrigin.Source
        } != null
        val deserializeOverridden = declarations.filterIsInstance<FirFunctionSymbol<*>>().singleOrNull {
            it.name == SerialEntityNames.LOAD_NAME
                    && it.valueParameterSymbols.size == 1
                    && it.isOverride
                    && it.origin == FirDeclarationOrigin.Source
        } != null

        if (descriptorOverridden && serializeOverridden && deserializeOverridden) {
            val source = classSymbol.getSerializerAnnotation(session)?.source ?: classSymbol.source
            reporter.reportOn(source, EXTERNAL_SERIALIZER_USELESS, classSymbol, this)
            return
        }

        val properties = session.serializablePropertiesProvider.getSerializablePropertiesForClass(serializableClassSymbol)
        if (!properties.isExternallySerializable) {
            val source = classSymbol.getSerializerAnnotation(session)?.source ?: classSymbol.source
            val error = if (serializableClassSymbol.moduleData == session.moduleData) {
                FirSerializationErrors.EXTERNAL_CLASS_NOT_SERIALIZABLE
            } else {
                FirSerializationErrors.EXTERNAL_CLASS_IN_ANOTHER_MODULE
            }
            reporter.reportOn(source, error, serializableClassSymbol, serializableKType, this)
        }

    }

    private fun CheckerContext.checkInheritedAnnotations(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        fun annotationsFilter(annotations: List<FirAnnotation>): List<Pair<ClassId, FirAnnotation>> {
            return annotations
                .filter { it.annotationTypeRef.toRegularClassSymbol(session)?.isInheritableSerialInfoAnnotation(session) == true }
                .mapNotNull { annotation -> annotation.toAnnotationClassId(session)?.let { it to annotation } }
        }

        val annotationByClassId = buildMap {
            putAll(annotationsFilter(classSymbol.resolvedAnnotationsWithClassIds))
        }

        for (superType in classSymbol.resolvedSuperTypes) {
            val superSymbol = superType.toRegularClassSymbol(session) ?: continue
            val superAnnotations = annotationsFilter(superSymbol.resolvedAnnotationsWithClassIds)
            for ((classId, superAnnotation) in superAnnotations) {
                val existingAnnotation = annotationByClassId[classId] ?: continue
                if (!existingAnnotation.hasSameArguments(superAnnotation, session)) {
                    reporter.reportOn(
                        existingAnnotation.source,
                        FirSerializationErrors.INCONSISTENT_INHERITABLE_SERIALINFO,
                        existingAnnotation.annotationTypeRef.coneType,
                        classSymbol.defaultType(),
                        this
                    )
                }
            }
        }
    }

    private fun FirAnnotation.hasSameArguments(other: FirAnnotation, session: FirSession): Boolean {
        val m1 = argumentMapping.mapping
        val m2 = other.argumentMapping.mapping
        if (m1.keys != m2.keys) return false
        for ((key, v1) in m1) {
            val v2 = m2.getValue(key)
            if (!v1.isEqualTo(v2, session)) return false
        }
        return true
    }

    private fun FirExpression.isEqualTo(other: FirExpression, session: FirSession): Boolean {
        return when {
            this is FirLiteralExpression<*> && other is FirLiteralExpression<*> -> kind == other.kind && value == other.value
            this is FirGetClassCall && other is FirGetClassCall -> AbstractTypeChecker.equalTypes(
                session.typeContext,
                resolvedType,
                other.resolvedType
            )

            this is FirPropertyAccessExpression && other is FirPropertyAccessExpression ->
                this.toResolvedCallableReference()?.resolvedSymbol == other.toResolvedCallableReference()?.resolvedSymbol

            else -> {
                val argumentsIfArray1 = when (this) {
                    is FirVarargArgumentsExpression -> arguments
                    is FirArrayLiteral -> arguments
                    else -> return false
                }
                val argumentsIfArray2 = when (other) {
                    is FirVarargArgumentsExpression -> other.arguments
                    is FirArrayLiteral -> other.arguments
                    else -> return false
                }
                argumentsIfArray1.size == argumentsIfArray2.size && argumentsIfArray1.zip(argumentsIfArray2)
                    .all { (a, b) -> a.isEqualTo(b, session) }
            }
        }
    }

    private fun CheckerContext.checkVersions(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        val currentVersions = session.versionReader.runtimeVersions ?: return
        if (!currentVersions.implementationVersionMatchSupported()) {
            reporter.reportOn(
                classSymbol.serializableOrMetaAnnotationSource(session),
                FirSerializationErrors.PROVIDED_RUNTIME_TOO_LOW,
                KotlinCompilerVersion.getVersion() ?: TOO_LOW,
                currentVersions.implementationVersion?.toString() ?: UNKNOWN,
                RuntimeVersions.MINIMAL_SUPPORTED_VERSION.toString(),
                this
            )
        }
        if (!currentVersions.currentCompilerMatchRequired()) {
            reporter.reportOn(
                classSymbol.serializableOrMetaAnnotationSource(session),
                FirSerializationErrors.REQUIRED_KOTLIN_TOO_HIGH,
                KotlinCompilerVersion.getVersion() ?: TOO_LOW,
                currentVersions.implementationVersion?.toString() ?: UNKNOWN,
                currentVersions.requireKotlinVersion?.toString() ?: NA,
                this
            )
        }
    }

    private fun CheckerContext.checkCorrectTransientAnnotationIsUsed(
        classSymbol: FirClassSymbol<*>,
        properties: List<FirSerializableProperty>,
        reporter: DiagnosticReporter
    ) {
        if (classSymbol.resolvedSuperTypes.any { it.classId == JAVA_SERIALIZABLE_ID }) return // do not check
        for (property in properties) {
            if (property.transient) continue
            val incorrectTransient =
                property.propertySymbol.backingFieldSymbol?.annotations?.getAnnotationByClassId(TRANSIENT_ANNOTATION_CLASS_ID, session)
            if (incorrectTransient != null) {
                reporter.reportOn(
                    source = incorrectTransient.source ?: property.propertySymbol.source,
                    FirSerializationErrors.INCORRECT_TRANSIENT,
                    this
                )
            }
        }
    }

    private fun CheckerContext.canBeSerializedInternally(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter): Boolean {
        // if enum has meta or SerialInfo annotation on a class or entries and used plugin-generated serializer
        if (session.dependencySerializationInfoProvider.useGeneratedEnumSerializer && classSymbol.isSerializableEnumWithMissingSerializer(session)) {
            reporter.reportOn(
                classSymbol.source,
                FirSerializationErrors.EXPLICIT_SERIALIZABLE_IS_REQUIRED,
                this,
                positioningStrategy = SourceElementPositioningStrategies.ENUM_MODIFIER
            )
            return false
        }

        checkCompanionSerializerDependency(classSymbol, reporter)

        if (!classSymbol.hasSerializableOrMetaAnnotation(session)) return false

        if (classSymbol.isAnonymousObjectOrInsideIt(this)) {
            reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource(session), FirSerializationErrors.ANONYMOUS_OBJECTS_NOT_SUPPORTED, this)
            return false
        }

        if (classSymbol.isInner) {
            reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource(session), FirSerializationErrors.INNER_CLASSES_NOT_SUPPORTED, this)
            return false
        }

        if (classSymbol.isInline && !session.versionReader.canSupportInlineClasses) {
            reporter.reportOn(
                classSymbol.serializableOrMetaAnnotationSource(session),
                FirSerializationErrors.INLINE_CLASSES_NOT_SUPPORTED,
                RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES.toString(),
                session.versionReader.runtimeVersions?.implementationVersion.toString(),
                this
            )
            return false
        }

        if (!classSymbol.hasSerializableOrMetaAnnotationWithoutArgs(session)) {
            // defined custom serializer
            checkClassWithCustomSerializer(classSymbol, reporter)
            return false
        }

        if (classSymbol.serializableAnnotationIsUseless(session)) {
            classSymbol.serializableOrMetaAnnotationSource(session)?.let {
                reporter.reportOn(it, FirSerializationErrors.SERIALIZABLE_ANNOTATION_IGNORED, this)
            }
            return false
        }

        // check that we can instantiate supertype
        if (!classSymbol.isEnumClass) { // enums are inherited from java.lang.Enum and can't be inherited from other classes
            val superClassSymbol = classSymbol.superClassOrAny(session)
            if (!superClassSymbol.isInternalSerializable(session)) {
                val noArgConstructorSymbol =
                    superClassSymbol.declarationSymbols.firstOrNull { it is FirConstructorSymbol && it.valueParameterSymbols.isEmpty() }
                if (noArgConstructorSymbol == null) {
                    reporter.reportOn(
                        classSymbol.serializableOrMetaAnnotationSource(session),
                        FirSerializationErrors.NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR,
                        this
                    )
                    return false
                }
            }
        }

        return true
    }

    private fun CheckerContext.checkCompanionSerializerDependency(
        classSymbol: FirClassSymbol<*>,
        reporter: DiagnosticReporter,
    ) {
        if (classSymbol !is FirRegularClassSymbol) return
        val companionObjectSymbol = classSymbol.companionObjectSymbol ?: return
        val serializerForInCompanion = companionObjectSymbol.getSerializerForClass(session)?.toRegularClassSymbol(session) ?: return
        val serializableWith: ConeKotlinType? = classSymbol.getSerializableWith(session)
        val context = this@checkCompanionSerializerDependency
        return if (classSymbol.hasSerializableOrMetaAnnotationWithoutArgs(session)) {
            if (serializerForInCompanion.classId == classSymbol.classId) {
                // @Serializable class Foo / @Serializer(Foo::class) companion object — prohibited due to problems with recursive resolve
                reporter.reportOn(
                    classSymbol.serializableOrMetaAnnotationSource(session),
                    FirSerializationErrors.COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED,
                    classSymbol,
                    context
                )
            } else {
                // @Serializable class Foo / @Serializer(Bar::class) companion object — prohibited as vague and confusing
                val source = companionObjectSymbol.getSerializerAnnotation(session)?.source
                reporter.reportOn(
                    source,
                    FirSerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS,
                    classSymbol.defaultType(),
                    serializerForInCompanion.defaultType(),
                    context
                )
            }
        } else if (serializableWith != null) {
            if (serializableWith.classId == companionObjectSymbol.classId && serializerForInCompanion.classId == classSymbol.classId) {
                // @Serializable(Foo.Companion) class Foo / @Serializer(Foo::class) companion object — the only case that is allowed
            } else {
                // @Serializable(anySer) class Foo / @Serializer(anyOtherClass) companion object — prohibited as vague and confusing
                reporter.reportOn(
                    companionObjectSymbol.getSerializerAnnotation(session)?.source,
                    FirSerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS,
                    classSymbol.defaultType(),
                    serializerForInCompanion.defaultType(),
                    context
                )
            }
        } else {
            // (regular) class Foo / @Serializer(something) companion object - not recommended
            reporter.reportOn(
                companionObjectSymbol.getSerializerAnnotation(session)?.source,
                FirSerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS,
                classSymbol.defaultType(),
                serializerForInCompanion.defaultType(),
                context
            )
        }
    }

    private fun CheckerContext.checkClassWithCustomSerializer(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        val serializerType = classSymbol.getSerializableWith(session)?.fullyExpandedType(session) ?: return

        val serializerForType = serializerType.serializerForType(session)?.fullyExpandedType(session)

        checkCustomSerializerMatch(classSymbol, source = null, classSymbol.defaultType(), serializerType, serializerForType, reporter)
        checkCustomSerializerIsNotLocal(source = null, classSymbol, serializerType, reporter)
        checkCustomSerializerParameters(classSymbol, null, serializerType, serializerForType, reporter)
        checkCustomSerializerNotAbstract(classSymbol, source = null, serializerType, reporter)
    }

    private fun FirClassSymbol<*>.isAnonymousObjectOrInsideIt(c: CheckerContext): Boolean {
        if (this is FirAnonymousObjectSymbol) return true
        return c.containingDeclarations.any { it is FirAnonymousObject }
    }

    private fun CheckerContext.checkEnum(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        if (!classSymbol.isEnumClass) return
        val entryBySerialName = mutableMapOf<String, FirEnumEntrySymbol>()
        for (enumEntrySymbol in classSymbol.collectEnumEntries()) {
            val serialNameAnnotation = enumEntrySymbol.getSerialNameAnnotation(session)
            val serialName = enumEntrySymbol.getSerialNameValue(session) ?: enumEntrySymbol.name.asString()
            val firstEntry = entryBySerialName[serialName]
            if (firstEntry != null) {
                reporter.reportOn(
                    source = serialNameAnnotation?.source ?: firstEntry.getSerialNameAnnotation(session)?.source ?: enumEntrySymbol.source,
                    FirSerializationErrors.DUPLICATE_SERIAL_NAME_ENUM,
                    classSymbol,
                    serialName,
                    enumEntrySymbol.name.asString(),
                    this
                )
            } else {
                entryBySerialName[serialName] = enumEntrySymbol
            }
        }
    }


    private fun CheckerContext.buildSerializableProperties(
        classSymbol: FirClassSymbol<*>,
        reporter: DiagnosticReporter,
    ): FirSerializableProperties? {
        if (!classSymbol.hasSerializableOrMetaAnnotation(session)) return null
        if (!classSymbol.isInternalSerializable(session)) return null
        if (classSymbol.isInternallySerializableObject(session)) return null

        val properties = session.serializablePropertiesProvider.getSerializablePropertiesForClass(classSymbol)
        if (!properties.isExternallySerializable) {
            reporter.reportOn(
                classSymbol.serializableOrMetaAnnotationSource(session),
                FirSerializationErrors.PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY,
                this
            )
        }

        // check that all names are unique
        val namesSet = mutableSetOf<String>()
        for (property in properties.serializableProperties) {
            val name = property.name
            if (!namesSet.add(name)) {
                reporter.reportOn(classSymbol.serializableOrMetaAnnotationSource(session), FirSerializationErrors.DUPLICATE_SERIAL_NAME, name, this)
            }
        }
        return properties
    }

    private fun CheckerContext.checkTransients(classSymbol: FirClassSymbol<*>, reporter: DiagnosticReporter) {
        for (propertySymbol in classSymbol.declarationSymbols.filterIsInstance<FirPropertySymbol>()) {
            val isInitialized = propertySymbol.isLateInit || declarationHasInitializer(propertySymbol)
            val transientAnnotation = propertySymbol.getSerialTransientAnnotation(session) ?: continue
            val hasBackingField = propertySymbol.hasBackingField
            if (!hasBackingField) {
                reporter.reportOn(transientAnnotation.source ?: propertySymbol.source, FirSerializationErrors.TRANSIENT_IS_REDUNDANT, this)
            } else if (!isInitialized) {
                reporter.reportOn(propertySymbol.source, FirSerializationErrors.TRANSIENT_MISSING_INITIALIZER, this)
            }
        }
    }

    private fun declarationHasInitializer(propertySymbol: FirPropertySymbol): Boolean {
        return when {
            propertySymbol.fromPrimaryConstructor -> propertySymbol.correspondingValueParameterFromPrimaryConstructor?.hasDefaultValue
                ?: false

            else -> propertySymbol.hasInitializer || propertySymbol.hasDelegate
        }
    }

    private fun CheckerContext.analyzePropertiesSerializers(
        classSymbol: FirClassSymbol<*>,
        properties: List<FirSerializableProperty>,
        reporter: DiagnosticReporter
    ) {
        val classLookupTag = classSymbol.toLookupTag()
        for (property in properties) {
            // Don't report anything on properties from supertypes
            if (!classLookupTag.isRealOwnerOf(property.propertySymbol)) continue
            val customSerializerType = property.serializableWith
            val serializerSymbol = customSerializerType?.toRegularClassSymbol(session)
            val propertySymbol = property.propertySymbol
            val typeRef = propertySymbol.resolvedReturnTypeRef
            val propertyType = typeRef.coneType.fullyExpandedType(session)
            val source = typeRef.source ?: propertySymbol.source
            if (customSerializerType != null && serializerSymbol != null) {
                // Do not account for @Polymorphic and @Contextual, as they are serializers for T: Any
                // and would not be compatible on direct comparison
                if (customSerializerType.classId in SerializersClassIds.setOfSpecialSerializers) return

                val serializerForType = customSerializerType.serializerForType(session)?.fullyExpandedType(session)

                checkCustomSerializerMatch(
                    classSymbol,
                    source = typeRef.source ?: propertySymbol.source,
                    propertyType,
                    customSerializerType,
                    serializerForType,
                    reporter
                )
                val annotationElement = propertySymbol.serializableAnnotation(needArguments = false, session)?.source
                checkCustomSerializerNotAbstract(classSymbol, source = annotationElement, customSerializerType, reporter)
                checkCustomSerializerIsNotLocal(source = annotationElement, classSymbol, customSerializerType, reporter)
                checkCustomSerializerParameters(classSymbol, annotationElement, customSerializerType, serializerForType, reporter)
                checkSerializerNullability(propertyType, customSerializerType, source, reporter)
            } else {
                checkType(typeRef, source, reporter)
                checkGenericArrayType(propertyType, source, reporter)
            }
        }
    }

    private fun CheckerContext.checkGenericArrayType(propertyType: ConeKotlinType, source: KtSourceElement?, reporter: DiagnosticReporter) {
        if (propertyType.isNonPrimitiveArray && propertyType.typeArguments.first().type?.isTypeParameter == true) {
            reporter.reportOn(
                source,
                FirSerializationErrors.GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED,
                this
            )
        }
    }

    private fun CheckerContext.checkTypeArguments(
        typeRef: FirTypeRef,
        fallbackSource: KtSourceElement?,
        reporter: DiagnosticReporter
    ) {
        val argsRefs = extractArgumentsTypeRefAndSource(typeRef) ?: return
        for (typeArgument in argsRefs) {
            val argTypeRef = typeArgument.typeRef ?: continue
            checkType(argTypeRef, typeArgument.source ?: fallbackSource, reporter)
        }
    }

    private fun CheckerContext.canSupportInlineClasses(): Boolean {
        return session.versionReader.canSupportInlineClasses
    }

    private fun ConeKotlinType.isUnsupportedInlineType(session: FirSession): Boolean = isSingleFieldValueClass(session) && !isPrimitiveOrNullablePrimitive

    private fun CheckerContext.checkType(typeRef: FirTypeRef, typeSource: KtSourceElement?, reporter: DiagnosticReporter) {
        val type = typeRef.coneType.fullyExpandedType(session)
        if (type.lowerBoundIfFlexible().isTypeParameter) return // type parameters always have serializer stored in class' field
        if (type.isUnsupportedInlineType(session) && !canSupportInlineClasses()) {
            reporter.reportOn(
                typeRef.source ?: typeSource,
                FirSerializationErrors.INLINE_CLASSES_NOT_SUPPORTED,
                RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES.toString(),
                session.versionReader.runtimeVersions?.implementationVersion.toString(),
                this
            )
        }

        val serializer = findTypeSerializerOrContextUnchecked(type, this)
        if (serializer != null) {
            val classSymbol = type.toRegularClassSymbol(session) ?: return
            type.getSerializableWith(session)?.fullyExpandedType(session)?.let { serializerType ->
                val serializerForType = serializerType.serializerForType(session)?.fullyExpandedType(session)

                checkCustomSerializerMatch(classSymbol, typeSource, type, serializerType, serializerForType, reporter)
                checkCustomSerializerIsNotLocal(typeSource, classSymbol, serializerType, reporter)

                val annotationElement = type.customAnnotations.serializableAnnotation(session)?.source ?: typeSource
                checkCustomSerializerParameters(classSymbol, annotationElement, serializerType, serializerForType, reporter)
                checkCustomSerializerNotAbstract(classSymbol, annotationElement, serializerType, reporter)
                checkSerializerNullability(type, serializerType, typeSource, reporter)
            }
            checkTypeArguments(typeRef, typeSource, reporter)
        } else {
            if (type.toRegularClassSymbol(session)?.isEnumClass != true) {
                // enums are always serializable
                reporter.reportOn(typeSource, FirSerializationErrors.SERIALIZER_NOT_FOUND, type, this)
            }
        }
    }

    private fun CheckerContext.checkCustomSerializerMatch(
        containingClassSymbol: FirClassSymbol<*>,
        source: KtSourceElement?,
        declarationType: ConeKotlinType,
        serializerType: ConeKotlinType,
        serializerForType: ConeKotlinType?,
        reporter: DiagnosticReporter
    ) {
        serializerForType ?: return

        val declarationTypeClassId = declarationType.classId
        if (declarationTypeClassId == null || declarationTypeClassId != serializerForType.classId) {
            reporter.reportOn(
                source ?: containingClassSymbol.serializableOrMetaAnnotationSource(session),
                FirSerializationErrors.SERIALIZER_TYPE_INCOMPATIBLE,
                declarationType,
                serializerType,
                serializerForType,
                this
            )
        }
    }

    private fun CheckerContext.checkCustomSerializerNotAbstract(
        containingClassSymbol: FirClassSymbol<*>,
        source: KtSourceElement?,
        serializerType: ConeKotlinType,
        reporter: DiagnosticReporter,
    ) {
        if (serializerType.isAbstractOrSealedOrInterface(session)) {
            reporter.reportOn(
                source ?: containingClassSymbol.serializableOrMetaAnnotationSource(session),
                FirSerializationErrors.ABSTRACT_SERIALIZER_TYPE,
                containingClassSymbol.defaultType(),
                serializerType,
                this
            )
        }
    }

    private fun CheckerContext.checkCustomSerializerParameters(
        containingClassSymbol: FirClassSymbol<*>,
        source: KtSourceElement?,
        serializerType: ConeKotlinType,
        serializerForType: ConeKotlinType?,
        reporter: DiagnosticReporter,
    ) {
        serializerForType ?: return

        // Do not account for @Polymorphic and @Contextual, as they are serializers for T: Any
        // and would not be compatible on direct comparison
        if (serializerType.classId in SerializersClassIds.setOfSpecialSerializers) {
            return
        }

        val primaryConstructor = serializerType.toRegularClassSymbol(session)?.primaryConstructorSymbol(session) ?: return

        val targetElement by lazy { source ?: containingClassSymbol.serializableOrMetaAnnotationSource(session) }

        val isExternalSerializer = serializerType.toRegularClassSymbol(session)?.getSerializerAnnotation(session) != null

        if ( // for external serializer, the verification will be carried out at the definition
            !isExternalSerializer
            // it is allowed that parameters are not passed in regular serializers at all
            && primaryConstructor.valueParameterSymbols.isNotEmpty()
            // if the parameters are still specified, then their number must match in the serializable class and constructor
            && serializerForType.typeArguments.size != primaryConstructor.valueParameterSymbols.size
        ) {
            val message = if (serializerForType.typeArguments.isNotEmpty()) {
                "expected no parameters or ${serializerForType.typeArguments.size}, but has ${primaryConstructor.valueParameterSymbols.size} parameters"
            } else {
                "expected no parameters but has ${primaryConstructor.valueParameterSymbols.size} parameters"
            }
            reporter.reportOn(
                targetElement,
                FirSerializationErrors.CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT,
                serializerType,
                serializerForType,
                message,
                this
            )
        }

        primaryConstructor.valueParameterSymbols.forEach { param ->
            val returnType = param.resolvedReturnType
            if (!returnType.isKSerializer) {
                reporter.reportOn(
                    targetElement,
                    FirSerializationErrors.CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE,
                    serializerType,
                    serializerForType,
                    param.name.asString(),
                    this
                )
            }
        }
    }

    private fun CheckerContext.checkCustomSerializerIsNotLocal(
        source: KtSourceElement?,
        classSymbol: FirClassSymbol<*>,
        serializerType: ConeKotlinType,
        reporter: DiagnosticReporter
    ) {
        val serializerClassId = serializerType.classId ?: return
        if (serializerClassId.isLocal) {
            reporter.reportOn(
                source ?: classSymbol.serializableOrMetaAnnotationSource(session),
                FirSerializationErrors.LOCAL_SERIALIZER_USAGE,
                serializerType,
                this
            )
        }
    }

    private fun CheckerContext.checkSerializerNullability(
        classType: ConeKotlinType,
        serializerType: ConeKotlinType,
        source: KtSourceElement?,
        reporter: DiagnosticReporter
    ) {
        // @Serializable annotation has proper signature so this error would be caught in type checker
        val serializerForType = serializerType.serializerForType(session) ?: return
        if (!classType.isMarkedNullable && serializerForType.isMarkedNullable) {
            reporter.reportOn(source, FirSerializationErrors.SERIALIZER_NULLABILITY_INCOMPATIBLE, serializerType, classType, this)
        }
    }
}
