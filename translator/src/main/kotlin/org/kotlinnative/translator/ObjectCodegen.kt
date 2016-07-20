package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.LLVMVariableScope
import org.kotlinnative.translator.llvm.types.LLVMReferenceType

class ObjectCodegen(override val state: TranslationState,
                    override val variableManager: VariableManager,
                    val objectDeclaration: KtObjectDeclaration,
                    override val codeBuilder: LLVMBuilder,
                    override val parentCodegen: StructCodegen? = null) :
        StructCodegen(state, variableManager, objectDeclaration, state.bindingContext.get(BindingContext.CLASS, objectDeclaration) ?: throw TranslationException(),
                codeBuilder, parentCodegen = parentCodegen) {
    override var size: Int = 0
    override val structName: String
    override val type: LLVMReferenceType

    init {
        structName = (if (parentCodegen != null) parentCodegen.structName + "." else "") + objectDeclaration.name!!
        type = LLVMReferenceType(structName, "class", byRef = true)
        generateInnerFields(objectDeclaration.declarations)
    }

    fun generate() {
        generate(objectDeclaration.declarations)
        val classInstance = LLVMVariable("object.instance.$structName", type, objectDeclaration.name, LLVMVariableScope(), pointer = 1)
        codeBuilder.addGlobalIntialize(classInstance, type)
        variableManager.addGlobalVariable(structName, classInstance)
    }
}
