/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.nodes

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.util.allParameters
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.swiftFqName
import org.jetbrains.kotlin.sir.util.swiftIdentifier

public open class SirTrampolineFunction(
    public val source: SirFunction,
) : SirFunction() {
    override lateinit var parent: SirDeclarationParent
    override val origin: SirOrigin get() = SirOrigin.Trampoline(source)
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    override val name: String get() = source.name
    override val returnType: SirType get() = source.returnType
    override val isOverride: Boolean get() = false
    override val isInstance: Boolean get() = false
    override val modality: SirModality get() = SirModality.UNSPECIFIED
    override val fixity: SirFixity? get() = source.fixity

    override val attributes: List<SirAttribute> get() = source.attributes

    override val contextParameter: SirParameter?
        get() = source.contextParameter
    override val extensionReceiverParameter: SirParameter?
        get() = source.extensionReceiverParameter

    override val parameters: List<SirParameter> by lazy {
        source.parameters.mapIndexed { index, element ->
            if (element.argumentName == null && element.parameterName == null) {
                SirParameter(parameterName = "_$index", type = element.type, isVariadic = element.isVariadic)
            } else {
                element
            }
        }
    }

    override val errorType: SirType get() = source.errorType

    override val isAsync: Boolean get() = source.isAsync

    override val bridges: List<SirBridge> = emptyList()

    override var body: SirFunctionBody?
        get() = buildTrampolineToSource(source)
        set(_) = Unit
}

internal fun SirDeclaration.buildTrampolineToSource(
    source: SirFunction,
    additionalStatements: MutableList<String>.() -> Unit = {},
): SirFunctionBody? {
    if (attributes.any { it is SirAttribute.Available && it.isUnusable }) return null
    val statements = buildList {
        additionalStatements()
        if (source.parameters.any { it.isVariadic }) {
            addAll(source.body?.statements ?: return null)
        } else {
            add(buildString {
                append("return ")
                if (source.errorType != SirType.never) append("try ")
                if (source.isAsync) append("await ")
                append(source.swiftFqName)
                append("(")
                append(source.allParameters.joinToString { it.forward ?: error("unreachable") })
                append(")")
            })
        }
    }
    return SirFunctionBody(statements)
}

private val SirParameter.forward: String?
    get() = this.name?.let { name -> this.argumentName?.let { "${it.swiftIdentifier}: ${name.swiftIdentifier}" } ?: name.swiftIdentifier }
