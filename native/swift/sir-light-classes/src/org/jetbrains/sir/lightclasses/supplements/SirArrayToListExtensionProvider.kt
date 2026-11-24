/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.supplements

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypeConstraint
import org.jetbrains.kotlin.sir.SirTypeParameter
import org.jetbrains.kotlin.sir.SirTypeParameterType
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.nonOptional
import org.jetbrains.kotlin.sir.optional
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.generateFunctionBridge
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.providers.utils.allRequiredOptIns
import org.jetbrains.kotlin.sir.providers.utils.containingModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.sir.lightclasses.SirSupplementaryDeclarationsProvider
import org.jetbrains.sir.lightclasses.nodes.SirFunctionFromKtSymbol
import org.jetbrains.sir.lightclasses.utils.baseBridgeName
import org.jetbrains.sir.lightclasses.utils.bridgeFqName
import org.jetbrains.sir.lightclasses.utils.forwardBridgedBody
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal object SirArrayToListExtensionProvider : SirSupplementaryDeclarationsProvider {

    private val callableId = CallableId(
        packageName = StandardNames.COLLECTIONS_PACKAGE_FQ_NAME,
        callableName = Name.identifier("listOf"),
    )

    private const val PARAM_NAME = "elements"

    @OptIn(ExperimentalContracts::class)
    private val SirDeclaration.isListOfFunction: Boolean
        get() {
            contract {
                returns(true) implies (this@isListOfFunction is SirFunctionFromKtSymbol)
            }
            if (this !is SirFunctionFromKtSymbol || ktSymbol.callableId != callableId) return false
            val parameter = parameters.singleOrNull() ?: return false
            return parameter.name == PARAM_NAME && parameter.isVariadic
        }

    context(sirSession: SirSession, kaSession: KaSession)
    override fun provide(declaration: SirDeclaration): List<SirDeclaration> {
        if (!sirSession.collectionsV2 || !declaration.isListOfFunction) return emptyList()
        val returnType = declaration.returnType
        val origin = returnType.origin
        check(origin is SirType.Origin.ReifiedType.List) { "Unexpected origin type for listOf function: ${origin::class}" }
        check(returnType is SirExistentialType) { "Return type for listOf function should be SirExistentialType" }
        val [listProtocol, _] = returnType.protocols.single()
        val fqName = checkNotNull(declaration.bridgeFqName) { "Missing bridgeFqName for listOf function" }
        return buildExtension {
            extendedType = SirNominalType(SirSwiftModule.array)
            val elementConstraint = KotlinRuntimeSupportModule.kotlinBridgeableType.optional()
            val elementTypeParam = SirTypeParameter("Element", elementConstraint)
            constraints.add(
                SirTypeConstraint.Conformance(
                    constraint = elementConstraint.nonOptional(), // should be optional, but we can't properly describe this
                    subjectPath = listOf(elementTypeParam.name)
                )
            )
            val elementType = SirTypeParameterType(elementTypeParam)
            val parameter = SirParameter(
                argumentName = PARAM_NAME,
                type = elementType,
                isVariadic = true,
                kotlinName = PARAM_NAME
            )
            val returnType = SirExistentialType(
                protocols = listOf(listProtocol to listOf(elementType)),
                origin = SirType.Origin.ReifiedType.List(origin.typedProtocol, origin.typedStruct, elementType, origin.erasedType)
            )
            val bridgeProxy = generateFunctionBridge(
                baseBridgeName = fqName.baseBridgeName,
                explicitParameters = listOf(parameter),
                returnType = returnType,
                kotlinFqName = fqName,
                kotlinOptIns = declaration.ktSymbol.allRequiredOptIns,
                selfParameter = null,
                contextParameters = emptyList(),
                extensionReceiverParameter = null,
                errorParameter = null,
                isAsync = false,
            )
            checkNotNull(bridgeProxy) { "Missing bridgeProxy for listOf function" }
            val functionName = "toList"
            val body = declaration.forwardBridgedBody(bridgeProxy, false, "member", functionName)
            buildFunction {
                name = functionName
                this.returnType = returnType
                this.body = SirFunctionBody(buildList {
                    add("let $PARAM_NAME = self")
                    addAll(body.statements)
                })
            }.also(declarations::add)
        }.apply {
            declarations.forEach { it.parent = this }
            parent = declaration.containingModule()
        }.let(::listOf)
    }
}
