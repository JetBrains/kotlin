/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlinx.serialization.compiler.backend.common.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.KSERIALIZER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_CTOR_MARKER_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_FOR_ENUM
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_LOADER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_SAVER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.UNKNOWN_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.typeArgPrefix
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages.internalPackageFqName
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages.packageFqName
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

// todo: extract packages constants too?
internal val descType = Type.getObjectType("kotlinx/serialization/$SERIAL_DESCRIPTOR_CLASS")
internal val descImplType = Type.getObjectType("kotlinx/serialization/internal/$SERIAL_DESCRIPTOR_CLASS_IMPL")
internal val descriptorForEnumsType = Type.getObjectType("kotlinx/serialization/internal/$SERIAL_DESCRIPTOR_FOR_ENUM")
internal val generatedSerializerType = Type.getObjectType("kotlinx/serialization/internal/${SerialEntityNames.GENERATED_SERIALIZER_CLASS}")
internal val kOutputType = Type.getObjectType("kotlinx/serialization/$STRUCTURE_ENCODER_CLASS")
internal val encoderType = Type.getObjectType("kotlinx/serialization/$ENCODER_CLASS")
internal val decoderType = Type.getObjectType("kotlinx/serialization/$DECODER_CLASS")
internal val kInputType = Type.getObjectType("kotlinx/serialization/$STRUCTURE_DECODER_CLASS")


internal val kSerialSaverType = Type.getObjectType("kotlinx/serialization/$SERIAL_SAVER_CLASS")
internal val kSerialLoaderType = Type.getObjectType("kotlinx/serialization/$SERIAL_LOADER_CLASS")
internal val kSerializerType = Type.getObjectType("kotlinx/serialization/$KSERIALIZER_CLASS")
internal val kSerializerArrayType = Type.getObjectType("[Lkotlinx/serialization/$KSERIALIZER_CLASS;")

internal val serializationExceptionName = "kotlinx/serialization/$SERIAL_EXC"
internal val serializationExceptionMissingFieldName = "kotlinx/serialization/$MISSING_FIELD_EXC"
internal val serializationExceptionUnknownIndexName = "kotlinx/serialization/$UNKNOWN_FIELD_EXC"

val OPT_MASK_TYPE: Type = Type.INT_TYPE
val OPT_MASK_BITS = 32

// compare with zero. if result == 0, property was not seen.
internal fun InstructionAdapter.genValidateProperty(index: Int, bitMaskAddress: Int) {
    load(bitMaskAddress, OPT_MASK_TYPE)
    iconst(1 shl (index % OPT_MASK_BITS))
    and(OPT_MASK_TYPE)
    iconst(0)
}

internal fun InstructionAdapter.genMissingFieldExceptionThrow(fieldName: String) {
    anew(Type.getObjectType(serializationExceptionMissingFieldName))
    dup()
    aconst(fieldName)
    invokespecial(serializationExceptionMissingFieldName, "<init>", "(Ljava/lang/String;)V", false)
    checkcast(Type.getObjectType("java/lang/Throwable"))
    athrow()
}

fun InstructionAdapter.genKOutputMethodCall(
    property: SerializableProperty, classCodegen: ImplementationBodyCodegen, expressionCodegen: ExpressionCodegen,
    propertyOwnerType: Type, ownerVar: Int, fromClassStartVar: Int? = null,
    generator: AbstractSerialGenerator
) {
    val propertyType = classCodegen.typeMapper.mapType(property.type)
    val sti = generator.getSerialTypeInfo(property, propertyType)
    val useSerializer = if (fromClassStartVar == null) stackValueSerializerInstanceFromSerializer(classCodegen, sti, generator)
    else stackValueSerializerInstanceFromClass(classCodegen, sti, fromClassStartVar, generator)
    val actualType = if (!sti.unit) ImplementationBodyCodegen.genPropertyOnStack(
        this,
        expressionCodegen.context,
        property.descriptor,
        propertyOwnerType,
        ownerVar,
        classCodegen.state
    ) else null
    actualType?.type?.let { type -> StackValue.coerce(type, sti.type, this) }
    invokeinterface(
        kOutputType.internalName,
        CallingConventions.encode + sti.elementMethodPrefix + (if (useSerializer) "Serializable" else "") + CallingConventions.elementPostfix,
        "(" + descType.descriptor + "I" +
                (if (useSerializer) kSerialSaverType.descriptor else "") +
                (if (sti.unit) "" else sti.type.descriptor) + ")V"
    )
}

