package org.kotlinnative.translator

import org.kotlinnative.translator.llvm.LLVMScope
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.types.LLVMType
import java.util.*

class VariableManager(val globalVariableCollection: HashMap<String, LLVMVariable>) {

    private var fileVariableCollectionTree = HashMap<String, Stack<Pair<LLVMVariable, Int>>>()
    private var variableVersion = HashMap<String, Int>()

    operator fun get(variableName: String): LLVMVariable? {
        return fileVariableCollectionTree[variableName]?.peek()?.first ?: globalVariableCollection[variableName]
    }

    fun pullOneUpwardLevelVariable(variableName: String) {
        fileVariableCollectionTree[variableName]?.pop()
    }

    fun pullUpwardsLevel(level: Int) {
        fileVariableCollectionTree.forEach { s, stack -> while (!stack.empty() && stack.peek().second >= level) stack.pop() }
    }

    fun addVariable(name: String, variable: LLVMVariable, level: Int) {
        val stack = fileVariableCollectionTree.getOrDefault(name, Stack<Pair<LLVMVariable, Int>>())
        stack.push(Pair(variable, level))
        fileVariableCollectionTree.put(name, stack)
    }

    fun addGlobalVariable(name: String, variable: LLVMVariable) {
        globalVariableCollection.put(name, variable)
    }

    fun receiveVariable(name: String, type: LLVMType, scope: LLVMScope, pointer: Int): LLVMVariable {
        val ourVersion = variableVersion.getOrDefault(name, 0) + 1
        variableVersion.put(name, ourVersion)
        return LLVMVariable("managed.$name.$ourVersion", type, name, scope, pointer)
    }

}