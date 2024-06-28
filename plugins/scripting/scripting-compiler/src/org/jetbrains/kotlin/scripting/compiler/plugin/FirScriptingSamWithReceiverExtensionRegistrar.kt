/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.scriptConfigurators
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.resolve.FirSamConversionTransformerExtension
import org.jetbrains.kotlin.fir.resolve.createFunctionType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.functionTypeService
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirScriptConfiguratorExtensionImpl
import org.jetbrains.kotlin.utils.addToStdlib.runIf


class FirScriptingSamWithReceiverExtensionRegistrar() : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirScriptingSamWithReceiverExtensionRegistrar::FirScriptSamWithReceiverConventionTransformer
    }

    class FirScriptSamWithReceiverConventionTransformer(
        session: FirSession
    ) : FirSamConversionTransformerExtension(session) {

        val knownAnnotations: Set<String> by lazy {
            session.extensionService.scriptConfigurators.flatMapTo(mutableSetOf()) {
                (it as? FirScriptConfiguratorExtensionImpl)?.knownAnnotationsForSamWithReceiver ?: emptySet()
            }
        }

        override fun getCustomFunctionTypeForSamConversion(function: FirSimpleFunction): ConeLookupTagBasedType? {
            val containingClassSymbol = function.containingClassLookupTag()?.toFirRegularClassSymbol(session) ?: return null
            return runIf(containingClassSymbol.resolvedAnnotationClassIds.any { it.asSingleFqName().asString() in knownAnnotations }) {
                val parameterTypes = function.valueParameters.map { it.returnTypeRef.coneType }
                if (parameterTypes.isEmpty()) return null
                val kind = session.functionTypeService.extractSingleSpecialKindForFunction(function.symbol) ?: FunctionTypeKind.Function
                createFunctionType(
                    kind,
                    parameters = parameterTypes.subList(1, parameterTypes.size),
                    receiverType = parameterTypes[0],
                    rawReturnType = function.returnTypeRef.coneType
                )
            }
        }
    }
}