internal fun InstructionAdapter.buildInternalConstructorDesc(
    propsStartVar: Int,
    bitMaskBase: Int,
    codegen: ClassBodyCodegen,
    args: List<SerializableProperty>
): String {
    val constructorDesc = StringBuilder("(")
    repeat(args.bitMaskSlotCount()) {
        constructorDesc.append("I")
        load(bitMaskBase + it, Type.INT_TYPE)
    }
    var propVar = propsStartVar
    for (property in args) {
        val propertyType = codegen.typeMapper.mapType(property.type)
        constructorDesc.append(propertyType.descriptor)
        load(propVar, propertyType)
        propVar += propertyType.size
    }
    constructorDesc.append("Lkotlinx/serialization/$SERIAL_CTOR_MARKER_NAME;)V")
    aconst(null)
    return constructorDesc.toString()
}

internal fun ImplementationBodyCodegen.generateMethod(
    function: FunctionDescriptor,
    block: InstructionAdapter.(JvmMethodSignature, ExpressionCodegen) -> Unit
) {
    this.functionCodegen.generateMethod(OtherOrigin(this.myClass.psiOrParent, function), function,
                                        object : FunctionGenerationStrategy.CodegenBased(this.state) {
                                            override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                                codegen.v.block(signature, codegen)
                                            }
                                        })
}


internal val enumSerializerId = ClassId(internalPackageFqName, Name.identifier(SpecialBuiltins.enumSerializer))
internal val polymorphicSerializerId = ClassId(packageFqName, Name.identifier(SpecialBuiltins.polymorphicSerializer))
internal val referenceArraySerializerId = ClassId(internalPackageFqName, Name.identifier(SpecialBuiltins.referenceArraySerializer))
internal val objectSerializerId = ClassId(internalPackageFqName, Name.identifier(SpecialBuiltins.objectSerializer))
internal val sealedSerializerId = ClassId(packageFqName, Name.identifier(SpecialBuiltins.sealedSerializer))
internal val contextSerializerId = ClassId(packageFqName, Name.identifier(SpecialBuiltins.contextSerializer))


internal fun InstructionAdapter.stackValueSerializerInstanceFromClass(
    codegen: ClassBodyCodegen,
    sti: JVMSerialTypeInfo,
    varIndexStart: Int,
    serializerCodegen: AbstractSerialGenerator
): Boolean {
    val serializer = sti.serializer
    return serializerCodegen.stackValueSerializerInstance(
        codegen,
        sti.property.module,
        sti.property.type,
        serializer,
        this,
        sti.property.genericIndex
    ) { idx, _ ->
        load(varIndexStart + idx, kSerializerType)
    }
}

internal fun InstructionAdapter.stackValueSerializerInstanceFromSerializerWithoutSti(
    codegen: ClassBodyCodegen,
    property: SerializableProperty,
    serializerCodegen: AbstractSerialGenerator
): Boolean {
    val serializer =
        property.serializableWith?.toClassDescriptor
            ?: if (!property.type.isTypeParameter()) serializerCodegen.findTypeSerializerOrContext(
                property.module,
                property.type,
                property.descriptor.findPsi()
            ) else null
    return serializerCodegen.stackValueSerializerInstance(
        codegen,
        property.module,
        property.type,
        serializer,
        this,
        property.genericIndex
    ) { idx, _ ->
        load(0, kSerializerType)
        getfield(codegen.typeMapper.mapClass(codegen.descriptor).internalName, "$typeArgPrefix$idx", kSerializerType.descriptor)
    }.also { if (it && property.type.isMarkedNullable) wrapStackValueIntoNullableSerializer() }
}

internal fun InstructionAdapter.stackValueSerializerInstanceFromSerializer(codegen: ClassBodyCodegen, sti: JVMSerialTypeInfo, serializerCodegen: AbstractSerialGenerator): Boolean {
    return serializerCodegen.stackValueSerializerInstance(
        codegen, sti.property.module, sti.property.type,
        sti.serializer, this, sti.property.genericIndex
    ) { idx, _ ->
        load(0, kSerializerType)
        getfield(codegen.typeMapper.mapClass(codegen.descriptor).internalName, "$typeArgPrefix$idx", kSerializerType.descriptor)
    }
}

