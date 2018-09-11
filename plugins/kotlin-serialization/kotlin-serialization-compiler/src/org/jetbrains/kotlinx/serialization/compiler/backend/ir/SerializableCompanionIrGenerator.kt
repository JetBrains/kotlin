package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCompanionCodegen
import org.jetbrains.kotlinx.serialization.compiler.resolve.classSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.getSerializableClassDescriptorByCompanion
import org.jetbrains.kotlinx.serialization.compiler.resolve.isInternalSerializable
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

class SerializableCompanionIrGenerator(
    val irClass: IrClass,
    override val compilerContext: BackendContext,
    bindingContext: BindingContext
) : SerializableCompanionCodegen(irClass.descriptor), IrBuilderExtension {
    override val translator: TypeTranslator = TypeTranslator(compilerContext.externalSymbols, compilerContext.irBuiltIns.languageVersionSettings)
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
            if (serializableClass.isInternalSerializable)
                SerializableCompanionIrGenerator(irClass, context, bindingContext).generate()
        }
    }

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) =
        irClass.contributeFunction(methodDescriptor, fromStubs = true) { _ ->
            val serializer = serializableDescriptor.classSerializer?.toClassDescriptor!!
            val expr = if (serializer.kind == ClassKind.OBJECT) {
                irGetObject(serializer)
            } else {
                val ctor = compilerContext.externalSymbols.referenceConstructor(serializer.unsubstitutedPrimaryConstructor!!)
                val args: List<IrExpression> = emptyList() // todo
                irInvoke(null, ctor, *args.toTypedArray())
            }
            +irReturn(expr)
        }
}