package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.*

class ProjectTranslator(val files: List<KtFile>, val state: TranslationState) {
    private var codeBuilder = state.codeBuilder

    fun generateCode(): String {
        with(files) {
            map { addClassDeclarations(it) }
            map { addObjectDeclarations(it) }
            map { addFunctionDeclarations(it) }
            map { addPropertyDeclarations(it) }
        }
        generateProjectBody()
        return codeBuilder.toString()
    }

    fun addFunctionDeclarations(file: KtFile) {
        val variableManager = VariableManager(state.globalVariableCollection)
        for (declaration in file.declarations.filter { it is KtNamedFunction }) {
            val function = FunctionCodegen(state, variableManager, declaration as KtNamedFunction, codeBuilder)
            if (function.external) {
                state.externalFunctions.put(function.fullName, function)
            } else {
                state.functions.put(function.fullName, function)
            }
        }
    }

    fun addClassDeclarations(file: KtFile) {
        val variableManager = VariableManager(state.globalVariableCollection)
        for (declaration in file.declarations.filter { it is KtClass }) {
            val codegen = ClassCodegen(state, variableManager, declaration as KtClass, codeBuilder)
            state.classes.put(codegen.structName, codegen)
        }
    }

    fun addPropertyDeclarations(file: KtFile) {
        val variableManager = VariableManager(state.globalVariableCollection)
        for (declaration in file.declarations.filter { it is KtProperty }) {
            val property = PropertyCodegen(state, variableManager, declaration as KtProperty, codeBuilder)
            state.properties.put(declaration.name!!, property)
        }
    }

    fun addObjectDeclarations(file: KtFile) {
        val variableManager = VariableManager(state.globalVariableCollection)
        for (declaration in file.declarations.filter { it is KtObjectDeclaration }) {
            val codegen = ObjectCodegen(state, variableManager, declaration as KtObjectDeclaration, codeBuilder)
            state.objects.put(codegen.structName, codegen)
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

        if (state.mainFunction != "main") {
            codeBuilder.declareEntryPoint(state.mainFunction)
        }
    }

}