// returns false is cannot not use serializer
// use iv == null to check only (do not emit serializer onto stack)
internal fun AbstractSerialGenerator.stackValueSerializerInstance(codegen: ClassBodyCodegen, module: ModuleDescriptor, kType: KotlinType, maybeSerializer: ClassDescriptor?,
                                                                  iv: InstructionAdapter?,
                                                                  genericIndex: Int? = null,
                                                                  genericSerializerFieldGetter: (InstructionAdapter.(Int, KotlinType) -> Unit)? = null
): Boolean {
    if (maybeSerializer == null && genericIndex != null) {
        // get field from serializer object
        iv?.run { genericSerializerFieldGetter?.invoke(this, genericIndex, kType) }
        return true
    }
    val serializer = maybeSerializer ?: return false
    if (serializer.kind == ClassKind.OBJECT) {
        // singleton serializer -- just get it
        if (iv != null)
            StackValue.singleton(serializer, codegen.typeMapper).put(kSerializerType, iv)
        return true
    }
    // serializer is not singleton object and shall be instantiated
    val argSerializers = kType.arguments.map { projection ->
        // bail out from stackValueSerializerInstance if any type argument is not serializable
        val argType = projection.type
        val argSerializer = if (argType.isTypeParameter()) null else {
            findTypeSerializerOrContext(module, argType, sourceElement = codegen.descriptor.findPsi())
                ?: return false
        }
        // check if it can be properly serialized with its args recursively
        if (!stackValueSerializerInstance(
                codegen,
                module,
                argType,
                argSerializer,
                null,
                argType.genericIndex,
                genericSerializerFieldGetter
            )
        )
            return false
        Pair(argType, argSerializer)
    }
    // new serializer if needed
    iv?.apply {
        val serializerType = codegen.typeMapper.mapClass(serializer)
        // todo: support static factory methods for serializers for shorter bytecode
        anew(serializerType)
        dup()
        // instantiate all arg serializers on stack
        val signature = StringBuilder("(")

        fun instantiate(typeArgument: Pair<KotlinType, ClassDescriptor?>, writeSignature: Boolean = true) {
            val (argType, argSerializer) = typeArgument
            assert(
                stackValueSerializerInstance(
                    codegen,
                    module,
                    argType,
                    argSerializer,
                    this,
                    argType.genericIndex,
                    genericSerializerFieldGetter
                )
            )
            // wrap into nullable serializer if argType is nullable
            if (argType.isMarkedNullable) wrapStackValueIntoNullableSerializer()
            if (writeSignature) signature.append(kSerializerType.descriptor)
        }

        val serialName = kType.serialName()
        when (serializer.classId) {
            enumSerializerId -> {
                aconst(serialName)
                signature.append("Ljava/lang/String;")
                val enumJavaType = codegen.typeMapper.mapType(kType, null, TypeMappingMode.GENERIC_ARGUMENT)
                val javaEnumArray = Type.getType("[Ljava/lang/Enum;")
                invokestatic(enumJavaType.internalName, "values","()[${enumJavaType.descriptor}", false)
                checkcast(javaEnumArray)
                signature.append(javaEnumArray.descriptor)
            }
            contextSerializerId, polymorphicSerializerId -> {
                // a special way to instantiate enum -- need a enum KClass reference
                // GENERIC_ARGUMENT forces boxing in order to obtain KClass
                aconst(codegen.typeMapper.mapType(kType, null, TypeMappingMode.GENERIC_ARGUMENT))
                AsmUtil.wrapJavaClassIntoKClass(this)
                signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
            }
            referenceArraySerializerId -> {
                // a special way to instantiate reference array serializer -- need an element KClass reference
                aconst(codegen.typeMapper.mapType(kType.arguments[0].type, null, TypeMappingMode.GENERIC_ARGUMENT))
                AsmUtil.wrapJavaClassIntoKClass(this)
                signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
                // Reference array serializer still needs serializer for its argument type
                instantiate(argSerializers[0])
            }
            sealedSerializerId -> {
                aconst(serialName)
                signature.append("Ljava/lang/String;")
                aconst(codegen.typeMapper.mapType(kType, null, TypeMappingMode.GENERIC_ARGUMENT))
                AsmUtil.wrapJavaClassIntoKClass(this)
                signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
                val (subClasses, subSerializers) = allSealedSerializableSubclassesFor(kType.toClassDescriptor!!, module)
                // KClasses vararg
                fillArray(AsmTypes.K_CLASS_TYPE, subClasses) { _, type ->
                    aconst(codegen.typeMapper.mapType(type, null, TypeMappingMode.GENERIC_ARGUMENT))
                    AsmUtil.wrapJavaClassIntoKClass(this)
                }
                signature.append(AsmTypes.K_CLASS_ARRAY_TYPE.descriptor)
                // Serializers vararg
                fillArray(kSerializerType, subSerializers) { i, serializer ->
                    val (argType, argSerializer) = subClasses[i] to serializer
                    assert(
                        stackValueSerializerInstance(
                            codegen,
                            module,
                            argType,
                            argSerializer,
                            this,
                            argType.genericIndex
                        ) { _, genericType ->
                            // if we encountered generic type parameter in one of subclasses of sealed class, use polymorphism from upper bound
                            assert(
                                stackValueSerializerInstance(
                                    codegen,
                                    module,
                                    (genericType.constructor.declarationDescriptor as TypeParameterDescriptor).representativeUpperBound,
                                    module.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer),
                                    this
                                )
                            )
                        }
                    )
                    if (argType.isMarkedNullable) wrapStackValueIntoNullableSerializer()
                }
                signature.append(kSerializerArrayType.descriptor)
            }
            objectSerializerId -> {
                aconst(serialName)
                signature.append("Ljava/lang/String;")
                StackValue.singleton(kType.toClassDescriptor!!, codegen.typeMapper).put(Type.getType("Ljava/lang/Object;"), iv)
                signature.append("Ljava/lang/Object;")
            }
            // all serializers get arguments with serializers of their generic types
            else -> argSerializers.forEach { instantiate(it) }
        }
        signature.append(")V")
        // invoke constructor
        invokespecial(serializerType.internalName, "<init>", signature.toString(), false)
    }
    return true
}

