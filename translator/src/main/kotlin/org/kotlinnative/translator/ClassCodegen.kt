package org.kotlinnative.translator

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMClassVariable
import org.kotlinnative.translator.llvm.types.LLVMEnumItemType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType

class ClassCodegen(override val state: TranslationState, override val variableManager: VariableManager, val clazz: KtClass, override val codeBuilder: LLVMBuilder) :
        StructCodegen(state, variableManager, clazz, state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException(), codeBuilder) {

    val annotation: Boolean

    override val size: Int
    override val structName: String
    override val type: LLVMType = LLVMReferenceType(clazz.name.toString(), "class", byRef = true)

    init {
        structName = clazz.name.toString()
        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()
        val parameterList = clazz.getPrimaryConstructorParameterList()?.parameters ?: listOf()

        annotation = descriptor.kind == ClassKind.ANNOTATION_CLASS
        size = indexFields(descriptor, parameterList)
    }

    private fun indexFields(descriptor: ClassDescriptor, parameters: MutableList<KtParameter>): Int {
        if (annotation) {
            return 0
        }

        var offset = 0
        var currentSize = 0

        for (field in parameters) {
            val item = resolveType(field)
            item.offset = offset

            fields.add(item)
            fieldsIndex[item.label] = item

            currentSize += type.size
            offset++
        }

        when (descriptor.kind) {
            ClassKind.ENUM_CLASS -> {
                val item = LLVMClassVariable("enum_item", LLVMEnumItemType())
                item.offset = offset
                fields.add(item)
                fieldsIndex["enum_item"] = item
                currentSize += type.size
                offset++
            }
        }

        return currentSize
    }

    fun generate() {
        if (annotation) {
            return
        }
        generate(clazz.declarations)
    }

    private fun generateStruct() {
        val name = clazz.name!!

        codeBuilder.createClass(name, fields)
    }
}
