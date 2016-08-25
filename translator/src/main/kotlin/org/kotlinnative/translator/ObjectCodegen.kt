package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.LLVMVariableScope
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType

class ObjectCodegen(state: TranslationState,
                    variableManager: VariableManager,
                    val objectDeclaration: KtObjectDeclaration,
                    codeBuilder: LLVMBuilder,
                    packageName: String,
                    parentCodegen: StructCodegen? = null) :
        StructCodegen(state, variableManager, objectDeclaration, codeBuilder, packageName, parentCodegen) {
    override var size: Int = 0
    override val structName: String = objectDeclaration.fqName?.asString()!!
    override val type: LLVMReferenceType

    init {
        type = LLVMReferenceType(structName, "class", align = TranslationState.pointerAlign, size = TranslationState.pointerSize, byRef = true)
        if (parentCodegen != null) {
            type.location.addAll(parentCodegen.type.location)
            type.location.add(parentCodegen.structName)
        }
        primaryConstructorIndex = LLVMType.mangleFunctionArguments(emptyList())
        constructorFields.put(primaryConstructorIndex!!, arrayListOf())
    }

    override fun prepareForGenerate() {
        generateInnerFields(objectDeclaration.declarations)
        calculateTypeSize()
        type.size = size
        type.align = TranslationState.pointerAlign

        super.prepareForGenerate()

        val classInstance = LLVMVariable("object.instance.$fullName", type, objectDeclaration.name, LLVMVariableScope(), pointer = 1)
        codeBuilder.addGlobalInitialize(classInstance, fields, initializedFields.map {
            val type = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, it.value)!!.type!!
            Pair(it.key, state.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, it.value)!!.getValue(type).toString())
        }.toMap(), type)
        variableManager.addGlobalVariable(fullName, classInstance)
    }

    override fun generate() {
        super.generate()
    }
}
