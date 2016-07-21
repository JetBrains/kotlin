package org.kotlinnative.translator

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMFunctionType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import java.util.*


class FunctionCodegen(override val state: TranslationState,
                      override val variableManager: VariableManager,
                      val function: KtNamedFunction,
                      override val codeBuilder: LLVMBuilder,
                      val parentCodegen: StructCodegen? = null) :
        BlockCodegen(state, variableManager, codeBuilder) {

    var name = function.fqName.toString()
    var args = LinkedList<LLVMVariable>()
    val isExtensionDeclaration = function.isExtensionDeclaration()
    var functionNamePrefix = ""
    val fullName: String
        get() = functionNamePrefix + name

    init {
        val descriptor = state.bindingContext.get(BindingContext.FUNCTION, function)!!
        args.addAll(descriptor.valueParameters.map {
            LLVMInstanceOfStandardType(it.name.toString(), it.type)
        })

        returnType = LLVMInstanceOfStandardType("instance", descriptor.returnType!!)
        val retType = returnType!!.type
        when (retType) {
            is LLVMReferenceType -> {
                if (state.classes.containsKey(retType.type)) {
                    retType.prefix = "class"
                    returnType!!.pointer = 2
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

        if (isExtensionDeclaration) {
            val receiverParameter = state.bindingContext.get(BindingContext.FUNCTION, function)!!.extensionReceiverParameter!!
            val receiverType = receiverParameter.type
            val translatorType = LLVMMapStandardType(receiverType)

            val extensionFunctionsOfThisType = state.extensionFunctions.getOrDefault(translatorType, HashMap())
            extensionFunctionsOfThisType.put(name, this)
            state.extensionFunctions.put(translatorType.toString(), extensionFunctionsOfThisType)

            val classVal = LLVMVariable("classvariable.this", translatorType, pointer = 0)
            variableManager.addVariable("this", classVal, 0)
            actualArgs.add(classVal)
            functionNamePrefix += translatorType.toString() + "."
        }

        if (this_type != null) {
            actualArgs.add(this_type)
        }

        actualArgs.addAll(args)

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(fullName, actualArgs, actualReturnType, external, state.arm))
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