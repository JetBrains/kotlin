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

    override var size: Int = 0
    override val structName: String
    override val type: LLVMType = LLVMReferenceType(clazz.name.toString(), "class", byRef = true)

    init {
        structName = clazz.name.toString()
        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()
        val parameterList = clazz.getPrimaryConstructorParameterList()?.parameters ?: listOf()

        annotation = descriptor.kind == ClassKind.ANNOTATION_CLASS
        indexFields(descriptor, parameterList)
        generateInnerFields(clazz.declarations)
        type.size = size
    }

    private fun indexFields(descriptor: ClassDescriptor, parameters: MutableList<KtParameter>) {
        if (annotation) {
            return
        }

        for (field in parameters) {
            val item = resolveType(field, state.bindingContext.get(BindingContext.TYPE, field.typeReference)!!)
            item.offset = fields.size

            constructorFields.add(item)
            fields.add(item)
            fieldsIndex[item.label] = item
            size += type.size
        }

        when (descriptor.kind) {
            ClassKind.ENUM_CLASS -> {
                val item = LLVMClassVariable("enum_item", LLVMEnumItemType())
                item.offset = fields.size
                fields.add(item)
                fieldsIndex["enum_item"] = item
                size += type.size
            }
        }
    }

    fun generate() {
        if (annotation) {
            return
        }

        generate(clazz.declarations)
    }

}
