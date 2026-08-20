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
import org.jetbrains.kotlin.sir.SirGetterFunction
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOptionalType
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirProtocol
import org.jetbrains.kotlin.sir.SirTupleType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirTypeConstraint
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.optional
import org.jetbrains.kotlin.sir.util.replaceOrAddPropagatedUnavailability
import org.jetbrains.kotlin.sir.util.unavailableTypes

public class SirTrampolineExtension private constructor(
    public val source: SirFunction,
    override val extendedType: SirType,
    public val trampoline: SirDeclaration,
) : SirExtension() {

    public companion object {
        private val SirFunction.extendedType: SirType?
            get() {
                val receiverType = extensionReceiverParameter?.type ?: return null
                val isOptional = receiverType is SirOptionalType
                val type = if (isOptional) receiverType.wrappedType else receiverType
                val extendedType = when (type) {
                    is SirType.Metatype -> null // we don't support extending metatypes
                    is SirOptionalType -> null // nested optionals are unsupported
                    is SirTupleType -> null // extending tuples is experimental
                    is SirFunctionalType -> null // functional types cannot be extended
                    is SirNominalType -> type
                    is SirExistentialType -> type.protocols.singleOrNull()?.let {
                        // single protocol can be extended as nominal type
                        SirNominalType(it.first, it.second)
                    }
                    is SirErrorType, is SirUnsupportedType -> null
                }
                return if (isOptional) extendedType?.optional() else extendedType
            }

        public operator fun invoke(source: SirFunction): SirTrampolineExtension? {
            if (source.isInstance) return null
            val extendedType = source.extendedType ?: return null
            return SirTrampolineExtension(
                source = source,
                extendedType = extendedType,
                trampoline = SirTrampolineExtensionFunction(source),
            )
        }

        public operator fun invoke(source: SirGetterFunction): SirTrampolineExtension? {
            val getter = source.getter
            if (getter.contextParameter != null) return SirTrampolineExtension(getter)
            if (getter.isInstance) return null
            val extendedType = getter.extendedType ?: return null
            return SirTrampolineExtension(
                source = getter,
                extendedType = extendedType,
                trampoline = SirTrampolineExtensionVariable(source.variableName, getter, source.setter),
            )
        }
    }

    override lateinit var parent: SirDeclarationParent

    override val origin: SirOrigin get() = SirOrigin.Trampoline(source)
    override val attributes: List<SirAttribute> by lazy {
        buildList {
            replaceOrAddPropagatedUnavailability { extendedType.unavailableTypes }
        }
    }
    override val visibility: SirVisibility get() = source.visibility

    override val documentation: String? get() = null
    override val constraints: List<SirTypeConstraint> get() = emptyList()
    override val protocols: List<SirProtocol> get() = emptyList()

    override val declarations: MutableList<SirDeclaration> = mutableListOf(trampoline.apply { parent = this@SirTrampolineExtension })
}
