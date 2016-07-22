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

class ClassCodegen(override val state: TranslationState,
                   override val variableManager: VariableManager,
                   val clazz: KtClass,
                   override val codeBuilder: LLVMBuilder,
                   parentCodegen: StructCodegen? = null) :

        StructCodegen(state, variableManager, clazz, state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException(), codeBuilder, parentCodegen) {

    val annotation: Boolean

    override var size: Int = 0
    override val structName: String = clazz.name!!
    override val type: LLVMReferenceType

    init {
        type = LLVMReferenceType(structName, "class", byRef = true)
        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()
        val parameterList = clazz.getPrimaryConstructorParameterList()?.parameters ?: listOf()

        annotation = descriptor.kind == ClassKind.ANNOTATION_CLASS
        indexFields(descriptor, parameterList)
        generateInnerFields(clazz.declarations)

        if (parentCodegen != null) {
            type.location.addAll(parentCodegen.type.location)
            type.location.add(parentCodegen.structName)
        }

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
            else -> {
            }
        }
    }

    fun generate() {
        if (annotation) {
            return
        }

        generate(clazz.declarations)
        nestedClasses.forEach { x, classCodegen -> classCodegen.generate() }

        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()
        val companionObjectDescriptor = descriptor.companionObjectDescriptor
        if (companionObjectDescriptor != null) {
            val companionObject = clazz.getCompanionObjects().first()
            val property = ObjectCodegen(state, variableManager, companionObject, codeBuilder, this)
            val companionObjectName = structName + "." + companionObject.name
            property.generate()

            for ((key, value) in property.methods) {
                val methodName = key.removePrefix(companionObjectName + ".")
                companionMethods.put(structName + "." + methodName, value)
            }
            companionFields.addAll(property.fields)
            for (field in property.fields) {
                companionFieldsSource.put(field.label, property)
            }
            companionFieldsIndex.putAll(property.fieldsIndex)
        }
    }

}
