package org.kotlinnative.translator

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMCharType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import java.util.*

abstract class StructCodegen(open val state: TranslationState, open val variableManager: VariableManager, open val classDescriptor: ClassDescriptor, open val codeBuilder: LLVMBuilder) {

    val plain: Boolean = false // TODO
    val fields = ArrayList<LLVMVariable>()
    val fieldsIndex = HashMap<String, LLVMClassVariable>()
    abstract val type: LLVMType
    abstract val size: Int
    var methods = HashMap<String, FunctionCodegen>()
    abstract val structName: String


    fun generate(declarationList: List<KtDeclaration>) {
        generateStruct()
        generatePrimaryConstructor()

        for (declaration in declarationList) {
            when (declaration) {
                is KtNamedFunction -> {
                    val function = FunctionCodegen(state, variableManager, declaration, codeBuilder)
                    methods.put(function.name, function)
                }
            }
        }
        val classVal = LLVMVariable("classvariable.this", type, pointer = 1)
        variableManager.addVariable("this", classVal, 0)
        for (function in methods.values) {
            function.generate(classVal)
        }
    }

    private fun generateStruct() {
        val name = classDescriptor.name.identifier

        codeBuilder.createClass(name, fields)
    }

    private fun generatePrimaryConstructor() {
        val argFields = ArrayList<LLVMVariable>()
        val refType = type.makeClone() as LLVMReferenceType
        refType.addParam("sret")
        refType.byRef = true

        val classVal = LLVMVariable("classvariable.this", type, pointer = 1)
        variableManager.addVariable("this", classVal, 0)

        argFields.add(classVal)
        argFields.addAll(fields)

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(classDescriptor.name.identifier, argFields, LLVMVoidType(), arm = state.arm))

        codeBuilder.addStartExpression()
        generateLoadArguments(classVal)
        generateAssignments()
        generateReturn()
        codeBuilder.addAnyReturn(LLVMVoidType())
        codeBuilder.addEndExpression()
    }

    private fun generateLoadArguments(thisField: LLVMVariable) {

        val thisVariable = LLVMVariable(thisField.label, thisField.type, thisField.label, LLVMRegisterScope(), pointer = 0)
        codeBuilder.loadArgument(thisVariable, false)

        fields.forEach {
            if (it.type !is LLVMReferenceType) {
                val loadVariable = LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope())
                codeBuilder.loadArgument(loadVariable)
            }
        }
    }

    private fun generateAssignments() {
        fields.forEach {
            when (it.type) {
                is LLVMReferenceType -> {
                    val classField = codeBuilder.getNewVariable(it.type, pointer = it.pointer + 1)
                    codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
                    codeBuilder.storeVariable(classField, it)
                }
                else -> {
                    val argument = codeBuilder.getNewVariable(it.type, it.pointer)
                    codeBuilder.loadVariable(argument, LLVMVariable("${it.label}.addr", it.type, scope = LLVMRegisterScope(), pointer = it.pointer + 1))
                    val classField = codeBuilder.getNewVariable(it.type, pointer = 1)
                    codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
                    codeBuilder.storeVariable(classField, argument)
                }
            }

        }
    }

    private fun generateReturn() {
        val dst = LLVMVariable("classvariable.this", type, scope = LLVMRegisterScope(), pointer = 1)
        val src = LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1)

        val castedDst = codeBuilder.bitcast(dst, LLVMVariable("", LLVMCharType(), pointer = 1))
        val castedSrc = codeBuilder.bitcast(src, LLVMVariable("", LLVMCharType(), pointer = 1))

        codeBuilder.memcpy(castedDst, castedSrc, size)
    }

    protected fun resolveType(field: KtParameter): LLVMClassVariable {
        val annotations = parseFieldAnnotations(field)

        val ktType = state.bindingContext.get(BindingContext.TYPE, field.typeReference)!!
        val result = LLVMMapStandardType(field.name!!, ktType, LLVMRegisterScope())

        if (result.type is LLVMReferenceType) {
            val type = result.type as LLVMReferenceType
            type.prefix = "class"
            type.byRef = true
        }

        if (annotations.contains("Plain")) {
            result.pointer = 0
        }

        return LLVMClassVariable(result.label, result.type, result.pointer)
    }

    private fun parseFieldAnnotations(field: KtParameter): Set<String> {
        val result = HashSet<String>()

        for (annotation in field.annotationEntries) {
            val annotationDescriptor = state.bindingContext.get(BindingContext.ANNOTATION, annotation)
            val type = annotationDescriptor?.type.toString()

            result.add(type)
        }

        return result
    }
}
