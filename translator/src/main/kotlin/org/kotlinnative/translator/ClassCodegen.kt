package org.kotlinnative.translator

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import java.util.*

class ClassCodegen(state: TranslationState,
                   variableManager: VariableManager,
                   val clazz: KtClass,
                   codeBuilder: LLVMBuilder,
                   parentCodegen: StructCodegen? = null) :
        StructCodegen(state, variableManager, clazz, codeBuilder, parentCodegen) {

    val annotation: Boolean
    val enum: Boolean
    var companionObjectCodegen: ObjectCodegen? = null
    val descriptor: ClassDescriptor

    override var size: Int = 0
    override val structName: String = clazz.fqName?.asString()!!
    override val type: LLVMReferenceType

    init {
        type = LLVMReferenceType(structName, "class", align = TranslationState.pointerAlign, size = TranslationState.pointerSize, byRef = true)
        if (parentCodegen != null) {
            type.location.addAll(parentCodegen.type.location)
            type.location.add(parentCodegen.structName)
        }

        descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException("Can't receive descriptor of class " + clazz.name)

        annotation = descriptor.kind == ClassKind.ANNOTATION_CLASS
        enum = descriptor.kind == ClassKind.ENUM_CLASS

        type.align = TranslationState.pointerAlign
    }

    private fun indexFields(parameters: MutableList<KtParameter>) {
        if (annotation) {
            return
        }
        val currentConstructorFields = ArrayList<LLVMVariable>()
        for (field in parameters) {
            val item = resolveType(field, state.bindingContext.get(BindingContext.TYPE, field.typeReference)!!)
            item.offset = fields.size

            currentConstructorFields.add(item)
            fields.add(item)
            fieldsIndex[item.label] = item
        }
        primaryConstructorIndex = LLVMType.mangleFunctionArguments(currentConstructorFields)
        constructorFields.put(primaryConstructorIndex!!, currentConstructorFields)
    }

    override fun prepareForGenerate() {
        val parameterList = clazz.getPrimaryConstructorParameterList()?.parameters ?: listOf()
        indexFields(parameterList)
        generateInnerFields(clazz.declarations)

        calculateTypeSize()
        type.size = size

        if (annotation) {
            return
        }

        super.prepareForGenerate()
        nestedClasses.forEach { x, classCodegen -> classCodegen.prepareForGenerate() }

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
