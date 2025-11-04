/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.k2

import androidx.compose.compiler.plugins.kotlin.COMPOSE_PLUGIN_ID
import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import androidx.compose.compiler.plugins.kotlin.ComposeMetadata
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenFunctionsSafe
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenPropertiesSafe
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationStringParameter
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.compilerPluginMetadata
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.JvmStandardClassIds

fun FirAnnotationContainer.hasComposableAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.Composable, session)

fun FirBasedSymbol<*>.hasComposableAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.Composable, session)

fun FirAnnotationContainer.hasReadOnlyComposableAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.ReadOnlyComposable, session)

fun FirBasedSymbol<*>.hasReadOnlyComposableAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.ReadOnlyComposable, session)

fun FirAnnotationContainer.hasDisallowComposableCallsAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.DisallowComposableCalls, session)

fun FirAnnotationContainer.hasComposableTargetMarkerAnnotation(session: FirSession): Boolean =
    hasAnnotation(ComposeClassIds.ComposableTargetMarker, session)

fun FirCallableSymbol<*>.isComposable(session: FirSession): Boolean =
    when (this) {
        is FirFunctionSymbol<*> ->
            hasComposableAnnotation(session)
        is FirPropertySymbol ->
            getterSymbol?.let {
                it.hasComposableAnnotation(session) || it.isComposableDelegate(session)
            } ?: false
        else -> false
    }

fun FirValueParameterSymbol.isComposable(context: CheckerContext): Boolean =
    resolvedReturnType.customAnnotations.hasAnnotation(ComposeClassIds.Composable, context.session) ||
            findSamFunction(context)?.isComposable(context.session) == true

private fun FirValueParameterSymbol.findSamFunction(context: CheckerContext): FirNamedFunctionSymbol? {
    val type = resolvedReturnType
    val session = context.session
    val classSymbol = type.toClassSymbol(session) ?: return null
    val samFunction = classSymbol
        .unsubstitutedScope(
            session,
            context.scopeSession,
            withForcedTypeCalculator = true,
            memberRequiredPhase = FirResolvePhase.DECLARATIONS
        )
        .collectAllFunctions()
        .singleOrNull { it.modality == Modality.ABSTRACT }
    return samFunction
}

fun FirCallableSymbol<*>.isReadOnlyComposable(session: FirSession): Boolean =
    when (this) {
        is FirFunctionSymbol<*> ->
            hasReadOnlyComposableAnnotation(session)
        is FirPropertySymbol ->
            getterSymbol?.hasReadOnlyComposableAnnotation(session) ?: false
        else -> false
    }

fun ConeKotlinType.isComposableFunction(session: FirSession): Boolean {
    val kind = functionTypeKind(session)
    return kind == ComposableFunction || kind == KComposableFunction
}


@OptIn(SymbolInternals::class)
private fun FirPropertyAccessorSymbol.isComposableDelegate(session: FirSession): Boolean {
    if (!propertySymbol.hasDelegate) return false
    fir.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
    return ((fir
        .body
        ?.statements
        ?.singleOrNull() as? FirReturnExpression)
        ?.result as? FirFunctionCall)
        ?.calleeReference
        ?.toResolvedCallableSymbol()
        ?.isComposable(session)
        ?: false
}

fun FirFunction.getDirectOverriddenFunctions(
    context: CheckerContext,
): List<FirFunctionSymbol<*>> {
    return when (val symbol = symbol) {
        is FirNamedFunctionSymbol -> {
            symbol.directOverriddenFunctionsSafe(context)
        }
        is FirPropertyAccessorSymbol -> {
            symbol.propertySymbol.directOverriddenPropertiesSafe(context).mapNotNull {
                if (symbol.isGetter) it.getterSymbol else it.setterSymbol
            }
        }
        else -> listOf()
    }.map { it.originalOrSelf() }
}

// TODO: Replace this with the FIR MainFunctionDetector once it lands upstream!
fun FirFunctionSymbol<*>.isMain(session: FirSession): Boolean {
    if (this !is FirNamedFunctionSymbol) return false
    if (typeParameterSymbols.isNotEmpty()) return false
    if (!resolvedReturnType.isUnit) return false
    if (jvmNameAsString(session) != "main") return false

    val parameterTypes = explicitParameterTypes
    when (parameterTypes.size) {
        0 -> {
            /*
            assert(DescriptorUtils.isTopLevelDeclaration(descriptor)) { "main without parameters works only for top-level" }
            val containingFile = DescriptorToSourceUtils.getContainingFile(descriptor)
            // We do not support parameterless entry points having JvmName("name") but different real names
            // See more at https://github.com/Kotlin/KEEP/blob/master/proposals/enhancing-main-convention.md#parameterless-main
            if (descriptor.name.asString() != "main") return false
            if (containingFile?.declarations?.any { declaration -> isMainWithParameter(declaration, checkJvmStaticAnnotation) } == true) {
                return false
            }*/
        }
        1 -> {
            val type = parameterTypes.single()
            if (!type.isArrayType || type.typeArguments.size != 1) return false
            val elementType = type.typeArguments[0].takeIf { it.kind != ProjectionKind.IN }?.type
                ?: return false
            if (!elementType.isString) return false
        }
        else -> return false
    }
    /*
    if (DescriptorUtils.isTopLevelDeclaration(descriptor)) return true

    val containingDeclaration = descriptor.containingDeclaration
    return containingDeclaration is ClassDescriptor
            && containingDeclaration.kind.isSingleton
            && (descriptor.hasJvmStaticAnnotation() || !checkJvmStaticAnnotation)
     */
    return true
}

private fun FirNamedFunctionSymbol.jvmNameAsString(session: FirSession): String =
    getAnnotationStringParameter(JvmStandardClassIds.Annotations.JvmName, session)
        ?: name.asString()

private val FirFunctionSymbol<*>.explicitParameterTypes: List<ConeKotlinType>
    get() = contextParameterSymbols.map { it.resolvedReturnType } +
            listOfNotNull(resolvedReceiverType) +
            valueParameterSymbols.map { it.resolvedReturnType }

internal val FirDeclaration.composeMetadata: ComposeMetadata?
    get() = compilerPluginMetadata?.get(COMPOSE_PLUGIN_ID)?.let { ComposeMetadata(it) }
