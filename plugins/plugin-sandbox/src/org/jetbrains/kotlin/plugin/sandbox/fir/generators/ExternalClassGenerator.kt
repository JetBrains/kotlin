/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate.BuilderContext.or
import org.jetbrains.kotlin.fir.java.FirJavaLazyDeprecationsProvider
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.buildJavaClass
import org.jetbrains.kotlin.fir.java.declarations.buildJavaMethod
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaAnnotationList
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructStarProjectedType
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.plugin.sandbox.fir.SANDBOX_ANNOTATIONS_PACKAGE
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn
import org.jetbrains.kotlin.types.ConstantValueKind

/*
 * Generates class /foo.AllOpenGenerated with
 *  - empty public constructor
 *  - testClassName() functions for all classes annotated with @ExternalClassWithNested
 *  - NestedClassName nested classes for all classes annotated with @ExternalClassWithNested
 *  - function `materialize: ClassName` in those nested classes
 *
 * If there are no annotated classes then AllOpenGenerated class is not generated
 */
@OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
class ExternalClassGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val FOO_PACKAGE = FqName.topLevel(Name.identifier("foo"))
        private val GENERATED_CLASS_ID = ClassId(FOO_PACKAGE, Name.identifier("AllOpenGenerated"))
        private val MATERIALIZE_NAME = Name.identifier("materialize")

        private val PREDICATE = LookupPredicate.create { annotated("ExternalClassWithNested".fqn()) }
        private val JAVA_PREDICATE = LookupPredicate.create { annotated("JavaClassWithHiddenNested".fqn()) }
    }

    data object ExternalClassGeneratorKey : GeneratedDeclarationKey()
    data object JavaClassWithHiddenNestedGeneratorKey : GeneratedDeclarationKey()
    data object JavaAccessorGeneratorKey : GeneratedDeclarationKey()

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE.or(JAVA_PREDICATE)).filterIsInstance<FirRegularClassSymbol>()
    }
    private val classIdsForMatchedClasses: Map<ClassId, FirRegularClassSymbol> by lazy {
        matchedClasses.associateBy {
            GENERATED_CLASS_ID.createNestedClassId(Name.identifier("Nested${it.classId.shortClassName}"))
        }
    }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId != GENERATED_CLASS_ID) return null
        if (matchedClasses.isEmpty()) return null
        return createTopLevelClass(classId, ExternalClassGeneratorKey).symbol
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        return when (val origin = owner.origin) {
            is FirDeclarationOrigin.Plugin -> when (origin.key) {
                ExternalClassGeneratorKey -> generateNestedClass(owner.classId.createNestedClassId(name), owner)
                else -> null
            }
            is FirDeclarationOrigin.Java.Source -> generateNestedJavaClass(owner.classId.createNestedClassId(name), owner)
            else -> null
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val classId = context.owner.classId
        if (classId != GENERATED_CLASS_ID && classId !in classIdsForMatchedClasses) return emptyList()
        return listOf(createConstructor(context.owner, ExternalClassGeneratorKey).symbol)
    }

    private fun generateNestedClass(classId: ClassId, owner: FirClassSymbol<*>): FirClassLikeSymbol<*>? {
        if (owner.classId != GENERATED_CLASS_ID) return null
        val matchedClass = classIdsForMatchedClasses[classId] ?: return null

        return createNestedClass(owner, classId.shortClassName, ExternalClassGeneratorKey).also {
            it.matchedClass = matchedClass.classId
        }.symbol
    }

    private fun generateNestedJavaClass(classId: ClassId, owner: FirClassSymbol<*>): FirClassLikeSymbol<*>? {
        if (!owner.hasAnnotation(ClassId(SANDBOX_ANNOTATIONS_PACKAGE, FqName("JavaClassWithHiddenNested"), false), session)) {
            return null
        }
        val annotation = buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                coneType = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(StandardClassIds.Annotations.Deprecated),
                    emptyArray(),
                    isMarkedNullable = false
                )
            }
            argumentMapping = buildAnnotationArgumentMapping {
                mapping[StandardClassIds.Annotations.ParameterNames.deprecatedMessage] = buildLiteralExpression(
                    null, ConstantValueKind.String, "", setType = true
                )
                mapping[StandardClassIds.Annotations.ParameterNames.deprecatedLevel] = buildPropertyAccessExpression {
                    calleeReference = buildResolvedNamedReference {
                        name = Name.identifier("HIDDEN")
                        resolvedSymbol = FirEnumEntrySymbol(
                            CallableId(StandardClassIds.DeprecationLevel, Name.identifier("HIDDEN"))
                        )
                    }
                }
            }
        }
        return buildJavaClass {
            javaOrigin = FirDeclarationOrigin.Java.Plugin(JavaClassWithHiddenNestedGeneratorKey)
            key = JavaClassWithHiddenNestedGeneratorKey
            scopeProvider = JavaScopeProvider
            containingClassSymbol = owner
            javaTypeParameterStack = MutableJavaTypeParameterStack()
            name = classId.shortClassName
            moduleData = session.moduleData
            annotationList = object : FirJavaAnnotationList, List<FirAnnotation> by listOf(annotation) {}
            deprecationsProvider = FirJavaLazyDeprecationsProvider(listOf(annotation), session)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public,
            )
            classKind = ClassKind.CLASS
            symbol = FirRegularClassSymbol(classId)
        }.symbol
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner
        if (owner?.origin is FirDeclarationOrigin.Java.Source) {
            return generateJavaAccessor(callableId, context)
        }
        if (callableId.classId !in classIdsForMatchedClasses || callableId.callableName != MATERIALIZE_NAME) return emptyList()
        require(owner is FirRegularClassSymbol)
        val matchedClassId = owner.matchedClass ?: return emptyList()
        val matchedClassSymbol = session.getRegularClassSymbolByClassId(matchedClassId) ?: return emptyList()
        val function = createMemberFunction(
            owner, ExternalClassGeneratorKey, callableId.callableName, matchedClassSymbol.constructStarProjectedType(),
        ) {
            withGeneratedDefaultBody()
        }
        return listOf(function.symbol)
    }

    private fun generateJavaAccessor(callableId: CallableId, context: MemberGenerationContext): List<FirNamedFunctionSymbol> {
        val owner = context.owner
        if (!owner.hasAnnotation(ClassId(SANDBOX_ANNOTATIONS_PACKAGE, FqName("JavaClassWithGeneratedGetter"), false), session)) {
            return emptyList()
        }
        val accessor = buildJavaMethod {
            javaOrigin = FirDeclarationOrigin.Java.Plugin(JavaAccessorGeneratorKey)
            containingClassSymbol = owner
            dispatchReceiverType = owner.defaultType()
            name = callableId.callableName
            moduleData = session.moduleData
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public,
            )
            symbol = FirNamedFunctionSymbol(callableId)
            returnTypeRef = session.builtinTypes.intType
        }
        return listOf(accessor.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return when (classSymbol.classId) {
            in classIdsForMatchedClasses -> setOf(MATERIALIZE_NAME, SpecialNames.INIT)
            GENERATED_CLASS_ID -> setOf(SpecialNames.INIT)
            else -> if (classSymbol.origin is FirDeclarationOrigin.Java) setOf(Name.identifier("getFoo")) else emptySet()
        }
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return when {
            classSymbol.classId == GENERATED_CLASS_ID -> classIdsForMatchedClasses.keys.mapTo(mutableSetOf()) { it.shortClassName }
            classSymbol.origin is FirDeclarationOrigin.Java -> setOf(Name.identifier("Nested"))
            else -> emptySet()
        }
    }

    override fun getTopLevelClassIds(): Set<ClassId> {
        return if (matchedClasses.isEmpty()) emptySet() else setOf(GENERATED_CLASS_ID)
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == FOO_PACKAGE
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
        register(JAVA_PREDICATE)
    }
}

private object MatchedClassAttributeKey : FirDeclarationDataKey()

private var FirRegularClass.matchedClass: ClassId? by FirDeclarationDataRegistry.data(MatchedClassAttributeKey)
private val FirRegularClassSymbol.matchedClass: ClassId? by FirDeclarationDataRegistry.symbolAccessor(MatchedClassAttributeKey)

