/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.buildUnaryArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.processAllClassifiers
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addIfNotNull

data object LoggerGeneratorKey : GeneratedDeclarationKey()

val FirDeclarationOrigin.isLogger get() = this is FirDeclarationOrigin.Plugin && this.key == LoggerGeneratorKey

class LoggerGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val LOGGING_FQ_NAME = FqName("java.util.logging")
        private val LOGGER_CLASS_ID = ClassId.topLevel(LOGGING_FQ_NAME.child(Name.identifier("Logger")))
        private val GET_LOGGER_METHOD_NAME = Name.identifier("getLogger")
        private val QUALIFIER_NAME = Name.identifier("qualifiedName")

        private val PREDICATE = DeclarationPredicate.create { annotated(listOf(LombokNames.LOG)) }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    private val companionObjectsCache: FirCache<FirClassSymbol<*>, FirRegularClassSymbol?, NestedClassGenerationContext> =
        session.firCachesFactory.createCache(::initializeCompanionObjectIfNeeded)
    private val logPropertiesCache: FirCache<FirClassSymbol<*>, FirPropertySymbol?, MemberGenerationContext> =
        session.firCachesFactory.createCache(::initializeLogPropertyIfNeeded)

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (companionObjectsCache.getValue(classSymbol, context) != null) {
            return setOf(DEFAULT_NAME_FOR_COMPANION_OBJECT)
        }
        return emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        return companionObjectsCache.getValue(owner, context)
    }

    private fun initializeCompanionObjectIfNeeded(owner: FirClassSymbol<*>, context: NestedClassGenerationContext): FirRegularClassSymbol? {
        // Ignore local classes and anonymous objects to prevent potential exceptions
        if (owner.isLocal) {
            return null
        }

        // Check for already existing companion or normal objects
        if (owner.classKind.isObject) {
            return null
        }

        var companionAlreadyExists = false
        context.declaredScope?.processAllClassifiers {
            companionAlreadyExists = companionAlreadyExists || (it as? FirClassLikeSymbol)?.isCompanion == true
        }
        if (companionAlreadyExists) {
            return null
        }

        // Don't generate the companion if the `owner` isn't marked with `@Log` annotation
        // or if config specifies the logger mustn't be static
        if (session.lombokService.getLog(owner)?.fieldIsStatic != true) {
            return null
        }

        return createCompanionObject(owner, LoggerGeneratorKey).symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return buildSet {
            if (classSymbol.origin.isLogger) {
                add(SpecialNames.INIT) // Generated companion needs a constructor
            }
            addIfNotNull(logPropertiesCache.getValue(classSymbol, context)?.name)
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val constructor = createDefaultPrivateConstructor(context.owner, LoggerGeneratorKey)
        return listOf(constructor.symbol)
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        return logPropertiesCache.getValue(classSymbol, context)
            ?.let { listOf(it) }
            ?: emptyList()
    }

    private fun initializeLogPropertyIfNeeded(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): FirPropertySymbol? {
        // Ignore local classes and anonymous objects to prevent potential exceptions
        if (classSymbol.isLocal) {
            return null
        }

        val log = if (classSymbol.isCompanion) {
            val logOnCompanion = session.lombokService.getLog(classSymbol)
            if (logOnCompanion != null) {
                logOnCompanion.takeIf { it.fieldIsStatic } ?: return null
            } else {
                val outerClass = classSymbol.classId.outerClassId?.toSymbol(session) as? FirRegularClassSymbol ?: return null
                session.lombokService.getLog(outerClass)?.takeIf { it.fieldIsStatic } ?: return null
            }
        } else {
            session.lombokService.getLog(classSymbol)?.takeIf { classSymbol.classKind.isObject || !it.fieldIsStatic } ?: return null
        }

        val logPropertyName = Name.identifier(log.fieldName)

        // Ignore generation if a property with the same name already exists (but warn about it in a checker)
        var propertyAlreadyExists = false
        context.declaredScope?.processPropertiesByName(logPropertyName) {
            propertyAlreadyExists = propertyAlreadyExists || it.isRelevantForConflictsCheck
        }
        if (propertyAlreadyExists) return null

        return tryGeneratingLogProperty(log, classSymbol)
    }

    /**
     * Immediately break (don't generate a property) if the necessary classes/methods can't be found to prevent compiler crashing
     * Report resolving errors instead.
     */
    @OptIn(SymbolInternals::class)
    private fun tryGeneratingLogProperty(log: ConeLombokAnnotations.Log, logContainingClass: FirClassSymbol<*>): FirPropertySymbol? {
        if (log.visibility == null) return null
        val logContainingClassId = logContainingClass.classId
        val logPropertyName = Name.identifier(log.fieldName)

        val loggerSymbol =
            session.symbolProvider.getClassLikeSymbolByClassId(LOGGER_CLASS_ID) as? FirRegularClassSymbol ?: return null

        // We are only interested in static `getLogger(String)` signature
        val staticCallableMemberScope =
            loggerSymbol.fir.scopeProvider.getStaticCallableMemberScope(loggerSymbol.fir, session, ScopeSession())
                ?: return null
        val getLoggerFunctionSymbol = staticCallableMemberScope
            .getFunctions(GET_LOGGER_METHOD_NAME)
            .firstNotNullOfOrNull { function ->
                val singleValueParameter = function.valueParameterSymbols.singleOrNull() ?: return@firstNotNullOfOrNull null
                if (singleValueParameter.resolvedReturnType.lowerBoundIfFlexible() != session.builtinTypes.stringType.coneType)
                    return@firstNotNullOfOrNull null
                function
            } ?: return null

        val topicExpression = tryGeneratingTopicExpression(log, logContainingClassId) ?: return null

        return createMemberProperty(
            owner = logContainingClass,
            key = LoggerGeneratorKey,
            name = logPropertyName,
            returnType = LOGGER_CLASS_ID.constructClassLikeType(),
        ) {
            visibility = log.visibility
        }.also { logProperty ->
            if (log.fieldIsStatic) {
                // Add `@JvmStatic` annotation call
                logProperty.replaceAnnotations(
                    listOf(
                        buildAnnotationCall {
                            annotationTypeRef =
                                JvmStandardClassIds.Annotations.JvmStatic.constructClassLikeType().toFirResolvedTypeRef()
                            calleeReference = buildResolvedNamedReference {
                                name = JvmStandardClassIds.Annotations.JvmStatic.shortClassName
                                resolvedSymbol =
                                    session.symbolProvider.getClassLikeSymbolByClassId(JvmStandardClassIds.Annotations.JvmStatic)
                                        ?: return null
                            }
                            containingDeclarationSymbol = logProperty.symbol
                        }
                    )
                )
            }

            // Finalize the property initializer with `= ClassWithLogger.getLogger(ClassWithLogger::class.qualifiedName)`
            val loggerClassType = LOGGER_CLASS_ID.constructClassLikeType()
            logProperty.replaceInitializer(
                buildFunctionCall {
                    calleeReference = buildResolvedNamedReference {
                        name = GET_LOGGER_METHOD_NAME
                        resolvedSymbol = getLoggerFunctionSymbol
                    }
                    dispatchReceiver = buildResolvedQualifier {
                        packageFqName = LOGGER_CLASS_ID.packageFqName
                        relativeClassFqName = LOGGER_CLASS_ID.relativeClassName
                        symbol = loggerSymbol
                        resolvedToCompanionObject = false
                        coneTypeOrNull = loggerClassType
                    }
                    coneTypeOrNull = loggerClassType

                    argumentList = buildResolvedArgumentList(
                        original = null,
                        linkedMapOf(topicExpression to getLoggerFunctionSymbol.valueParameterSymbols.single().fir)
                    )
                }
            )
        }.symbol
    }

    private fun tryGeneratingTopicExpression(log: ConeLombokAnnotations.Log, targetClassId: ClassId): FirExpression? {
        return if (log.topic.isEmpty()) {
            // Generate `ClassWithLogger::class`
            val containingClassType = targetClassId.constructClassLikeType()
            val getClassCall = buildGetClassCall {
                argumentList = buildUnaryArgumentList(
                    buildResolvedQualifier {
                        packageFqName = targetClassId.packageFqName
                        relativeClassFqName = targetClassId.relativeClassName
                        symbol = containingClassType.toSymbol(session)
                        resolvedToCompanionObject = false
                        coneTypeOrNull = containingClassType
                    }
                )
                coneTypeOrNull = StandardClassIds.KClass.constructClassLikeType(arrayOf(containingClassType))
            }

            // Generate `ClassWithLogger::class.qualifiedName`
            val getClassQualifierNameSymbol = session.getClassDeclaredPropertySymbols(
                StandardClassIds.KClass, QUALIFIER_NAME,
            ).singleOrNull() ?: return null

            buildPropertyAccessExpression {
                dispatchReceiver = getClassCall
                calleeReference = buildResolvedNamedReference {
                    name = QUALIFIER_NAME
                    resolvedSymbol = getClassQualifierNameSymbol
                }
                // Use not nullable string because if it's null,
                // The `@Log` annotations must not be applicable (local, anonymous and other synthetic classes)
                coneTypeOrNull = session.builtinTypes.stringType.coneType
            }
        } else {
            buildLiteralExpression(
                source = null,
                kind = ConstantValueKind.String,
                value = log.topic,
                setType = true,
            )
        }
    }
}
