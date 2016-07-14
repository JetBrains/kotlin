package org.kotlinnative.translator

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.*
import java.util.*

class ClassCodegen(val state: TranslationState, val clazz: KtClass, val codeBuilder: LLVMBuilder) {

    val annotation: Boolean
    val native: Boolean
    val fields = ArrayList<LLVMClassVariable>()
    val fieldsIndex = HashMap<String, LLVMClassVariable>()
    val name = "%class.${clazz.name}"
    val constructorName = "@${clazz.name}"
    val type: LLVMType = LLVMReferenceType(clazz.name.toString(), "class")
    val size: Int

    init {
        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()
        val parameterList = clazz.getPrimaryConstructorParameterList()!!.parameters

        var offset = 0
        var currentSize = 0
        annotation = descriptor.kind == ClassKind.ANNOTATION_CLASS

        if (!annotation) {
            for (field in parameterList) {
                val type = getNativeType(field) ?: parseLLVMType((field.typeReference?.typeElement as KtUserType).referencedName!!)
                val item = LLVMClassVariable(field.name!!, type, offset)

                fields.add(item)
                fieldsIndex[item.label] = item

                currentSize += type.size
                offset++
            }
        }

        native = isNative(descriptor.annotations)
        size = currentSize
    }

    fun generate() {
        if (annotation) {
            return
        }

        generateStruct()
        generateDefaultConstructor()
    }

    private fun generateStruct() {
        val name = clazz.name!!

        codeBuilder.createClass(name, fields)
    }

    private fun generateDefaultConstructor() {
        val argFields = ArrayList<LLVMVariable>()
        val refType = type.makeClone() as LLVMReferenceType
        refType.addParam("sret")

        val thisField = LLVMVariable("instance", refType, clazz.name, pointer = true)

        argFields.add(thisField)
        argFields.addAll(fields)

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(clazz.name!!, argFields, LLVMVoidType(), arm = state.arm))

        codeBuilder.addStartExpression()
        generateLoadArguments(thisField)
        generateAssignments()
        generateReturn()
        codeBuilder.addVoidReturn()
        codeBuilder.addEndExpression()
    }

    private fun generateLoadArguments(thisField: LLVMVariable) {

        val thisVariable = LLVMVariable(thisField.label, thisField.type, thisField.label, LLVMLocalScope(), pointer = true)
        codeBuilder.loadArgument(thisVariable, false)

        fields.forEach {
            val loadVariable = LLVMVariable(it.label, it.type, it.label, LLVMLocalScope())
            codeBuilder.loadArgument(loadVariable)
        }
    }

    private fun generateAssignments() {
        fields.forEach {
            val argument = codeBuilder.getNewVariable(it.type)
            codeBuilder.loadVariable(argument, LLVMVariable("${it.label}.addr", it.type, scope = LLVMLocalScope(), pointer = true))
            val classField = codeBuilder.getNewVariable(it.type, true)
            codeBuilder.loadClassField(classField, LLVMVariable("instance.addr", type, scope = LLVMLocalScope(), pointer = true), it.offset)
            codeBuilder.storeVariable(classField, argument)
        }
    }

    private fun generateReturn() {
        val dst = LLVMVariable("instance", type, scope = LLVMLocalScope(), pointer = true)
        val src = LLVMVariable("instance.addr", type, scope = LLVMLocalScope(), pointer = true)

        val castedDst = codeBuilder.bitcast(dst, LLVMCharType())
        val castedSrc = codeBuilder.bitcast(src, LLVMCharType())

        codeBuilder.memcpy(castedDst, castedSrc, size)
    }

    private fun getNativeType(field: KtParameter): LLVMType? {
        for (annotation in field.annotationEntries) {
            val annotationDescriptor = state.bindingContext.get(BindingContext.ANNOTATION, annotation)
            val type = annotationDescriptor?.type.toString()
            if (type == "Native") {
                return parseLLVMType(annotationDescriptor!!.argumentValue("type").toString())
            }
        }

        return null
    }

    private fun isNative(annotations: Annotations?): Boolean {
        annotations ?: return false

        for (i in annotations) {
            if (i.type.toString() == "Native") {
                return true
            }
        }

        return false
    }

}
