/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.nodes

import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.util.name
import org.jetbrains.kotlin.sir.util.swiftIdentifier

public class SirTrampolineExtensionFunction(
    source: SirFunction,
) : SirTrampolineFunction(source) {
    override val isInstance: Boolean get() = true
    override val extensionReceiverParameter: SirParameter? get() = null

    override var body: SirFunctionBody?
        get() = buildTrampolineToSource(source) {
            add("let ${source.extensionReceiverParameter!!.name!!.swiftIdentifier} = self")
        }
        set(_) = Unit
}
