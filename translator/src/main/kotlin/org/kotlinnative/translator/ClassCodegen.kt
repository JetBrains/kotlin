package org.kotlinnative.translator

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMClassVariable
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.parseLLVMType
import java.util.*

class ClassCodegen(val state: TranslationState, val clazz: KtClass, val codeBuilder: LLVMBuilder) {

    var native = false

    fun generate() {
        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()

        if (descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            return
        }

        native = isNative(descriptor.annotations)

        generateBody()
    }

    private fun generateBody() {
        val name = clazz.name!!
        val fields = getFields()

        codeBuilder.createClass(name, fields)
    }

    private fun getFields(): List<LLVMVariable> {
        val fields = ArrayList<LLVMVariable>()
        val parameterList = clazz.getPrimaryConstructorParameterList()!!.parameters
        var offset = 0

        for (field in parameterList) {
            val type = getNativeType(field) ?: parseLLVMType((field.typeReference?.typeElement as KtUserType).referencedName!!)
            val field = LLVMClassVariable(field.name!!, type, offset)
            fields.add(field)

            offset++
        }

        return fields
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
