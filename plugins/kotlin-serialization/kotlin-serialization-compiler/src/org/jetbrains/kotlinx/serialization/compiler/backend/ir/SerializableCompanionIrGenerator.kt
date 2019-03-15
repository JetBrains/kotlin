package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCompanionCodegen
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

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
            if (serializableClass.shouldHaveGeneratedMethodsInCompanion)
                SerializableCompanionIrGenerator(irClass, context, bindingContext).generate()
        }
    }

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) =
        irClass.contributeFunction(methodDescriptor, fromStubs = true) { getter ->
            val serializer = serializableDescriptor.classSerializer?.toClassDescriptor!!
            val expr = if (serializer.kind == ClassKind.OBJECT) {
                irGetObject(serializer)
            } else {
                val desc = requireNotNull(
                    KSerializerDescriptorResolver.findSerializerConstructorForTypeArgumentsSerializers(serializer)
                ) { "Generated serializer does not have constructor with required number of arguments" }
                val ctor = compilerContext.externalSymbols.referenceConstructor(desc)
                val args: List<IrExpression> = getter.valueParameters.map { irGet(it) }
                irInvoke(null, ctor, *args.toTypedArray())
            }
            +irReturn(expr)
        }
}