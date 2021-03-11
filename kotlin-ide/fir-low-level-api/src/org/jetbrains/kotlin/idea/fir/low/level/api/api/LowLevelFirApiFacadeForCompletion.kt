/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessorCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.resolve.FirTowerDataContext
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateForCompletion
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

object LowLevelFirApiFacadeForCompletion {
    fun getResolveStateForCompletion(originalState: FirModuleResolveState): FirModuleResolveState {
        check(originalState is FirModuleResolveStateImpl)
        return FirModuleResolveStateForCompletion(originalState.project, originalState)
    }

    fun recordCompletionContextForFunction(
        firFile: FirFile,
        fakeElement: KtNamedFunction,
        originalElement: KtNamedFunction,
        state: FirModuleResolveState,
    ) {
        val firIdeProvider = firFile.session.firIdeProvider

        val originalFunction = state.getOrBuildFirFor(originalElement) as FirSimpleFunction
        val copyFunction = buildFunctionCopyForCompletion(firIdeProvider, fakeElement, originalFunction, state)

        state.lazyResolveDeclarationForCompletion(copyFunction, firFile, firIdeProvider, FirResolvePhase.BODY_RESOLVE)
        state.recordPsiToFirMappingsForCompletionFrom(copyFunction, firFile, fakeElement.containingKtFile)
    }

    fun recordCompletionContextForProperty(
        firFile: FirFile,
        fakeElement: KtProperty,
        originalElement: KtProperty,
        state: FirModuleResolveState,
    ) {
        val firIdeProvider = firFile.session.firIdeProvider

        val originalProperty = state.getOrBuildFirFor(originalElement) as FirProperty
        val copyProperty = buildPropertyCopyForCompletion(firIdeProvider, fakeElement, originalProperty, state)

        state.lazyResolveDeclarationForCompletion(copyProperty, firFile, firIdeProvider, FirResolvePhase.BODY_RESOLVE)
        state.recordPsiToFirMappingsForCompletionFrom(copyProperty, firFile, fakeElement.containingKtFile)
    }

    private fun buildFunctionCopyForCompletion(
        firIdeProvider: FirIdeProvider,
        element: KtNamedFunction,
        originalFunction: FirSimpleFunction,
        state: FirModuleResolveState
    ): FirSimpleFunction {
        val builtFunction = firIdeProvider.buildFunctionWithBody(element, originalFunction)

        // right now we can't resolve builtFunction header properly, as it built right in air,
        // without file, which is now required for running stages other then body resolve, so we
        // take original function header (which is resolved) and copy replacing body with body from builtFunction
        return buildSimpleFunctionCopy(originalFunction) {
            body = builtFunction.body
            symbol = builtFunction.symbol as FirNamedFunctionSymbol
            resolvePhase = minOf(originalFunction.resolvePhase, FirResolvePhase.DECLARATIONS)
            source = builtFunction.source
            session = state.rootModuleSession
        }.apply { reassignAllReturnTargets (builtFunction) }
    }

    private fun buildPropertyCopyForCompletion(
        firIdeProvider: FirIdeProvider,
        element: KtProperty,
        originalProperty: FirProperty,
        state: FirModuleResolveState
    ): FirProperty {
        val builtProperty = firIdeProvider.buildPropertyWithBody(element, originalProperty)

        val originalSetter = originalProperty.setter
        val builtSetter = builtProperty.setter

        // setter has a header with `value` parameter, and we want it type to be resolved
        val copySetter = if (originalSetter != null && builtSetter != null) {
            buildPropertyAccessorCopy(originalSetter) {
                body = builtSetter.body
                symbol = builtSetter.symbol
                resolvePhase = minOf(builtSetter.resolvePhase, FirResolvePhase.DECLARATIONS)
                source = builtSetter.source
                session = state.rootModuleSession
            }.apply { reassignAllReturnTargets(builtSetter) }
        } else {
            builtSetter
        }

        return buildPropertyCopy(originalProperty) {
            symbol = builtProperty.symbol
            initializer = builtProperty.initializer

            getter = builtProperty.getter
            setter = copySetter

            resolvePhase = minOf(originalProperty.resolvePhase, FirResolvePhase.DECLARATIONS)
            source = builtProperty.source
            session = state.rootModuleSession
        }
    }


    private fun FirFunction<*>.reassignAllReturnTargets(from: FirFunction<*>) {
        this.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element is FirReturnExpression && element.target.labeledElement == from) {
                    element.target.bind(this@reassignAllReturnTargets)
                }
                element.acceptChildren(this)
            }
        })
    }
}
