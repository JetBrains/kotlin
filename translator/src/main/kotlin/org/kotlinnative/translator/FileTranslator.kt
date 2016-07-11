package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.utils.FunctionDescriptor
import java.util.*

class FileTranslator(val state: TranslationState, val file: KtFile) {

    private var codeBuilder = LLVMBuilder()

    fun generateCode(): String {
        codeBuilder.clean()
        generateFileBody()
        return codeBuilder.toString()
    }

    private fun generateFileBody() {
        for (declaration in file.declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val function = FunctionCodegen(state, declaration, codeBuilder)
                    state.functions.put(function.name, FunctionDescriptor(
                            function.returnType,
                            function.args?.map { it.type }?.toList() ?: listOf()
                    ))

                    function.generate()
                }
            }
        }
    }

}
