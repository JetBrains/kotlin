package org.kotlinnative.translator

import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.types.LLVMType
import java.util.*

class VariableManager {

    private var fileVariableCollectionTree = HashMap<String, Stack<Pair<LLVMVariable, Int>>>()
    private var globalVariableCollection = HashMap<String, LLVMVariable>()
    private var variableVersion = HashMap<String, Int>()

    fun getLLVMvalue(variableName: String): LLVMVariable? {
        return fileVariableCollectionTree[variableName]?.peek()?.first ?: globalVariableCollection[variableName]
    }

    fun pullUpwardsLevel(level: Int) {
        fileVariableCollectionTree.forEach { s, stack -> if (!stack.empty() && stack.peek().second >= level) stack.pop() else Unit }
    }

    fun addVariable(name: String, variable: LLVMVariable, level: Int) {
        val stack = fileVariableCollectionTree.getOrDefault(name, Stack<Pair<LLVMVariable, Int>>())
        stack.push(Pair(variable, level))
        fileVariableCollectionTree.put(name, stack)
    }

    fun getVariable(name: String, type: LLVMType, pointer: Boolean): LLVMVariable {
        val ourVersion = variableVersion.getOrDefault(name, 0) + 1
        variableVersion.put(name, ourVersion)

        return LLVMVariable("%managed.$name.$ourVersion", type, name, pointer)
    }

}