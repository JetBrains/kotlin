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

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationStringParameter
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isArrayType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.type
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

fun FirCallableSymbol<*>.isReadOnlyComposable(session: FirSession): Boolean =
    when (this) {
        is FirFunctionSymbol<*> ->
            hasReadOnlyComposableAnnotation(session)
        is FirPropertySymbol ->
            getterSymbol?.hasReadOnlyComposableAnnotation(session) ?: false
        else -> false
    }

@OptIn(SymbolInternals::class)
private fun FirPropertyAccessorSymbol.isComposableDelegate(session: FirSession): Boolean {
    if (!propertySymbol.hasDelegate) return false
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
    context: CheckerContext
): List<FirFunctionSymbol<*>> {
    if (!isOverride && (this as? FirPropertyAccessor)?.propertySymbol?.isOverride != true)
        return listOf()

    val scope = (containingClassLookupTag()
        ?.toSymbol(context.session) as? FirClassSymbol<*>)
        ?.unsubstitutedScope(context)
        ?: return listOf()

    return when (val symbol = symbol) {
        is FirNamedFunctionSymbol -> {
            scope.processFunctionsByName(symbol.name) {}
            scope.getDirectOverriddenFunctions(symbol, true)
        }
        is FirPropertyAccessorSymbol -> {
            scope.getDirectOverriddenProperties(symbol.propertySymbol, true).mapNotNull {
                if (symbol.isGetter) it.getterSymbol else it.setterSymbol
            }
        }
        else -> listOf()
    }
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
    get() = resolvedContextReceivers.map { it.typeRef.coneType } +
        listOfNotNull(receiverParameter?.typeRef?.coneType) +
        valueParameterSymbols.map { it.resolvedReturnType }
