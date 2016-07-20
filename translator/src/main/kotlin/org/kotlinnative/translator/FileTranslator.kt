package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.*

class FileTranslator(val state: TranslationState, val file: KtFile) {

    private var codeBuilder = state.codeBuilder

    fun addDeclarations() {
        for (declaration in file.declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val function = FunctionCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.functions.put(function.name, function)
                }
                is KtClass -> {
                    val codegen = ClassCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.classes.put(declaration.name!!, codegen)
                }
                is KtProperty -> {
                    val property = PropertyCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.properties.put(declaration.name!!, property)
                }
                is KtObjectDeclaration -> {
                    val property = ObjectCodegen(state, VariableManager(state.globalVariableCollection), declaration, codeBuilder)
                    state.objects.put(declaration.name!!, property)
                }
            }
        }

    }
}

