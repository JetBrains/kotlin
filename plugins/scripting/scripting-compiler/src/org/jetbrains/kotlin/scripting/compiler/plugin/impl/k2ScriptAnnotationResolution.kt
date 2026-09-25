/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.ArgumentTypeMismatch
import org.jetbrains.kotlin.fir.resolve.calls.NameNotFound
import org.jetbrains.kotlin.fir.resolve.calls.NoValueForParameter
import org.jetbrains.kotlin.fir.resolve.calls.TooManyArguments
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.scriptRefinedCompilationConfigurationsCache
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.FirScriptCompilationComponent
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.scriptCompilationConfiguration
import org.jetbrains.kotlin.scripting.resolve.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromNamedArgs
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.host.with
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.GetScriptingClassByClassLoader
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.toSourceCodePosition

/**
 * Collects the file annotations of the [script] accepted by the `onAnnotations` refinement handlers of the [compilationConfiguration],
 * resolving them in the session provided by [getSessionForAnnotationResolution]. The session is expected to see the annotation classes.
 *
 * An accepted annotation that cannot be constructed fails the collecting, unless [tolerateInvalidAnnotations] is set. In this case it is
 * returned as [InvalidScriptResolverAnnotation] and reported as a warning instead. The PSI-based hosts rely on this contract.
 */
@OptIn(SessionConfiguration::class, DirectDeclarationsAccess::class)
internal fun collectAndResolveScriptAnnotationsViaFir(
    script: SourceCode,
    compilationConfiguration: ScriptCompilationConfiguration,
    baseHostConfiguration: ScriptingHostConfiguration,
    getSessionForAnnotationResolution: (SourceCode, ScriptCompilationConfiguration) -> FirSession,
    convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile,
    tolerateInvalidAnnotations: Boolean = false,
): ResultWithDiagnostics<ScriptCollectedData> {
    val hostConfiguration =
        compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration].withDefaultsFrom(baseHostConfiguration)
    val messageCollector = ScriptDiagnosticsMessageCollector(null)
    val acceptedAnnotations = loadAcceptedAnnotationClasses(compilationConfiguration, hostConfiguration) { ann, e ->
        messageCollector.report(e.asDiagnostics(customMessage = "Failed to load annotation class ${ann.typeName}"))
    }.toList().takeIf { it.isNotEmpty() } ?: return ScriptCollectedData(emptyMap()).asSuccess()

    if (messageCollector.hasErrors()) return failure(messageCollector)

    val sessionForAnnotationResolution = getSessionForAnnotationResolution(script, compilationConfiguration)
    sessionForAnnotationResolution.register(
        FirScriptCompilationComponent::class,
        FirScriptCompilationComponent(
            hostConfiguration.with {
                reset(scriptRefinedCompilationConfigurationsCache)
            } ,
            getSessionForAnnotationResolution = { _, _ -> error("recursive refinement attempted") }
        )
    )

    // separate reporter for refinement to avoid double raw fir warnings reporting
    val diagnosticsCollector = DiagnosticsCollectorImpl()
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
    val firFile = script.convertToFir(sessionForAnnotationResolution, diagnosticsCollector)
    firFile.declarations.forEach {
        if (it is FirScript) {
            it.scriptCompilationConfiguration = compilationConfiguration
        }
    }
    if (diagnosticsCollector.hasErrors) {
        diagnosticsCollector.reportToMessageCollector(messageCollector, renderDiagnosticName = false)
        return failure(messageCollector)
    }

    fun ResultWithDiagnostics<Annotation>.toInvalidAnnotationIfFailed(name: String): ResultWithDiagnostics<Annotation> =
        when (this) {
            is ResultWithDiagnostics.Success -> this
            is ResultWithDiagnostics.Failure -> {
                val error = IllegalArgumentException(reports.joinToString("; ") { it.message }, reports.firstNotNullOfOrNull { it.exception })
                // the refinement handlers are invoked only if the annotations of their classes are found, so an invalid annotation
                // alone would go unnoticed - therefore it is additionally reported as a warning
                val warning = ScriptDiagnostic(
                    ScriptDiagnostic.unspecifiedError,
                    "Unable to construct the annotation $name: ${error.message}",
                    ScriptDiagnostic.Severity.WARNING,
                    script.locationId,
                )
                InvalidScriptResolverAnnotation(name, null, error).asSuccess(listOf(warning))
            }
        }

    fun loadAnnotation(firAnnotation: FirAnnotation): ResultWithDiagnostics<ScriptSourceAnnotation<Annotation>?> {
        val annotationCall = firAnnotation as? FirAnnotationCall ?: return ResultWithDiagnostics.Success(null)
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        val referencedName = annotationCall.referencedName()
        val annotation =
            annotationCall.toAnnotationObjectIfMatches(acceptedAnnotations, sessionForAnnotationResolution, firFile)
                ?.let { if (tolerateInvalidAnnotations) it.toInvalidAnnotationIfFailed(referencedName ?: "<unknown>") else it }
                ?: return ResultWithDiagnostics.Success(null)
        return annotation.onSuccess {
            val location = script.locationId
            val startPosition = firAnnotation.source?.startOffset?.toSourceCodePosition(script)
            val endPosition = firAnnotation.source?.endOffset?.toSourceCodePosition(script)
            ScriptSourceAnnotation(
                it,
                if (location != null && startPosition != null)
                    SourceCode.LocationWithId(
                        location, SourceCode.Location(startPosition, endPosition)
                    )
                else null
            ).asSuccess()
        }
    }

    return firFile.annotations.mapNotNullSuccess(::loadAnnotation).onSuccess { annotations ->
        ScriptCollectedData(mapOf(ScriptCollectedData.collectedAnnotations to annotations)).asSuccess()
    }
}

