package org.kotlinnative.translator

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.types.LLVMReferenceType

class ClassCodegen(state: TranslationState,
                   variableManager: VariableManager,
                   val clazz: KtClass,
                   codeBuilder: LLVMBuilder,
                   parentCodegen: StructCodegen? = null) :

        StructCodegen(state, variableManager, clazz, state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException(), codeBuilder, parentCodegen) {

    val annotation: Boolean
    val enum: Boolean
    var companionObjectCodegen: ObjectCodegen? = null

    override var size: Int = 0
    override val structName: String = clazz.name!!
    override val type: LLVMReferenceType

    init {
        type = LLVMReferenceType(structName, "class", align = state.pointerAlign, size = state.pointerSize, byRef = true)
        if (parentCodegen != null) {
            type.location.addAll(parentCodegen.type.location)
            type.location.add(parentCodegen.structName)
        }

        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()
        val parameterList = clazz.getPrimaryConstructorParameterList()?.parameters ?: listOf()

        annotation = descriptor.kind == ClassKind.ANNOTATION_CLASS
        enum = descriptor.kind == ClassKind.ENUM_CLASS

        indexFields(parameterList)
        generateInnerFields(clazz.declarations)

        calculateTypeSize()
        type.size = size
        type.align = state.pointerAlign
    }

    private fun indexFields(parameters: MutableList<KtParameter>) {
        if (annotation) {
            return
        }

        for (field in parameters) {
            val item = resolveType(field, state.bindingContext.get(BindingContext.TYPE, field.typeReference)!!)
            item.offset = fields.size

            constructorFields.add(item)
            fields.add(item)
            fieldsIndex[item.label] = item
        }
    }

    override fun prepareForGenerate() {
        if (annotation) {
            return
        }
        super.prepareForGenerate()
        nestedClasses.forEach { x, classCodegen -> classCodegen.prepareForGenerate() }

        val descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException()
        val companionObjectDescriptor = descriptor.companionObjectDescriptor
        if (companionObjectDescriptor != null) {
            val companionObject = clazz.getCompanionObjects().first()
            companionObjectCodegen = ObjectCodegen(state, variableManager, companionObject, codeBuilder, this)
            companionObjectCodegen!!.prepareForGenerate()
        }
    }

    override fun generate() {
        if (annotation) {
            return
        }

        super.generate()
        nestedClasses.forEach { x, classCodegen -> classCodegen.generate() }

        if (companionObjectCodegen != null) {
            companionObjectCodegen!!.generate()
        }
    }

}
