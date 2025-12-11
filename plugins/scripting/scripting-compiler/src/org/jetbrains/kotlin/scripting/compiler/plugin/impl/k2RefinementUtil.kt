/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromNamedArgs
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.map
import kotlin.collections.orEmpty
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asDiagnostics
import kotlin.script.experimental.api.asErrorDiagnostics
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.makeFailureResult


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
            // TODO: class refs?
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
                when (result) {
                    is FirEvaluatorResult.Evaluated -> argName to result.result.toArgument(argName)
                    else -> {
                        reportError("Error while evaluating annotation arguments: ${result::class.simpleName}")
                        null to null
                    }
                }
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

private fun FirAnnotationCall.evaluateArguments(session: FirSession, firFile: FirFile): Map<Name, FirEvaluatorResult>? {
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
    return FirExpressionEvaluator.evaluateAnnotationArguments(resolvedAnnotation, session)
}

// TODO: implement. Probably need to change SourceCode.Position to accept offsets and then remap them later on reporting
private fun FirElement.getLocation(): SourceCode.Location? = null
