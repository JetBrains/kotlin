package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.utils.FunctionDescriptor
import org.kotlinnative.translator.utils.KtType

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
                            KtType(function.returnType),
                            function.args?.map { KtType(it.type) }?.toList() ?: listOf()
                    ))

                    function.generate()
                }
            }
        }
    }

}