fun InstructionAdapter.wrapStackValueIntoNullableSerializer() =
    invokestatic(
        "kotlinx/serialization/internal/NullableSerializerKt", "makeNullable",
        "(" + kSerializerType.descriptor + ")" + kSerializerType.descriptor, false
    )

fun <T> InstructionAdapter.fillArray(type: Type, args: List<T>, onEach: (Int, T) -> Unit) {
    iconst(args.size)
    newarray(type)
    args.forEachIndexed { i, serializer ->
        dup()
        iconst(i)
        onEach(i, serializer)
        astore(type)
    }
}

//
// ======= Serializers Resolving =======
//


class JVMSerialTypeInfo(
    property: SerializableProperty,
    val type: Type,
    nn: String,
    serializer: ClassDescriptor? = null,
    unit: Boolean = false
) : SerialTypeInfo(property, nn, serializer, unit)

fun AbstractSerialGenerator.getSerialTypeInfo(property: SerializableProperty, type: Type): JVMSerialTypeInfo {
    fun SerializableInfo(serializer: ClassDescriptor?) =
        JVMSerialTypeInfo(
            property,
            Type.getType("Ljava/lang/Object;"),
            if (property.type.isMarkedNullable) "Nullable" else "",
            serializer
        )

    property.serializableWith?.toClassDescriptor?.let { return SerializableInfo(it) }
    findAddOnSerializer(property.type, property.module)?.let { return SerializableInfo(it) }
    property.type.overridenSerializer?.toClassDescriptor?.let { return SerializableInfo(it) }

    if (property.type.isTypeParameter()) return JVMSerialTypeInfo(
        property,
        Type.getType("Ljava/lang/Object;"),
        if (property.type.isMarkedNullable) "Nullable" else "",
        null
    )
    when (type.sort) {
        BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR -> {
            val name = type.className
            return JVMSerialTypeInfo(property, type, Character.toUpperCase(name[0]) + name.substring(1))
        }
        ARRAY -> {
            // check for explicit serialization annotation on this property
            var serializer = property.serializableWith.toClassDescriptor
            if (serializer == null) {
                // no explicit serializer for this property. Select strategy by element type
                when (type.elementType.sort) {
                    OBJECT, ARRAY -> {
                        // reference elements
                        serializer = property.module.findClassAcrossModuleDependencies(referenceArraySerializerId)
                    }
                    else -> {
                        serializer = findTypeSerializerOrContext(
                            property.module,
                            property.type,
                            property.descriptor.findPsi()
                        )
                    }
                    // primitive elements are not supported yet
                }
            }
            return JVMSerialTypeInfo(
                property, Type.getType("Ljava/lang/Object;"),
                if (property.type.isMarkedNullable) "Nullable" else "", serializer
            )
        }
        OBJECT -> {
            // no explicit serializer for this property. Check other built in types
            if (KotlinBuiltIns.isString(property.type))
                return JVMSerialTypeInfo(property, Type.getType("Ljava/lang/String;"), "String")
            if (KotlinBuiltIns.isUnit(property.type))
                return JVMSerialTypeInfo(property, Type.getType("Lkotlin/Unit;"), "Unit", unit = true)
            // todo: more efficient enum support here, but only for enums that don't define custom serializer
            // otherwise, it is a serializer for some other type
            val serializer = property.serializableWith?.toClassDescriptor
                ?: findTypeSerializerOrContext(
                    property.module,
                    property.type,
                    property.descriptor.findPsi()
                )
            return JVMSerialTypeInfo(
                property, Type.getType("Ljava/lang/Object;"),
                if (property.type.isMarkedNullable) "Nullable" else "", serializer
            )
        }
        else -> throw AssertionError("Unexpected sort  for $type") // should not happen
    }
}

fun InstructionAdapter.stackValueDefault(type: Type) {
    when (type.sort) {
        BOOLEAN, BYTE, SHORT, CHAR, INT -> iconst(0)
        LONG -> lconst(0)
        FLOAT -> fconst(0f)
        DOUBLE -> dconst(0.0)
        else -> aconst(null)
    }
}
