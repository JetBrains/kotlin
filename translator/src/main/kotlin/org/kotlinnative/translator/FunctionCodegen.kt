package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMFunctionType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import java.util.*


class FunctionCodegen(override val state: TranslationState, override val variableManager: VariableManager, val function: KtNamedFunction, override val codeBuilder: LLVMBuilder) :
        BlockCodegen(state, variableManager, codeBuilder) {

    var name = function.fqName.toString()
    var args = ArrayList<LLVMVariable>()

    init {
        val descriptor = state.bindingContext.get(BindingContext.FUNCTION, function)!!
        args.addAll(descriptor.valueParameters.map {
            LLVMMapStandardType(it.name.toString(), it.type)
        })

        returnType = LLVMMapStandardType("instance", descriptor.returnType!!)
        val retType = returnType!!.type
        when (retType) {
            is LLVMReferenceType -> {
                if (state.classes.containsKey(retType.type)) {
                    retType.prefix = "class"
                }

                retType.byRef = true
            }
        }
        if (retType is LLVMReferenceType && state.classes.containsKey(retType.type)) {
            retType.prefix = "class"
        }
    }

    fun generate(this_type: LLVMVariable? = null) {
        if (generateDeclaration(this_type)) {
            return
        }

        codeBuilder.addStartExpression()
        generateLoadArguments()
        evaluateCodeBlock(function.bodyExpression, scopeDepth = topLevel)

        if (!wasReturnOnTopLevel)
            codeBuilder.addAnyReturn(returnType!!.type)

        codeBuilder.addEndExpression()
    }

    private fun generateDeclaration(this_type: LLVMVariable? = null): Boolean {
        var external = false

        if (this_type != null) {
            args.add(this_type)
        }
        args.forEach {
            val type = it.type
            if (type is LLVMReferenceType && state.classes.containsKey(type.type)) {
                type.prefix = "class"
            }
        }

        var keyword = function.firstChild
        while (keyword != null) {
            if (keyword.text == "external") {
                external = true
                break
            }

            keyword = keyword.getNextSiblingIgnoringWhitespaceAndComments()
        }

        var actualReturnType: LLVMType = returnType!!.type
        val actualArgs = ArrayList<LLVMVariable>()

        if (returnType!!.pointer > 0) {
            actualReturnType = LLVMVoidType()
            actualArgs.add(returnType!!)
        }

        actualArgs.addAll(args)

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(function.fqName.toString(), actualArgs, actualReturnType, external, state.arm))
        return external
    }

    private fun generateLoadArguments() {
        args.forEach(fun(it: LLVMVariable) {
            if (it.type is LLVMFunctionType || (it.type is LLVMReferenceType && (it.type as LLVMReferenceType).byRef)) {
                variableManager.addVariable(it.label, LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope(), pointer = 1), topLevel)
                return
            }

            if (it.type !is LLVMReferenceType || (it.type as LLVMReferenceType).byRef) {
                val loadVariable = LLVMVariable("${it.label}", it.type, it.label, LLVMRegisterScope(), pointer = it.pointer)
                val allocVar = codeBuilder.loadArgument(loadVariable)
                variableManager.addVariable(it.label, allocVar, topLevel)
            } else {
                variableManager.addVariable(it.label, LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope(), pointer = 0), topLevel)
            }
        })
    }
}