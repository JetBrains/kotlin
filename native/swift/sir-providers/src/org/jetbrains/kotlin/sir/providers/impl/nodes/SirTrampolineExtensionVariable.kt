/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.nodes

import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.SirDeclarationParent
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirGetter
import org.jetbrains.kotlin.sir.SirModality
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirSetter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.SirVariable
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildGetter
import org.jetbrains.kotlin.sir.builder.buildSetter
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.swiftIdentifier

public class SirTrampolineExtensionVariable(
    override val name: String,
    public val getterSource: SirFunction,
    public val setterSource: SirFunction?,
) : SirVariable() {
    override lateinit var parent: SirDeclarationParent
    override val origin: SirOrigin get() = SirOrigin.Trampoline(getterSource)
    override val visibility: SirVisibility get() = getterSource.visibility
    override val documentation: String? get() = getterSource.documentation
    override val type: SirType get() = getterSource.returnType
    override val isConstant: Boolean get() = false
    override val isOverride: Boolean get() = false
    override val isInstance: Boolean get() = true
    override val modality: SirModality get() = SirModality.UNSPECIFIED
    override val attributes: List<SirAttribute> get() = getterSource.attributes

    override val bridges: List<SirBridge> get() = emptyList()

    override val getter: SirGetter by lazy {
        buildGetter {
            origin = SirOrigin.Trampoline(getterSource)
            attributes.addAll(getterSource.attributes)
        }.apply {
            parent = this@SirTrampolineExtensionVariable
            body = buildTrampolineToSource(getterSource) {
                add("let ${getterSource.extensionReceiverParameter!!.name!!.swiftIdentifier} = self")
            }
        }
    }

    override val setter: SirSetter? by lazy {
        if (setterSource == null) return@lazy null
        buildSetter {
            origin = SirOrigin.Trampoline(setterSource)
            attributes.addAll(setterSource.attributes)
            parameterName = setterSource.parameters.single().name!!
        }.apply {
            parent = this@SirTrampolineExtensionVariable
            body = buildTrampolineToSource(setterSource) {
                add("let ${setterSource.extensionReceiverParameter!!.name!!.swiftIdentifier} = self")
            }
        }
    }
}