/**
 * Loads the annotation classes accepted by the `onAnnotations` refinement handlers of the [compilationConfiguration] according to the
 * [hostConfiguration].
 */
internal fun loadAcceptedAnnotationClasses(
    compilationConfiguration: ScriptCompilationConfiguration,
    hostConfiguration: ScriptingHostConfiguration,
    onError: (KotlinType, Throwable) -> Unit,
): Set<KClass<Annotation>> {
    val contextClassLoader = hostConfiguration[ScriptingHostConfiguration.jvm.baseClassLoader]
    val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
    val jvmGetScriptingClass = (getScriptingClass as? GetScriptingClassByClassLoader)
        ?: error("Expecting class implementing GetScriptingClassByClassLoader in the hostConfiguration[getScriptingClass], got $getScriptingClass")
    return compilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations].orEmpty().flatMapTo(LinkedHashSet()) {
        it.annotations.mapNotNull { ann ->
            try {
                @Suppress("UNCHECKED_CAST")
                jvmGetScriptingClass(ann, contextClassLoader, hostConfiguration) as? KClass<Annotation>
            } catch (e: Throwable) {
                onError(ann, e)
                null
            }
        }
    }
}

/**
 * Resolves the annotation call in the [session] and constructs the annotation object if the resolved annotation class is one of
 * [expectedAnnClasses].
 */
internal fun FirAnnotationCall.toAnnotationObjectIfMatches(
    expectedAnnClasses: List<KClass<out Annotation>>,
    session: FirSession,
    firFile: FirFile
): ResultWithDiagnostics<Annotation>? {
    val referencedShortName = referencedName()?.substringAfterLast('.')
    if (referencedShortName != null &&
        expectedAnnClasses.none { it.simpleName == referencedShortName } &&
        firFile.imports.none { it.aliasName != null }
    ) return null
    val resolvedAnnotation = resolve(session, firFile)

    (resolvedAnnotation.annotationTypeRef as? FirErrorTypeRef)?.let {
        // an unresolved annotation is reported only if it could be one of the expected ones
        return if (expectedAnnClasses.any { it.simpleName == referencedShortName }) {
            makeFailureResult(it.diagnostic.reason) // TODO: precise error with location (KT-83947)
        } else null
    }
    val fqName = resolvedAnnotation.annotationTypeRef.coneTypeOrNull?.classId?.asFqNameString() ?: return null
    val annClass = expectedAnnClasses.firstOrNull { it.qualifiedName == fqName } ?: return null

    val errors = mutableListOf<ScriptDiagnostic>()

    fun ConeKotlinType?.isArray(): Boolean {
        val classId = this?.lowerBoundIfFlexible()?.classId ?: return false
        return classId == StandardClassIds.Array || classId in StandardClassIds.primitiveArrayTypeByElementType.values
    }

    fun FirElement.reportError(message: String) {
        errors.add(message.asErrorDiagnostics(path = firFile.name, location = getLocation()))
    }

    @OptIn(UnresolvedExpressionTypeAccess::class)
    fun FirElement.toArgument(argName: String): Any? {

        fun FirExpression.convertAsCollection(arguments: List<FirExpression>): Any? {
            val collectionType = coneTypeOrNull
            if (!collectionType.isArray()) {
                reportError("Only arrays are supported as collections in annotation arguments, but $collectionType is passed")
                return null
            }
            return Array(arguments.size) { arguments[it].toArgument("element of $argName") }
        }

        return when (this) {
            // TODO: add support for class refs (KT-83500)
            is FirLiteralExpression -> toRuntimeValue()
            is FirVarargArgumentsExpression -> convertAsCollection(arguments)
            is FirCollectionLiteral -> convertAsCollection(argumentList.arguments)
            else -> {
                reportError("Unsupported annotation argument type: ${this::class.simpleName}")
                null
            }
        }
    }

    // The argument mapping omits the arguments that do not match a parameter, so the call itself has to be checked.
    (resolvedAnnotation.calleeReference as? FirDiagnosticHolder)?.let {
        resolvedAnnotation.reportError("Error resolving annotation: ${it.diagnostic.describe()}")
    }
    val evaluatedArguments = FirExpressionEvaluator.evaluateAnnotationArguments(resolvedAnnotation, session, firFile)
    val arguments = resolvedAnnotation.argumentMapping.mapping.map { [name, expression] ->
        val argName = name.asString()
        val value = when (val evaluated = evaluatedArguments[name]) {
            is FirEvaluatorResult.Evaluated -> evaluated.result.toArgument(argName)
            else -> {
                val resolutionError = expression.findResolutionError()
                if (resolutionError != null) {
                    expression.reportError("Error resolving annotation argument: $resolutionError")
                } else {
                    expression.reportError("Annotation argument is not a compile-time constant: ${expression.source?.text ?: evaluated}")
                }
                null
            }
        }
        argName to value
    }
    return if (errors.isNotEmpty()) makeFailureResult(errors)
    else annClass.instantiate(arguments, firFile)
}

