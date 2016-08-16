package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.*

class ProjectTranslator(val files: List<KtFile>, val state: TranslationState) {
    private var codeBuilder = state.codeBuilder

    fun generateCode(): String {
        codeBuilder.clean()
        files.map { addDeclarations(it) }
        generateProjectBody()
        return codeBuilder.toString()
    }

    fun addDeclarations(file: KtFile) {
        val variableManager = VariableManager(state.globalVariableCollection)
        for (declaration in file.declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val function = FunctionCodegen(state, variableManager, declaration, codeBuilder)
                    if (function.external) {
                        state.externalFunctions.put(function.fullName, function)
                    } else {
                        state.functions.put(function.fullName, function)
                    }
                }
                is KtClass -> {
                    val codegen = ClassCodegen(state, variableManager, declaration, codeBuilder)
                    state.classes.put(declaration.name!!, codegen)
                }
                is KtProperty -> {
                    val property = PropertyCodegen(state, variableManager, declaration, codeBuilder)
                    state.properties.put(declaration.name!!, property)
                }
                is KtObjectDeclaration -> {
                    val property = ObjectCodegen(state, variableManager, declaration, codeBuilder)
                    state.objects.put(declaration.name!!, property)
                }
            }
        }
    }

    private fun generateProjectBody() {
        with(state) {
            properties.values.map { it.generate() }
            objects.values.map { it.prepareForGenerate() }
            classes.values.map { it.prepareForGenerate() }
            objects.values.map { it.generate() }
            classes.values.map { it.generate() }
            externalFunctions.values.map { it.generate() }
            functions.values.filter { it.isExtensionDeclaration }.map { it.generate() }
            functions.values.filter { !it.isExtensionDeclaration }.map { it.generate() }
        }
    }

}

