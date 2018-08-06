package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCodegen
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.getClassFromSerializationPackage
import org.jetbrains.kotlinx.serialization.compiler.resolve.isInternalSerializable

class SerializableIrGenerator(
    val irClass: IrClass,
    override val compilerContext: BackendContext,
    bindingContext: BindingContext
) : SerializableCodegen(irClass.descriptor, bindingContext), IrBuilderExtension {
    override val translator: TypeTranslator = TypeTranslator(compilerContext.externalSymbols, compilerContext.irBuiltIns.languageVersionSettings)
    private val _table = SymbolTable()
    override val BackendContext.localSymbolTable: SymbolTable
        get() = _table

    override fun generateInternalConstructor(constructorDescriptor: ClassConstructorDescriptor) =
        irClass.contributeConstructor(constructorDescriptor) { ctor ->
            val original = irClass.constructors.singleOrNull { it.isPrimary } ?: throw IllegalStateException("Serializable class must have single primary constructor")
            // default arguments of original constructor
            val defaultsMap: Map<ParameterDescriptor, IrExpression?> = original.valueParameters.associate { it.descriptor to it.defaultValue?.expression }

            fun transformFieldInitializer(f: IrField): IrExpression? {
                val i = f.initializer?.expression ?: return null
                return if (i is IrGetValueImpl && i.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER) {
                    // this is a primary constructor property, use corresponding default of value parameter
                    defaultsMap.getValue(i.descriptor as ParameterDescriptor)
                } else {
                    i
                }
            }

            // Missing field exception parts
            val exceptionCtor =
                serializableDescriptor.getClassFromSerializationPackage(MISSING_FIELD_EXC)
                    .unsubstitutedPrimaryConstructor!!
            val exceptionCtorRef = compilerContext.externalSymbols.referenceConstructor(exceptionCtor)
            val exceptionType = exceptionCtorRef.owner.returnType

            val thiz = irClass.thisReceiver!!
            if (KotlinBuiltIns.isAny(irClass.descriptor.getSuperClassOrAny()))
                generateAnySuperConstructorCall(toBuilder = this@contributeConstructor)
            else
                TODO("Serializable classes with inheritance")
            val seenVar = ctor.valueParameters[0]

            for ((index, prop) in properties.serializableProperties.withIndex()) {
                val paramRef = ctor.valueParameters[index + 1]
                // assign this.a = a in else branch
                val assignParamExpr = irSetField(irGet(thiz), prop.irField, irGet(paramRef))

                val ifNotSeenExpr: IrExpression = if (prop.optional) {
                    val initializerBody =
                        requireNotNull(transformFieldInitializer(prop.irField)) { "Optional value without an initializer" } // todo: filter abstract here
                    irSetField(irGet(thiz), prop.irField, initializerBody)
                } else {
                    irThrow(irInvoke(null, exceptionCtorRef, irString(prop.name), typeHint = exceptionType))
                }

                val propNotSeenTest =
                    irEquals(
                        irInt(0),
                        irBinOp(OperatorNameConventions.AND, irGet(seenVar), irInt(1 shl (index % 32)))
                    )

                +irIfThenElse(compilerContext.irBuiltIns.unitType, propNotSeenTest, ifNotSeenExpr, assignParamExpr)
            }

            // todo: transient initializers and init blocks
            // remaining initalizers of variables
//            val serialDescs = properties.serializableProperties.map { it.descriptor }.toSet()
//            irClass.declarations.asSequence()
//                .filterIsInstance<IrProperty>()
//                .filter { it.descriptor !in serialDescs }
//                .filter { it.backingField != null }
//                .mapNotNull { prop -> prop.backingField!!.initializer?.let { prop to it.expression } }
//                .forEach { (prop, expr) -> +irSetField(irGet(thiz), prop.backingField!!, expr) }
        }

    override fun generateWriteSelfMethod(methodDescriptor: FunctionDescriptor) {
        // no-op
    }

    companion object {
        fun generate(
            irClass: IrClass,
            context: BackendContext,
            bindingContext: BindingContext
        ) {
            if (irClass.descriptor.isInternalSerializable)
                SerializableIrGenerator(irClass, context, bindingContext).generate()
        }
    }
}