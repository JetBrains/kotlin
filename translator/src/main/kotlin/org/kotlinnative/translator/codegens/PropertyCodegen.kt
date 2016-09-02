package org.kotlinnative.translator.codegens

import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.VariableManager
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMInstanceOfStandardType
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.LLVMVariableScope


class PropertyCodegen(val state: TranslationState,
                      val variableManager: VariableManager,
                      val property: KtProperty,
                      val codeBuilder: LLVMBuilder) {

    fun generate() {
        val varInfo = state.bindingContext.get(BindingContext.VARIABLE, property)?.compileTimeInitializer ?: return

        val kotlinType = varInfo.type
        val value = varInfo.value
        if (kotlinType.nameIfStandardType != null) {
            val variableType = LLVMInstanceOfStandardType(property.name ?: return, kotlinType, state = state).type
            val variable = LLVMVariable(property.name.toString(), variableType, property.name.toString(), LLVMVariableScope())
            variableManager.addGlobalVariable(property.name.toString(), variable)
            codeBuilder.defineGlobalVariable(variable, variableType.parseArg(value.toString()))
            variable.pointer++
        }
    }

}