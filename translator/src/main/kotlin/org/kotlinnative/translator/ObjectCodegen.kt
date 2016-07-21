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
                    parentCodegen: StructCodegen? = null) :
        StructCodegen(state, variableManager, objectDeclaration, state.bindingContext.get(BindingContext.CLASS, objectDeclaration) ?: throw TranslationException(),
                codeBuilder, parentCodegen = parentCodegen) {
    override var size: Int = 0
    override val structName: String = objectDeclaration.name!!
    override val type: LLVMReferenceType

    init {
        type = LLVMReferenceType(structName, "class", byRef = true)
        if (parentCodegen != null) {
            type.location.addAll(parentCodegen.type.location)
            type.location.add(parentCodegen.structName)
        }
        generateInnerFields(objectDeclaration.declarations)
    }

    fun generate() {
        generate(objectDeclaration.declarations)
        val classInstance = LLVMVariable("object.instance.$structName", type, objectDeclaration.name, LLVMVariableScope(), pointer = 1)
        codeBuilder.addGlobalIntialize(classInstance, type)
        variableManager.addGlobalVariable(fullName, classInstance)
    }
}
