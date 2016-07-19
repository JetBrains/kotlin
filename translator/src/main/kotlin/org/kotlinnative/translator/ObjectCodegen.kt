package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.LLVMVariableScope
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType

class ObjectCodegen(override val state: TranslationState, override val variableManager: VariableManager, val objectDeclaration: KtObjectDeclaration, override val codeBuilder: LLVMBuilder) :
        StructCodegen(state, variableManager, objectDeclaration, state.bindingContext.get(BindingContext.CLASS, objectDeclaration) ?: throw TranslationException(),
                codeBuilder) {
    override val size: Int = 0
    override val structName: String
    override val type: LLVMType = LLVMReferenceType(objectDeclaration.name.toString(), "class", byRef = true)

    init {
        structName = objectDeclaration.name!!
    }

    fun generate() {
        generate(objectDeclaration.declarations)
        val classInstance = LLVMVariable("object.instance.$structName", type, objectDeclaration.name, LLVMVariableScope())
        codeBuilder.addGlobalIntialize(classInstance, type)
        variableManager.addGlobalVariable(structName, classInstance)
    }
}
