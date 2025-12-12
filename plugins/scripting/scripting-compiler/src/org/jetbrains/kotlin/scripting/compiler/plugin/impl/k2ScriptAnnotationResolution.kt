/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromNamedArgs
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.GetScriptingClassByClassLoader
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.toSourceCodePosition

internal fun collectAndResolveScriptAnnotationsViaFir(
    script: SourceCode,
    compilationConfiguration: ScriptCompilationConfiguration,
    baseHostConfiguration: ScriptingHostConfiguration,
    getSessionForAnnotationResolution: (SourceCode, ScriptCompilationConfiguration) -> FirSession,
    convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile,
): ResultWithDiagnostics<ScriptCollectedData> {
    val hostConfiguration =
        compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration].withDefaultsFrom(baseHostConfiguration)
    val contextClassLoader = hostConfiguration[ScriptingHostConfiguration.jvm.baseClassLoader]
    val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
    val jvmGetScriptingClass = (getScriptingClass as? GetScriptingClassByClassLoader)
        ?: throw IllegalArgumentException("Expecting class implementing GetScriptingClassByClassLoader in the hostConfiguration[getScriptingClass], got $getScriptingClass")
    val acceptedAnnotations =
        compilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.flatMap {
            it.annotations.mapNotNull { ann ->
                @Suppress("UNCHECKED_CAST")
                jvmGetScriptingClass(ann, contextClassLoader, hostConfiguration) as? KClass<Annotation> // TODO errors
            }
        }?.takeIf { it.isNotEmpty() } ?: return ScriptCollectedData(emptyMap()).asSuccess()
    // separate reporter for refinement to avoid double raw fir warnings reporting
    val diagnosticsCollector = DiagnosticReporterFactory.createPendingReporter()

    val sessionForAnnotationResolution = getSessionForAnnotationResolution(script, compilationConfiguration)

    val firFile = script.convertToFir(sessionForAnnotationResolution, diagnosticsCollector)
    if (diagnosticsCollector.hasErrors) {
        val messageCollector = ScriptDiagnosticsMessageCollector(parentMessageCollector = null)
        diagnosticsCollector.reportToMessageCollector(messageCollector, renderDiagnosticName = false)
        return failure(messageCollector)
    }

    fun loadAnnotation(firAnnotation: FirAnnotation): ResultWithDiagnostics<ScriptSourceAnnotation<Annotation>?> =
        (firAnnotation as? FirAnnotationCall)?.toAnnotationObjectIfMatches(acceptedAnnotations, sessionForAnnotationResolution, firFile)?.onSuccess {
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
        } ?: ResultWithDiagnostics.Success(null)

    return firFile.annotations.mapNotNullSuccess(::loadAnnotation).onSuccess { annotations ->
        ScriptCollectedData(mapOf(ScriptCollectedData.collectedAnnotations to annotations)).asSuccess()
    }
}


internal fun FirAnnotationCall.toAnnotationObjectIfMatches(
    expectedAnnClasses: List<KClass<out Annotation>>,
    session: FirSession,
    firFile: FirFile
): ResultWithDiagnostics<Annotation>? {
    val shortName = when (val typeRef = annotationTypeRef) {
        is FirResolvedTypeRef -> typeRef.coneType.classId?.shortClassName ?: return null
        is FirUserTypeRef -> typeRef.qualifier.last().name
        else -> return null
    }.asString()
    val expectedAnnClass = expectedAnnClasses.firstOrNull { it.simpleName == shortName } ?: return null
    val ctor = expectedAnnClass.constructors.firstOrNull() ?: return null

    val evalRes = evaluateArguments(session, firFile).orEmpty()

    val errors = mutableListOf<ScriptDiagnostic>()

    fun ConeKotlinType?.isString() = this?.classId?.asFqNameString() == StandardNames.FqNames.string.asString()
    fun ConeKotlinType?.isArray() = this?.classId?.asFqNameString() == StandardNames.FqNames.array.asString()

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
            val elementType = collectionType?.typeArguments?.first()?.type
            return when {
                elementType.isString() -> Array(arguments.size) { arguments[it].toArgument("element of $argName") as? String }
                else -> {
                    reportError("Only string are supported now as collection element types in annotation arguments, but $elementType is passed")
                    null
                }
            }
        }

        return when (this) {
            is FirErrorExpression -> {
                reportError("Error resolving annotation argument: ${this.diagnostic.reason}")
                null
            }
            // TODO: add support for class refs (KT-83500)
            is FirLiteralExpression -> value
            is FirVarargArgumentsExpression -> convertAsCollection(arguments)
            is FirCollectionLiteral -> convertAsCollection(argumentList.arguments)
            else -> {
                reportError("Unsupported annotation argument type: ${this::class.simpleName}")
                null
            }
        }
    }

    val mapping =
        tryCreateCallableMappingFromNamedArgs(
            ctor,
            evalRes.map { (name, result) ->
                val argName = name.asString()
                argName to result.toArgument(argName)
            }
        )
    if (mapping == null) {
        reportError("Unable to map annotation arguments")
    }
    return when {
        errors.isNotEmpty() -> makeFailureResult(errors)
        else -> try {
            ctor.callBy(mapping!!).asSuccess()
        } catch (e: Error) {
            makeFailureResult(e.asDiagnostics())
        }
    }
}

private fun FirAnnotationCall.evaluateArguments(session: FirSession, firFile: FirFile): Map<Name, FirExpression> {
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
    val resolvedAnnotation =
        transformer.context.withFile(firFile, transformer.components) {
            withFileAnalysisExceptionWrapping(firFile) {
                transformer.transformAnnotationCall(this, ResolutionMode.ContextDependent) as FirAnnotationCall
            }
        }
    return resolvedAnnotation.argumentMapping.mapping
}

// TODO: implement. Probably need to change SourceCode.Position to accept offsets and then remap them later on reporting
private fun FirElement.getLocation(): SourceCode.Location? = null
