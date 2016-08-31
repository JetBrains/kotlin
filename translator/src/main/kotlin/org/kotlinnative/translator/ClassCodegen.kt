package org.kotlinnative.translator

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType

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
        type = LLVMReferenceType(structName, "class", align = TranslationState.POINTER_ALIGN, size = TranslationState.POINTER_SIZE, byRef = true)
        descriptor = state.bindingContext.get(BindingContext.CLASS, clazz) ?: throw TranslationException("Can't receive descriptor of class " + clazz.name)

        annotation = descriptor.kind == ClassKind.ANNOTATION_CLASS
        enum = descriptor.kind == ClassKind.ENUM_CLASS

        type.align = TranslationState.POINTER_ALIGN
    }

    override fun prepareForGenerate() {
        val parameterList = clazz.getPrimaryConstructorParameterList()?.parameters ?: listOf()
        indexFields(parameterList)
        generateInnerFields(clazz.declarations)

        type.size = calculateTypeSize()

        if (annotation) {
            return
        }

        super.prepareForGenerate()
        nestedClasses.forEach { x, classCodegen -> classCodegen.prepareForGenerate() }

        if (descriptor.companionObjectDescriptor != null) {
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
        companionObjectCodegen?.generate()
    }

    private fun indexFields(parameters: MutableList<KtParameter>) {
        if (annotation) {
            return
        }

        val currentConstructorFields = parameters.mapIndexed { i, it -> resolveType(it, state.bindingContext.get(BindingContext.TYPE, it.typeReference)!!, fields.size + i) }
        fields.addAll(currentConstructorFields)
        fieldsIndex.putAll(currentConstructorFields.map { Pair(it.label, it) })
        primaryConstructorIndex = LLVMType.mangleFunctionArguments(currentConstructorFields)
        constructorFields.put(primaryConstructorIndex!!, currentConstructorFields)
    }

}