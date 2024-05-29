/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.nodes

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildGetterCopy
import org.jetbrains.kotlin.sir.builder.buildSetterCopy
import org.jetbrains.kotlin.sir.util.swiftFqName

public class SirTrampolineVariable(
    public val source: SirVariable
) : SirVariable() {
    override lateinit var parent: SirDeclarationParent
    override val origin: SirOrigin get() = SirOrigin.Trampoline(source)
    override val visibility: SirVisibility get() = source.visibility
    override val documentation: String? get() = source.documentation
    override val name: String get() = source.name
    override val type: SirType get() = source.type

    override val getter: SirGetter by lazy {
        buildGetterCopy(source.getter) {
            origin = SirOrigin.Trampoline(source.getter)
            kind = SirCallableKind.FUNCTION
            body = SirFunctionBody(
                listOf(
                    source.swiftFqName
                )
            )
        }
    }

    override val setter: SirSetter? by lazy {
        source.setter?.let { setter ->
            buildSetterCopy(setter) {
                origin = SirOrigin.Trampoline(setter)
                kind = SirCallableKind.FUNCTION
                body = SirFunctionBody(
                    listOf(
                        "${source.swiftFqName} = newValue"
                    )
                )
            }
        }
    }
}
