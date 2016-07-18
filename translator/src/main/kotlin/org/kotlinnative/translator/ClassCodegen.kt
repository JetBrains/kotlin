package org.kotlinnative.translator

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMCharType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import java.util.*

class ClassCodegen(val state: TranslationState, val variableManager: VariableManager, val clazz: KtClass, val codeBuilder: LLVMBuilder) {

    val annotation: Boolean
    val plain: Boolean = false // TODO
    val fields = ArrayList<LLVMVariable>()
    val fieldsIndex = HashMap<String, LLVMClassVariable>()
    val type: LLVMType = LLVMReferenceType(clazz.name.toString(), "class", byRef = true)
    val size: Int
    var methods = HashMap<String, FunctionCodegen>()

    init {
        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()
        val parameterList = clazz.getPrimaryConstructorParameterList()?.parameters ?: listOf()

        var offset = 0
        var currentSize = 0
        annotation = descriptor.kind == ClassKind.ANNOTATION_CLASS

        if (!annotation) {
            for (field in parameterList) {
                val item = resolveType(field)

                fields.add(item)
                fieldsIndex[item.label] = item

                currentSize += type.size
                offset++
            }
        }

        size = currentSize
    }

    fun generate() {
        if (annotation) {
            return
        }

        generateStruct()
        generateDefaultConstructor()

        for (declaration in clazz.declarations) {
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
        val name = clazz.name!!

        codeBuilder.createClass(name, fields)
    }

    private fun generateDefaultConstructor() {
        val argFields = ArrayList<LLVMVariable>()
        val refType = type.makeClone() as LLVMReferenceType
        refType.addParam("sret")
        refType.byRef = true

        val classVal = LLVMVariable("classvariable.this", type, pointer = 1)
        variableManager.addVariable("this", classVal, 0)

        argFields.add(classVal)
        argFields.addAll(fields)

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(clazz.name!!, argFields, LLVMVoidType(), arm = state.arm))

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
            val loadVariable = LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope())
            codeBuilder.loadArgument(loadVariable)
        }
    }

    private fun generateAssignments() {
        fields.forEach {
            val argument = codeBuilder.getNewVariable(it.type)
            codeBuilder.loadVariable(argument, LLVMVariable("${it.label}.addr", it.type, scope = LLVMRegisterScope(), pointer = 1))
            val classField = codeBuilder.getNewVariable(it.type, pointer = 1)
            codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
            codeBuilder.storeVariable(classField, argument)
        }
    }

    private fun generateReturn() {
        val dst = LLVMVariable("classvariable.this", type, scope = LLVMRegisterScope(), pointer = 1)
        val src = LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1)

        val castedDst = codeBuilder.bitcast(dst, LLVMVariable("", LLVMCharType(), pointer = 1))
        val castedSrc = codeBuilder.bitcast(src, LLVMVariable("", LLVMCharType(), pointer = 1))

        codeBuilder.memcpy(castedDst, castedSrc, size)
    }

    private fun resolveType(field: KtParameter): LLVMClassVariable {
        val annotations = parseFieldAnnotations(field)

        val ktType = state.bindingContext.get(BindingContext.TYPE, field.typeReference)!!
        val result = LLVMMapStandardType(field.name!!, ktType, LLVMRegisterScope())

        if (result.type is LLVMReferenceType) {
            (result.type as LLVMReferenceType).prefix = "class"
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
