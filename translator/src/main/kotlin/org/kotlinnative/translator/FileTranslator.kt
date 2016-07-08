package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlin.native.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMDescriptorGenearte
import org.kotlinnative.translator.llvm.LLVMMapStandardType

class FileTranslator(val state: TranslationState, val file: KtFile) {

    private var codeBuilder = LLVMBuilder()
    private var compiled = false;

    fun generateCode(): String {
        if (!compiled) {
            generateFileBody()
        }

        compiled = true
        return codeBuilder.toString()
    }

    private fun generateFileBody() {
        for (declaration in file.declarations) {
            when (declaration) {
                is KtNamedFunction -> FunctionCodegen(state, declaration, codeBuilder).generate()
            }
        }
    }

}
