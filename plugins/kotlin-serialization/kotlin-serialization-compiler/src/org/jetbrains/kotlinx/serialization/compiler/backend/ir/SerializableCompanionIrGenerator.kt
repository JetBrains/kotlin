/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCompanionCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializer
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class SerializableCompanionIrGenerator(
    val irClass: IrClass,
    override val compilerContext: SerializationPluginContext,
    bindingContext: BindingContext
) : SerializableCompanionCodegen(irClass.descriptor, bindingContext), IrBuilderExtension {

    private val lazyDescriptor = irClass.module.findClassAcrossModuleDependencies(
        ClassId(FqName("kotlin"), Name.identifier("Lazy"))
    )!!

    private val lazySafeModeClassDescriptor = irClass.module.findClassAcrossModuleDependencies(
        ClassId(FqName("kotlin"), Name.identifier("LazyThreadSafetyMode"))
    )!!

    private val lazyFunctionSymbol = compilerContext.referenceFunctions(FqName("kotlin").child(Name.identifier("lazy"))).single {
        it.descriptor.valueParameters.size == 2 && it.descriptor.valueParameters[0].type == lazySafeModeClassDescriptor.defaultType
    }

    private val publicationEntryDescriptor = lazySafeModeClassDescriptor.enumEntries().single { it.name == Name.identifier("PUBLICATION") }

    private val function0Descriptor = irClass.module.findClassAcrossModuleDependencies(
        ClassId(FqName("kotlin"), Name.identifier("Function0"))
    )!!


    companion object {
        fun generate(
            irClass: IrClass,
            context: SerializationPluginContext,
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

        val irSerializableClass = compilerContext.referenceClass(serializableDescriptor.fqNameSafe)?.owner ?: return
        val serializableWithAlreadyPresent = irSerializableClass.annotations.any {
            it.symbol.descriptor.constructedClass.fqNameSafe == annotationMarkerClass.fqNameSafe
        }
        if (serializableWithAlreadyPresent) return

        val annotationCtor = compilerContext.referenceConstructors(annotationMarkerClass.fqNameSafe).single { it.owner.isPrimary }
        val annotationType = annotationMarkerClass.defaultType.toIrType()
        val annotationCtorCall = IrConstructorCallImpl.fromSymbolDescriptor(startOffset, endOffset, annotationType, annotationCtor).apply {
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

        irSerializableClass.annotations += annotationCtorCall
    }

    override fun generateLazySerializerGetter(methodDescriptor: FunctionDescriptor) {
        val serializerDescriptor = requireNotNull(
            findTypeSerializer(
                serializableDescriptor.module,
                serializableDescriptor.toSimpleType()
            )
        )

        val field = irClass.addField {
            name = Name.identifier(SerialEntityNames.SERIALIZER_LAZY_DELEGATE_FIELD_NAME)
            visibility = DescriptorVisibilities.PRIVATE
            origin = SERIALIZABLE_PLUGIN_ORIGIN
            isFinal = true
            isStatic = true
            type = lazyDescriptor.defaultType.toIrType()
        }.apply {
            val lambda = irClass.createSingletonLambda(serializerDescriptor.defaultType.toIrType(), startOffset, endOffset) {
                val expr = serializerInstance(
                    this@SerializableCompanionIrGenerator,
                    serializerDescriptor, serializableDescriptor.module,
                    serializableDescriptor.defaultType
                )
                patchSerializableClassWithMarkerAnnotation(serializerDescriptor)
                +irReturn(requireNotNull(expr))
            }

            val call = IrCallImpl(startOffset, endOffset, type, lazyFunctionSymbol, 0, 2)
            call.putValueArgument(
                0,
                IrGetEnumValueImpl(
                    startOffset,
                    endOffset,
                    publicationEntryDescriptor.classValueType!!.toIrType(),
                    compilerContext.symbolTable.referenceEnumEntry(publicationEntryDescriptor)
                ),
            )

            call.putValueArgument(
                1,
                IrFunctionExpressionImpl(
                    startOffset,
                    endOffset,
                    function0Descriptor.defaultType.toIrType(),
                    lambda,
                    IrStatementOrigin.LAMBDA
                ),
            )

            initializer = irClass.factory.createExpressionBody(startOffset, endOffset, call)
        }

        val valueGetter = compilerContext.referenceClass(lazyDescriptor.fqNameSafe)!!.getPropertyGetter("value")!!

        irClass.contributeFunction(methodDescriptor) {
            +irReturn(
                irGet(
                    serializerDescriptor.defaultType.toIrType(),
                    irGetField(null, field), valueGetter
                )
            )
        }
        generateSerializerFactoryIfNeeded(methodDescriptor)
    }

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) {
        irClass.contributeFunction(methodDescriptor) { getter ->
            val serializer = requireNotNull(
                findTypeSerializer(
                    serializableDescriptor.module,
                    serializableDescriptor.toSimpleType()
                )
            )
            val args: List<IrExpression> = getter.valueParameters.map { irGet(it) }
            val expr = serializerInstance(
                this@SerializableCompanionIrGenerator,
                serializer, serializableDescriptor.module,
                serializableDescriptor.defaultType
            ) { it, _ -> args[it] }
            patchSerializableClassWithMarkerAnnotation(serializer)
            +irReturn(requireNotNull(expr))
        }
        generateSerializerFactoryIfNeeded(methodDescriptor)
    }

    private fun generateSerializerFactoryIfNeeded(getterDescriptor: FunctionDescriptor) {
        if (!companionDescriptor.needSerializerFactory()) return
        val serialFactoryDescriptor = companionDescriptor.unsubstitutedMemberScope.getContributedFunctions(
            SerialEntityNames.SERIALIZER_PROVIDER_NAME,
            NoLookupLocation.FROM_BACKEND
        ).firstOrNull {
            it.valueParameters.size == 1
                    && it.valueParameters.first().isVararg
                    && it.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
                    && it.returnType != null && isKSerializer(it.returnType)
        } ?: return
        irClass.contributeFunction(serialFactoryDescriptor) { factory ->
            val kSerializerStarType = factory.returnType
            val array = factory.valueParameters.first()
            val argsSize = serializableDescriptor.declaredTypeParameters.size
            val arrayGet = compilerContext.irBuiltIns.arrayClass.owner.declarations.filterIsInstance<IrSimpleFunction>()
                .single { it.name.asString() == "get" }

            val serializers: List<IrExpression> = (0 until argsSize).map {
                irInvoke(irGet(array), arrayGet.symbol, irInt(it), typeHint = kSerializerStarType)
            }
            val serializerCall = compilerContext.symbolTable.referenceSimpleFunction(getterDescriptor)
            val call = irInvoke(
                IrGetValueImpl(startOffset, endOffset, factory.dispatchReceiverParameter!!.symbol),
                serializerCall,
                List(argsSize) { compilerContext.irBuiltIns.anyNType },
                serializers,
                returnTypeHint = kSerializerStarType
            )
            +irReturn(call)
            patchSerializableClassWithMarkerAnnotation(companionDescriptor)
        }
    }

}
