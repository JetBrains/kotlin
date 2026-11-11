/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.nodes

import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirErrorType
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirExtension
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionalType
import org.jetbrains.kotlin.sir.SirGenericDeclaration
import org.jetbrains.kotlin.sir.SirGetterFunction
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOptionalType
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirSelfType
import org.jetbrains.kotlin.sir.SirTupleType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypeConstraint
import org.jetbrains.kotlin.sir.SirTypeParameter
import org.jetbrains.kotlin.sir.SirTypeParameterType
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.optional
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.containsTypeParameter
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.sir.util.substituteTypeParameters
import org.jetbrains.kotlin.sir.util.unavailableTypes

internal data class ExtensionMapping(
    val extendedType: SirType,
    val extensionConstraints: List<SirTypeConstraint>,
    val remainingFunctionTypeParameters: List<SirTypeParameter>,
    val remainingFunctionConstraints: List<SirTypeConstraint>,
    val typeSubstitution: Map<SirTypeParameter, SirType>,
    val receiverTypeAnnotation: SirType? = null,
)

public class SirTrampolineExtension private constructor(
    public val source: SirFunction,
    override val extendedType: SirType,
    override val constraints: List<SirTypeConstraint>,
    public val trampoline: SirDeclaration,
) : SirExtension() {

    public companion object {
        private fun resolveExtensionMapping(source: SirFunction): ExtensionMapping? {
            val receiverType = source.extensionReceiverParameter?.type ?: return null
            val isOptional = receiverType is SirOptionalType
            val type = if (isOptional) receiverType.wrappedType else receiverType
            if (isOptional && type.containsTypeParameter()) return null

            return when (type) {
                is SirType.Metatype,
                is SirOptionalType,
                is SirTupleType,
                is SirFunctionalType,
                is SirSelfType,
                is SirErrorType,
                is SirUnsupportedType,
                    -> null

                is SirTypeParameterType -> {
                    if (isOptional) return null
                    val tp = type.parameter
                    if (tp !in source.typeParameters) return null
                    if (tp.bounds.isEmpty() || tp.bounds.any { (it as? SirNominalType)?.typeDeclaration !is SirProtocol || it.containsTypeParameter() }) {
                        return null
                    }
                    val primaryProto = (tp.bounds.first() as SirNominalType).typeDeclaration as SirProtocol
                    val substitution: Map<SirTypeParameter, SirType> = mapOf(tp to SirSelfType())
                    val remainingParams = source.typeParameters.filter { it != tp }.map { otherTp ->
                        if (otherTp.bounds.any { it.containsTypeParameter() }) {
                            SirTypeParameter(
                                name = otherTp.name,
                                variance = otherTp.variance,
                                bounds = otherTp.bounds.map { it.substituteTypeParameters(substitution) }.toMutableList(),
                                isPrimaryAssociatedType = otherTp.isPrimaryAssociatedType,
                                origin = otherTp.origin,
                            )
                        } else {
                            otherTp
                        }
                    }
                    val remainingNames = remainingParams.map { it.name }.toSet()
                    val extConstraints = mutableListOf<SirTypeConstraint>()
                    val fnConstraints = mutableListOf<SirTypeConstraint>()
                    for (extraBound in tp.bounds.drop(1)) {
                        extConstraints += SirTypeConstraint.Conformance(extraBound, listOf("Self"))
                    }
                    for (constraint in source.constraints) {
                        val head = constraint.subjectPath.firstOrNull()
                        val newPath = if (head == tp.name) {
                            listOf("Self") + constraint.subjectPath.drop(1)
                        } else {
                            constraint.subjectPath
                        }
                        val newRhs = constraint.constraint.substituteTypeParameters(substitution)
                        val remapped = when (constraint) {
                            is SirTypeConstraint.Conformance -> SirTypeConstraint.Conformance(newRhs, newPath)
                            is SirTypeConstraint.Equality -> SirTypeConstraint.Equality(newRhs, newPath)
                        }
                        val usesRemaining = (newPath.firstOrNull() in remainingNames) || newRhs.referencesAnyName(remainingNames)
                        if (usesRemaining) fnConstraints += remapped else extConstraints += remapped
                    }
                    ExtensionMapping(
                        extendedType = SirNominalType(primaryProto),
                        extensionConstraints = extConstraints.distinct(),
                        remainingFunctionTypeParameters = remainingParams,
                        remainingFunctionConstraints = fnConstraints.distinct(),
                        typeSubstitution = substitution,
                    )
                }

                is SirExistentialType -> {
                    val single = type.protocols.singleOrNull() ?: return null
                    val protocol = single.first
                    val protoArgs = single.second
                    if (protocol.typeParameters.isEmpty()) {
                        val ext = if (isOptional) SirNominalType(protocol).optional() else SirNominalType(protocol)
                        ExtensionMapping(
                            extendedType = ext,
                            extensionConstraints = emptyList(),
                            remainingFunctionTypeParameters = source.typeParameters,
                            remainingFunctionConstraints = source.constraints,
                            typeSubstitution = emptyMap(),
                        )
                    } else {
                        if (isOptional) return null
                        if (!type.containsTypeParameter()) {
                            val eqConstraints = if (protoArgs.size == protocol.typeParameters.size) {
                                protocol.typeParameters.zip(protoArgs).map { [assocParam, arg] ->
                                    SirTypeConstraint.Equality(
                                        constraint = arg,
                                        subjectPath = listOf(assocParam.name),
                                    )
                                }
                            } else {
                                emptyList()
                            }
                            ExtensionMapping(
                                extendedType = SirNominalType(protocol),
                                extensionConstraints = eqConstraints,
                                remainingFunctionTypeParameters = source.typeParameters,
                                remainingFunctionConstraints = source.constraints,
                                typeSubstitution = emptyMap(),
                                receiverTypeAnnotation = SirExistentialType(listOf(protocol to protoArgs)),
                            )
                        } else {
                            if (protoArgs.size != protocol.typeParameters.size) return null
                            val srcParams = protoArgs.map { (it as? SirTypeParameterType)?.parameter ?: return null }
                            if (srcParams.any { it !in source.typeParameters } || srcParams.distinct().size != srcParams.size) return null
                            buildParameterizedExtensionMapping(
                                source = source,
                                extendedType = SirNominalType(protocol),
                                targetTypeParams = protocol.typeParameters,
                                srcParamsInReceiver = srcParams,
                                needsExplicitBridgeableBound = false,
                                decl = protocol,
                                receiverTypeAnnotation = SirExistentialType(
                                    listOf(protocol to protocol.typeParameters.map(::SirTypeParameterType))
                                ),
                            )
                        }
                    }
                }

                is SirNominalType -> {
                    if (!type.containsTypeParameter()) {
                        val ext = if (isOptional) type.optional() else type
                        ExtensionMapping(
                            extendedType = ext,
                            extensionConstraints = emptyList(),
                            remainingFunctionTypeParameters = source.typeParameters,
                            remainingFunctionConstraints = source.constraints,
                            typeSubstitution = emptyMap(),
                        )
                    } else {
                        if (type.parent != null) return null
                        val decl = type.typeDeclaration
                        val declTypeParams = (decl as? SirGenericDeclaration)?.typeParameters.orEmpty()
                        val targetTypeParams = when {
                            decl == SirSwiftModule.array || decl == SirSwiftModule.set -> listOf(SirTypeParameter("Element"))
                            decl == SirSwiftModule.dictionary -> listOf(SirTypeParameter("Key"), SirTypeParameter("Value"))
                            declTypeParams.isNotEmpty() -> declTypeParams
                            else -> return null
                        }
                        if (type.typeArguments.size != targetTypeParams.size) return null
                        val srcParams = type.typeArguments.map { (it as? SirTypeParameterType)?.parameter ?: return null }
                        if (srcParams.any { it !in source.typeParameters } || srcParams.distinct().size != srcParams.size) return null
                        val needsExplicitBridgeableBound =
                            decl == SirSwiftModule.array || decl == SirSwiftModule.set || decl == SirSwiftModule.dictionary
                        buildParameterizedExtensionMapping(
                            source = source,
                            extendedType = SirNominalType(decl),
                            targetTypeParams = targetTypeParams,
                            srcParamsInReceiver = srcParams,
                            needsExplicitBridgeableBound = needsExplicitBridgeableBound,
                            decl = decl,
                            receiverTypeAnnotation = null,
                        )
                    }
                }
            }
        }

        private fun buildParameterizedExtensionMapping(
            source: SirFunction,
            extendedType: SirType,
            targetTypeParams: List<SirTypeParameter>,
            srcParamsInReceiver: List<SirTypeParameter>,
            needsExplicitBridgeableBound: Boolean,
            decl: SirDeclaration,
            receiverTypeAnnotation: SirType?,
        ): ExtensionMapping {
            val srcToTargetName = srcParamsInReceiver.zip(targetTypeParams).associate { [src, dst] -> src.name to dst.name }
            val substitution = srcParamsInReceiver.zip(targetTypeParams).associate { [src, dst] -> src to SirTypeParameterType(dst) }
            val remainingParams = source.typeParameters.filter { it !in srcParamsInReceiver }.map { tp ->
                if (tp.bounds.any { it.containsTypeParameter() }) {
                    SirTypeParameter(
                        name = tp.name,
                        variance = tp.variance,
                        bounds = tp.bounds.map { it.substituteTypeParameters(substitution) }.toMutableList(),
                        isPrimaryAssociatedType = tp.isPrimaryAssociatedType,
                        origin = tp.origin,
                    )
                } else {
                    tp
                }
            }
            val remainingNames = remainingParams.map { it.name }.toSet()

            val extConstraints = mutableListOf<SirTypeConstraint>()
            val fnConstraints = mutableListOf<SirTypeConstraint>()

            for ([src, dst] in srcParamsInReceiver.zip(targetTypeParams)) {
                for (bound in src.bounds) {
                    val boundDecl = (bound as? SirNominalType)?.typeDeclaration
                    val isBridgeable = boundDecl == KotlinRuntimeSupportModule.kotlinBridgeable
                    val isStdlibImplicitHashable = boundDecl == SirSwiftModule.hashable &&
                            ((decl == SirSwiftModule.set && dst.name == "Element") ||
                                    (decl == SirSwiftModule.dictionary && dst.name == "Key"))
                    if ((isBridgeable && !needsExplicitBridgeableBound) || isStdlibImplicitHashable) continue
                    val substBound = bound.substituteTypeParameters(substitution)
                    val c = SirTypeConstraint.Conformance(substBound, listOf(dst.name))
                    if (substBound.referencesAnyName(remainingNames)) fnConstraints += c else extConstraints += c
                }
            }

            for (constraint in source.constraints) {
                val head = constraint.subjectPath.firstOrNull()
                val newPath = if (head != null && head in srcToTargetName) {
                    listOf(srcToTargetName.getValue(head)) + constraint.subjectPath.drop(1)
                } else {
                    constraint.subjectPath
                }
                val newRhs = constraint.constraint.substituteTypeParameters(substitution)
                val remapped = when (constraint) {
                    is SirTypeConstraint.Conformance -> SirTypeConstraint.Conformance(newRhs, newPath)
                    is SirTypeConstraint.Equality -> SirTypeConstraint.Equality(newRhs, newPath)
                }
                val usesRemaining = (newPath.firstOrNull() in remainingNames) || newRhs.referencesAnyName(remainingNames)
                if (usesRemaining) fnConstraints += remapped else extConstraints += remapped
            }

            return ExtensionMapping(
                extendedType = extendedType,
                extensionConstraints = extConstraints.distinct(),
                remainingFunctionTypeParameters = remainingParams,
                remainingFunctionConstraints = fnConstraints.distinct(),
                typeSubstitution = substitution,
                receiverTypeAnnotation = receiverTypeAnnotation,
            )
        }

        private fun SirType.referencesAnyName(names: Set<String>): Boolean {
            if (names.isEmpty()) return false
            return when (this) {
                is SirTypeParameterType -> parameter.name in names
                is SirNominalType -> typeArguments.any { it.referencesAnyName(names) } || (parent?.referencesAnyName(names) == true)
                is SirExistentialType -> protocols.any { pair -> pair.second.any { it.referencesAnyName(names) } }
                is SirFunctionalType -> contextTypes.any { it.referencesAnyName(names) } ||
                        parameterTypes.any { it.referencesAnyName(names) } ||
                        errorType.referencesAnyName(names) ||
                        returnType.referencesAnyName(names)
                is SirTupleType -> types.any { it.second.referencesAnyName(names) }
                is SirType.Metatype -> type.referencesAnyName(names)
                is SirSelfType, is SirErrorType, is SirUnsupportedType -> false
            }
        }

        public operator fun invoke(source: SirFunction): SirTrampolineExtension? {
            if (source.isInstance) return null
            val mapping = resolveExtensionMapping(source) ?: return null
            return SirTrampolineExtension(
                source = source,
                extendedType = mapping.extendedType,
                constraints = mapping.extensionConstraints,
                trampoline = SirTrampolineExtensionFunction(source, mapping),
            )
        }

        public operator fun invoke(source: SirGetterFunction): SirTrampolineExtension? {
            val getter = source.getter
            if (getter.contextParameter != null) return SirTrampolineExtension(getter)
            if (getter.isInstance) return null
            val mapping = resolveExtensionMapping(getter) ?: return null
            if (mapping.remainingFunctionTypeParameters.isNotEmpty() || mapping.remainingFunctionConstraints.isNotEmpty()) {
                return null
            }
            return SirTrampolineExtension(
                source = getter,
                extendedType = mapping.extendedType,
                constraints = mapping.extensionConstraints,
                trampoline = SirTrampolineExtensionVariable(source.variableName, getter, source.setter, mapping),
            )
        }
    }

    override lateinit var parent: SirDeclarationParent

    override val origin: SirOrigin get() = SirOrigin.Trampoline(source)
    override val attributes: List<SirAttribute> by lazy {
        buildList {
            replaceOrAddPropagatedUnavailability {
                extendedType.unavailableTypes + constraints.flatMap { it.constraint.unavailableTypes }
            }
        }
    }
    override val visibility: SirVisibility get() = source.visibility

    override val documentation: String? get() = null
    override val protocols: List<SirProtocol> get() = emptyList()

    override val declarations: MutableList<SirDeclaration> = mutableListOf(trampoline.apply { parent = this@SirTrampolineExtension })
}
