/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.render

internal class IrSignaturesRenderer(private val output: Appendable, private val individualSignatureRenderer: IdSignatureRenderer) {

    fun render(signatures: IrSignaturesExtractor.Signatures) {
        header("Declared signatures: ${signatures.declaredSignatures.size}")
        sortedSignatures(signatures.declaredSignatures)
        blankLine()

        header("Imported signatures: ${signatures.importedSignatures.size}")
        sortedSignatures(signatures.importedSignatures)
    }

    private fun header(subject: String) {
        output.append("// ").append(subject).append('\n')
    }

    private fun blankLine() {
        output.append('\n')
    }

    private fun sortedSignatures(signatures: Set<IdSignature>) {
        signatures.asSequence()
                .map { signature -> signature.render(individualSignatureRenderer) }
                .sorted()
                .forEach { signatureText -> output.append(signatureText).append('\n') }
    }
}
