package org.kotlinnative.translator

import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMMapStandardType
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.LLVMVariableScope


class PropertyCodegen(val state: TranslationState, val variableManager: VariableManager, val property: KtProperty, val codeBuilder: LLVMBuilder) {

    fun generate() {
        val varInfo = state.bindingContext.get(BindingContext.VARIABLE, property)?.compileTimeInitializer ?: return

        val kotlinType = varInfo.type
        val value = varInfo.value
        if (kotlinType.nameIfStandardType != null) {
            val variableType = LLVMMapStandardType(property.name ?: return, kotlinType).type
            val variable = LLVMVariable(property.name.toString(), variableType, property.name.toString(), LLVMVariableScope(), pointer = 1)
            variableManager.addGlobalVariable(property.name.toString(), variable)
            codeBuilder.declareGlobalVariable(variable, variableType.parseArg(value.toString()))
        }
    }


}