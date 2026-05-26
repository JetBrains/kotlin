/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators.kotlin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
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
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.processAllClassifiers
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addIfNotNull

class LoggerGeneratorKey(val logAnnotation: FirAnnotation) : GeneratedDeclarationKey()

/**
 * Checks if the declaration origin is a logger annotation.
 *
 * @param logAnnotation Optional annotation to compare with the logger annotation.
 * If not provided, the function returns true if declaration is marked by any log annotation (`@Log`, `@Slf4j`, etc.).
 */
fun FirDeclarationOrigin.isLogger(logAnnotation: FirAnnotation? = null): Boolean {
    return this is FirDeclarationOrigin.Plugin && (key as? LoggerGeneratorKey)?.let {
        logAnnotation == null || it.logAnnotation == logAnnotation
    } == true
}

class LoggerGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val JAVA_PROPERTY_NAME = Name.identifier("java")
        private val JAVA_GET_NAME = Name.identifier("getName")

        private val PREDICATE = DeclarationPredicate.create {
            annotated(
                listOf(
                    LombokNames.LOG,
                    LombokNames.SLF4J,
                    LombokNames.LOG4J,
                    LombokNames.COMMONS_LOG,
                    LombokNames.FLOGGER,
                    LombokNames.JBOSS_LOG,
                    LombokNames.LOG4J2,
                    LombokNames.XSLF4J,
                )
            )
        }
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

        // Don't generate the companion if the `owner` isn't marked with `@Log`, `@Slf4j` or another annotation
        // or if config specifies the logger mustn't be static
        val firstLog = session.lombokService.getLogs(owner).firstOrNull()
        if (firstLog == null || !session.lombokService.config.logFieldIsStatic) {
            return null
        }

        return createCompanionObject(owner, LoggerGeneratorKey(firstLog.annotation)).symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return buildSet {
            if (classSymbol.origin.isLogger()) {
                add(SpecialNames.INIT) // Generated companion needs a constructor
            }
            addIfNotNull(logPropertiesCache.getValue(classSymbol, context)?.name)
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val origin = (context.owner.origin as? FirDeclarationOrigin.Plugin)?.key as? LoggerGeneratorKey ?: return emptyList()
        val constructor = createDefaultPrivateConstructor(context.owner, origin)
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

        val config = session.lombokService.config
        val targetClassSymbol: FirClassSymbol<*>

        // Generate log field based on the first encountered log annotation
        val log = if (classSymbol.isCompanion) {
            val logOnCompanion = session.lombokService.getLogs(classSymbol).firstOrNull()
            if (logOnCompanion != null) {
                targetClassSymbol = classSymbol
                logOnCompanion.takeIf { config.logFieldIsStatic } ?: return null
            } else {
                val outerClass = classSymbol.classId.outerClassId?.toSymbol(session) as? FirRegularClassSymbol ?: return null
                targetClassSymbol = outerClass
                session.lombokService.getLogs(outerClass).firstOrNull().takeIf { config.logFieldIsStatic } ?: return null
            }
        } else {
            targetClassSymbol = classSymbol
            session.lombokService.getLogs(classSymbol).firstOrNull().takeIf { classSymbol.classKind.isObject || !config.logFieldIsStatic }
                ?: return null
        }

        val logPropertyName = Name.identifier(config.logFieldName)

        // Ignore generation if a property with the same name already exists (but warn about it in a checker)
        var propertyAlreadyExists = false
        context.declaredScope?.processPropertiesByName(logPropertyName) {
            propertyAlreadyExists = propertyAlreadyExists || it.isRelevantForConflictsCheck
        }
        if (propertyAlreadyExists) return null

        return tryGeneratingLogProperty(log, classSymbol, targetClassSymbol)
    }

    /**
     * Immediately break (don't generate a property) if the necessary classes/methods can't be found to prevent compiler crashing
     * Report resolving errors instead.
     */
    private fun tryGeneratingLogProperty(
        log: ConeLombokAnnotations.AbstractLog,
        logContainingClass: FirClassSymbol<*>,
        logTargetClass: FirClassSymbol<*>
    ): FirPropertySymbol? {
        if (log.visibility == null) return null

        val loggerClassType = log.loggerClassId.constructClassLikeType()

        val topicExpression = if (log is ConeLombokAnnotations.FloggerLog) {
            null
        } else {
            tryGeneratingTopicExpression(log, logTargetClass.classId) ?: return null
        }
        val initializer = tryGeneratingInitializer(log, topicExpression, loggerClassType) ?: return null

        val config = session.lombokService.config

        return createMemberProperty(
            owner = logContainingClass,
            key = LoggerGeneratorKey(log.annotation),
            name = Name.identifier(config.logFieldName),
            returnType = loggerClassType,
        ) {
            visibility = log.visibility
        }.also { logProperty ->
            if (config.logFieldIsStatic) {
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

            // Finalize the property initializer
            logProperty.replaceInitializer(initializer)
        }.symbol
    }

    private fun tryGeneratingInitializer(
        log: ConeLombokAnnotations.AbstractLog,
        topicExpression: FirExpression?,
        loggerClassType: ConeClassLikeType,
    ): FirFunctionCall? {
        val loggerFactoryClassId = log.factoryClassId
        val factoryMethodName = log.getMethodName

        val loggerFactorySymbol =
            session.symbolProvider.getClassLikeSymbolByClassId(loggerFactoryClassId) as? FirRegularClassSymbol ?: return null

        @OptIn(SymbolInternals::class)
        val loggerFactoryStaticCallableMemberScope =
            loggerFactorySymbol.fir.scopeProvider.getStaticCallableMemberScope(loggerFactorySymbol.fir, session, ScopeSession())
                ?: return null

        var getLoggerFunctionSymbol: FirNamedFunctionSymbol? = null

        loggerFactoryStaticCallableMemberScope.processFunctionsByName(factoryMethodName) {
            if (getLoggerFunctionSymbol != null) return@processFunctionsByName

            if (topicExpression == null) {
                // For zero-argument factory methods (e.g., `FluentLogger.forEnclosingClass()`)
                if (it.valueParameterSymbols.isEmpty()) {
                    getLoggerFunctionSymbol = it
                }
            } else {
                // We are only interested in a method that returns type that matches topic expression type
                val singleValueParameterReturnType =
                    it.valueParameterSymbols.singleOrNull()?.resolvedReturnType?.lowerBoundIfFlexible()
                        ?: return@processFunctionsByName

                if (singleValueParameterReturnType.classId == topicExpression.resolvedType.classId) {
                    getLoggerFunctionSymbol = it
                }
            }
        }

        if (getLoggerFunctionSymbol == null) return null

        return buildFunctionCall {
            val loggerFactoryClassType = loggerFactoryClassId.constructClassLikeType()
            calleeReference = buildResolvedNamedReference {
                name = factoryMethodName
                resolvedSymbol = getLoggerFunctionSymbol
            }
            dispatchReceiver = buildResolvedQualifier {
                packageFqName = loggerFactoryClassId.packageFqName
                relativeClassFqName = loggerFactoryClassId.relativeClassName
                symbol = loggerFactorySymbol
                resolvedToCompanionObject = false
                coneTypeOrNull = loggerFactoryClassType
            }
            coneTypeOrNull = loggerClassType

            @OptIn(SymbolInternals::class)
            argumentList = buildResolvedArgumentList(
                original = null,
                linkedMapOf<FirExpression, FirValueParameter>().apply {
                    if (topicExpression != null) {
                        this[topicExpression] = getLoggerFunctionSymbol.valueParameterSymbols.single().fir
                    }
                }
            )
        }
    }

    private fun tryGeneratingTopicExpression(
        log: ConeLombokAnnotations.AbstractLog,
        targetClassId: ClassId,
    ): FirExpression? {
        return if (log.topic.isEmpty()) {
            // Generate `ClassWithLogger::class`
            val targetClassType = targetClassId.constructClassLikeType()
            val getClassCall = buildGetClassCall {
                argumentList = buildUnaryArgumentList(
                    buildResolvedQualifier {
                        packageFqName = targetClassId.packageFqName
                        relativeClassFqName = targetClassId.relativeClassName
                        symbol = targetClassType.toSymbol(session)
                        resolvedToCompanionObject = false
                        coneTypeOrNull = targetClassType
                    }
                )
                coneTypeOrNull = StandardClassIds.KClass.constructClassLikeType(arrayOf(targetClassType))
            }

            // Generate `ClassWithLogger::class.java`
            val javaPropertySymbol = session.symbolProvider
                .getTopLevelPropertySymbols(JvmStandardClassIds.BASE_JVM_PACKAGE, JAVA_PROPERTY_NAME)
                .singleOrNull() ?: return null

            val javaClassType =
                javaPropertySymbol.resolvedReturnType.toClassSymbol(session)?.constructType(arrayOf(targetClassType))
            val javaPropertyAccess = buildPropertyAccessExpression {
                extensionReceiver = getClassCall
                calleeReference = buildResolvedNamedReference {
                    name = JAVA_PROPERTY_NAME
                    resolvedSymbol = javaPropertySymbol
                }
                coneTypeOrNull = javaClassType
            }

            when (log) {
                is ConeLombokAnnotations.Log -> {
                    // Generate `ClassWithLogger::class.java.getName()`
                    @OptIn(SymbolInternals::class)
                    val javaClassFir = javaClassType?.toClassSymbol(session)?.fir ?: return null
                    val useSiteMemberScope =
                        javaClassFir.scopeProvider.getUseSiteMemberScope(javaClassFir, session, ScopeSession(), memberRequiredPhase = null)

                    var getNameFunction: FirFunctionSymbol<*>? = null
                    useSiteMemberScope.processFunctionsByName(JAVA_GET_NAME) {
                        if (getNameFunction == null && it.valueParameterSymbols.isEmpty()) {
                            getNameFunction = it
                        }
                    }
                    if (getNameFunction == null) return null

                    buildFunctionCall {
                        dispatchReceiver = javaPropertyAccess
                        calleeReference = buildResolvedNamedReference {
                            name = JAVA_GET_NAME
                            resolvedSymbol = getNameFunction
                        }
                        coneTypeOrNull = session.builtinTypes.stringType.coneType
                    }
                }
                is ConeLombokAnnotations.Slf4jLog,
                is ConeLombokAnnotations.Log4jLog,
                is ConeLombokAnnotations.CommonsLog,
                is ConeLombokAnnotations.JBossLog,
                is ConeLombokAnnotations.Log4j2Log,
                is ConeLombokAnnotations.XSlf4jLog
                    -> {
                    javaPropertyAccess
                }
                is ConeLombokAnnotations.FloggerLog -> error("@Flogger has no topic expression; should be handled in tryGeneratingLogProperty")
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
