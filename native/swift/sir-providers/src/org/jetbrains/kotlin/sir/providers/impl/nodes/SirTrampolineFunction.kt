/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.nodes

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.swiftFqName

public class SirTrampolineFunction(
    public val source: SirFunction,
) : SirFunction() {
    override lateinit var parent: SirDeclarationParent
    override val origin: SirOrigin get() = SirOrigin.Trampoline(source)
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    override val kind: SirCallableKind get() = SirCallableKind.FUNCTION
    override val name: String get() = source.name
    override val returnType: SirType get() = source.returnType

    override val parameters: List<SirParameter> by lazy {
        source.parameters.mapIndexed { index, element ->
            if (element.argumentName == null && element.parameterName == null) {
                SirParameter(parameterName = "_$index", type = element.type)
            } else {
                element
            }
        }
    }

    override var body: SirFunctionBody?
        get() = SirFunctionBody(
            listOf(
                "${source.swiftFqName}(${this.parameters.joinToString { it.forward ?: error("unreachable") }})"
            )
        )
        set(_) = Unit
}

private val SirParameter.forward: String? get() = this.name?.let { name -> this.argumentName?.let { "$it: $name" } ?: name }
