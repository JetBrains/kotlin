package org.kotlinnative.translator.codegens

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.VariableManager
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMFunctionType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import java.util.*


class FunctionCodegen(state: TranslationState,
                      variableManager: VariableManager,
                      val function: KtNamedFunction,
                      codeBuilder: LLVMBuilder) :
        BlockCodegen(state, variableManager, codeBuilder) {

    var name: String
    var args = LinkedList<LLVMVariable>()
    val isExtensionDeclaration = function.isExtensionDeclaration()
    var functionNamePrefix = ""
    val external: Boolean
    val defaultValues: List<KtExpression?>
    val fullName: String
        get() = functionNamePrefix + name

    init {
        val descriptor = state.bindingContext.get(BindingContext.FUNCTION, function)!!
        args.addAll(descriptor.valueParameters.map {
            LLVMInstanceOfStandardType(it?.fqNameSafe?.asString() ?: it.name.toString(), it.type, state = state)
        })

        returnType = LLVMInstanceOfStandardType("instance", descriptor.returnType!!, state = state)
        if (returnType!!.type is LLVMReferenceType) {
            returnType!!.pointer = 2
        }
        external = descriptor.isExternal
        name = "${function.fqName}${if (!external) LLVMType.mangleFunctionArguments(args) else ""}"

        if (isExtensionDeclaration) {
            name = "${function.name}${if (!external) LLVMType.mangleFunctionArguments(args) else ""}"
            val receiverType = descriptor.extensionReceiverParameter!!.type
            val translatorType = LLVMMapStandardType(receiverType, state)
            val packageName = (function.containingFile as KtFile).packageFqName.asString()
            functionNamePrefix = packageName.addAfterIfNotEmpty(".") + translatorType.mangle + "."

            val extensionFunctionsOfThisType = state.extensionFunctions.getOrDefault(translatorType.toString(), HashMap())
            extensionFunctionsOfThisType.put(fullName, this)
            state.extensionFunctions.put(translatorType.toString(), extensionFunctionsOfThisType)
        }

        defaultValues = descriptor.valueParameters.indices.map {
            val parameterDescriptor = descriptor.valueParameters[it]
            if (parameterDescriptor.declaresDefaultValue()) {
                val initializer = (parameterDescriptor.source as KotlinSourceElement).psi as KtParameter
                initializer.defaultValue
            } else {
                null
            }
        }
    }

    fun generate(this_type: LLVMVariable? = null) {
        generateDeclaration(this_type)

        if (external) {
            return
        }

        codeBuilder.addStartExpression()
        generateLoadArguments()
        evaluateCodeBlock(function.bodyExpression, scopeDepth = topLevelScopeDepth, isBlock = function.hasBlockBody())

        if (returnType?.type is LLVMVoidType){
            codeBuilder.addAnyReturn(LLVMVoidType())
        }
        codeBuilder.addEndExpression()
    }

    private fun generateDeclaration(this_type: LLVMVariable? = null) {
        var actualReturnType: LLVMType = returnType!!.type
        val actualArgs = ArrayList<LLVMVariable>()

        if (returnType!!.pointer > 0) {
            actualReturnType = LLVMVoidType()
            actualArgs.add(returnType!!)
        }

        if (isExtensionDeclaration) {
            val receiverParameter = state.bindingContext.get(BindingContext.FUNCTION, function)!!.extensionReceiverParameter!!
            val receiverType = receiverParameter.type
            val translatorType = LLVMMapStandardType(receiverType, state)

            val classVal = when (translatorType) {
                is LLVMReferenceType -> LLVMVariable("classvariable.this", translatorType, pointer = 1)
                else -> LLVMVariable("type", translatorType, pointer = 0)
            }

            variableManager.addVariable("this", classVal, level = 0)
            actualArgs.add(classVal)
        }

        if (this_type != null) {
            actualArgs.add(this_type)
        }

        actualArgs.addAll(args)

        codeBuilder.addLLVMCodeToLocalPlace(LLVMFunctionDescriptor(fullName, actualArgs, actualReturnType, external))
    }

    private fun generateLoadArguments() {
        args.forEach(fun(it: LLVMVariable) {
            if (it.type is LLVMFunctionType || (it.type is LLVMReferenceType && it.type.byRef)) {
                variableManager.addVariable(it.label, LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope(), pointer = 1), topLevelScopeDepth)
                return
            }

            if (it.type !is LLVMReferenceType || it.type.byRef) {
                val loadVariable = LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope(), pointer = it.pointer)
                val allocVar = codeBuilder.loadArgument(loadVariable)
                variableManager.addVariable(it.label, allocVar, topLevelScopeDepth)
            } else {
                variableManager.addVariable(it.label, LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope(), pointer = 0), topLevelScopeDepth)
            }
        })
    }

}