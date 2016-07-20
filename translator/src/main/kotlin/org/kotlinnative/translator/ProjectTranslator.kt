package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.*
import org.kotlinnative.translator.llvm.LLVMBuilder

class ProjectTranslator(val state: TranslationState) {
    private var codeBuilder = state.codeBuilder

    fun generateCode(): String {
        codeBuilder.clean()
        generateProjectBody()
        return codeBuilder.toString()
    }

    private fun generateProjectBody() {
        for (property in state.properties.values) {
            property.generate()
        }

        for (objectCodegen in state.objects.values) {
            objectCodegen.generate()
        }

        for (clazz in state.classes.values) {
            clazz.generate()
        }

        for (function in state.functions.values) {
            function.generate()
        }
    }

}

