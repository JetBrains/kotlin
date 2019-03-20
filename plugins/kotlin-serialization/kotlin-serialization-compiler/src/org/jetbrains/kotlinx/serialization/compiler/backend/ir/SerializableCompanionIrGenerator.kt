package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCompanionCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.getSerializableClassDescriptorByCompanion
import org.jetbrains.kotlinx.serialization.compiler.resolve.shouldHaveGeneratedMethodsInCompanion

class SerializableCompanionIrGenerator(
    val irClass: IrClass,
    override val compilerContext: BackendContext,
    bindingContext: BindingContext
) : SerializableCompanionCodegen(irClass.descriptor, bindingContext), IrBuilderExtension {
    override val translator: TypeTranslator = compilerContext.createTypeTranslator(serializableDescriptor.module)
    private val _table = SymbolTable()
    override val BackendContext.localSymbolTable: SymbolTable
        get() = _table

    companion object {
        fun generate(
            irClass: IrClass,
            context: BackendContext,
            bindingContext: BindingContext
        ) {
            val companionDescriptor = irClass.descriptor
            val serializableClass = getSerializableClassDescriptorByCompanion(companionDescriptor) ?: return
            if (serializableClass.shouldHaveGeneratedMethodsInCompanion) {
                SerializableCompanionIrGenerator(irClass, context, bindingContext).generate()
                irClass.patchDeclarationParents(irClass.parent)
            }
        }
    }

    private fun IrBuilderWithScope.patchSerializableClassWithMarkerAnnotation(serializer: ClassDescriptor) {
        if (serializer.kind != ClassKind.OBJECT) {
            return
        }

        val annotationMarkerClass = serializer.module.findClassAcrossModuleDependencies(
            ClassId(
                SerializationPackages.packageFqName,
                Name.identifier(SerialEntityNames.ANNOTATION_MARKER_CLASS)
            )
        ) ?: return
        val annotationCtor = requireNotNull(annotationMarkerClass.unsubstitutedPrimaryConstructor?.let {
            compilerContext.externalSymbols.referenceConstructor(it)
        })

        val annotationType = compilerContext.externalSymbols.referenceClass(annotationMarkerClass).owner.defaultType
        val irSerializableClass = compilerContext.externalSymbols.referenceClass(serializableDescriptor).owner
        val annotationCtorCall = IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, annotationType, annotationCtor).apply {
            val serializerType = serializer.toSimpleType(false)
            putValueArgument(
                0,
                createClassReference(
                    serializerType,
                    startOffset,
                    endOffset
                )
            )
        }

        irSerializableClass.annotations.add(annotationCtorCall)
    }

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) =
        irClass.contributeFunction(methodDescriptor, fromStubs = true) { getter ->
            val serializer = requireNotNull(
                findTypeSerializer(
                    serializableDescriptor.module,
                    serializableDescriptor.toSimpleType()
                )
            )
            val args: List<IrExpression> = getter.valueParameters.map { irGet(it) }
            val expr = serializerInstance(
                this@SerializableCompanionIrGenerator,
                serializableDescriptor, serializer,
                serializableDescriptor.module,
                serializableDescriptor.defaultType
            ) { args[it] }
            patchSerializableClassWithMarkerAnnotation(serializer)
            +irReturn(requireNotNull(expr))
        }

}