private fun FirAnnotationCall.resolve(session: FirSession, firFile: FirFile): FirAnnotationCall {
    val scopeSession = ScopeSession()
    createImportingScopes(firFile, session, scopeSession)

    val dispatcher = object : FirAbstractBodyResolveTransformerDispatcher(
        session,
        FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS,
        scopeSession = scopeSession,
        implicitTypeOnly = false,
        expandTypeAliases = false,
        outerBodyResolveContext = null
    ) {
        override val expressionsTransformer: FirExpressionsResolveTransformer = FirExpressionsResolveTransformer(this)
        override val declarationsTransformer: FirDeclarationsResolveTransformer? = null
    }

    val transformer = dispatcher.expressionsTransformer
    return transformer.context.withFile(firFile, holder = transformer.components) {
        withFileAnalysisExceptionWrapping(firFile) {
            transformer.transformAnnotationCall(this, ResolutionMode.ContextDependent) as FirAnnotationCall
        }
    }
}

private fun FirAnnotationCall.referencedName(): String? =
    when (val typeRef = annotationTypeRef) {
        is FirUserTypeRef -> typeRef.qualifier.joinToString(".") { it.name.asString() }
        else -> typeRef.coneTypeOrNull?.classId?.asFqNameString()
    }

private fun ConeDiagnostic.describe(): String {
    val details = (this as? ConeInapplicableCandidateError)?.candidate?.diagnostics?.mapNotNull {
        when (it) {
            is NameNotFound -> "no parameter with name '${it.argument.name}'"
            is NoValueForParameter -> "no value passed for parameter '${it.valueParameter.name}'"
            is TooManyArguments -> "too many arguments"
            is ArgumentTypeMismatch -> "argument type mismatch: expected ${it.expectedType.renderReadable()}, actual ${it.actualType.renderReadable()}"
            else -> null
        }
    }.orEmpty()
    return if (details.isEmpty()) reason else "$reason (${details.joinToString()})"
}

private fun FirExpression.findResolutionError(): String? =
    when (this) {
        is FirDiagnosticHolder -> diagnostic.reason
        is FirWrappedArgumentExpression -> expression.findResolutionError()
        is FirVarargArgumentsExpression -> arguments.firstNotNullOfOrNull { it.findResolutionError() }
        is FirCollectionLiteral -> argumentList.arguments.firstNotNullOfOrNull { it.findResolutionError() }
        is FirResolvable -> (calleeReference as? FirDiagnosticHolder)?.diagnostic?.reason
        else -> null
    }

private fun KClass<out Annotation>.instantiate(arguments: List<Pair<String?, Any?>>, firFile: FirFile): ResultWithDiagnostics<Annotation> {
    val ctor = constructors.firstOrNull()
        ?: return makeFailureResult("No constructor found for the annotation $qualifiedName".asErrorDiagnostics(path = firFile.name))
    val mapping = tryCreateCallableMappingFromNamedArgs(ctor, arguments)
        ?: return makeFailureResult("Unable to map arguments of the annotation $simpleName".asErrorDiagnostics(path = firFile.name))
    return try {
        ctor.callBy(mapping).asSuccess()
    } catch (e: Throwable) {
        rethrowIntellijPlatformExceptionIfNeeded(e)
        makeFailureResult(e.asDiagnostics(path = firFile.name))
    }
}

private fun FirLiteralExpression.toRuntimeValue(): Any? {
    val value = value as? Number ?: return value
    return when (kind) {
        ConstantValueKind.Byte -> value.toByte()
        ConstantValueKind.Short -> value.toShort()
        ConstantValueKind.Int -> value.toInt()
        ConstantValueKind.Long -> value.toLong()
        ConstantValueKind.Float -> value.toFloat()
        ConstantValueKind.Double -> value.toDouble()
        else -> value
    }
}

// TODO: implement. Probably need to change SourceCode.Position to accept offsets and then remap them later on reporting
private fun FirElement.getLocation(): SourceCode.Location? = null
