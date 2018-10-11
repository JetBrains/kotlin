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
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerialTypeInfo
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.KSERIALIZER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_CTOR_MARKER_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL
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
internal fun InstructionAdapter.genValidateProperty(index: Int, bitMaskPos: (Int) -> Int) {
    val addr = bitMaskPos(index)
    load(addr, OPT_MASK_TYPE)
    iconst(1 shl (index % OPT_MASK_BITS))
    and(OPT_MASK_TYPE)
    iconst(0)
}

internal fun InstructionAdapter.genExceptionThrow(exceptionClass: String, message: String) {
    anew(Type.getObjectType(exceptionClass))
    dup()
    aconst(message)
    invokespecial(exceptionClass, "<init>", "(Ljava/lang/String;)V", false)
    checkcast(Type.getObjectType("java/lang/Throwable"))
    athrow()
}

fun InstructionAdapter.genKOutputMethodCall(property: SerializableProperty, classCodegen: ImplementationBodyCodegen, expressionCodegen: ExpressionCodegen,
                                            propertyOwnerType: Type, ownerVar: Int, fromClassStartVar: Int? = null) {
    val propertyType = classCodegen.typeMapper.mapType(property.type)
    val sti = getSerialTypeInfo(property, propertyType)
    val useSerializer = if (fromClassStartVar == null) stackValueSerializerInstanceFromSerializer(classCodegen, sti)
    else stackValueSerializerInstanceFromClass(classCodegen, sti, fromClassStartVar)
    if (!sti.unit) ImplementationBodyCodegen.genPropertyOnStack(
        this,
        expressionCodegen.context,
        property.descriptor,
        propertyOwnerType,
        ownerVar,
        classCodegen.state
    )
    invokeinterface(kOutputType.internalName,
        CallingConventions.encode + sti.elementMethodPrefix + (if (useSerializer) "Serializable" else "") + CallingConventions.elementPostfix,
                  "(" + descType.descriptor + "I" +
                  (if (useSerializer) kSerialSaverType.descriptor else "") +
                  (if (sti.unit) "" else sti.type.descriptor) + ")V")
}

internal fun InstructionAdapter.buildInternalConstructorDesc(propsStartVar: Int, bitMaskBase: Int, codegen: ClassBodyCodegen, args: List<SerializableProperty>): String {
    val constructorDesc = StringBuilder("(I")
    load(bitMaskBase, OPT_MASK_TYPE)
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

internal fun ImplementationBodyCodegen.generateMethod(function: FunctionDescriptor,
                                                      block: InstructionAdapter.(JvmMethodSignature, ExpressionCodegen) -> Unit) {
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
internal val contextSerializerId = ClassId(packageFqName, Name.identifier(SpecialBuiltins.contextSerializer))


internal fun InstructionAdapter.stackValueSerializerInstanceFromClass(codegen: ClassBodyCodegen, sti: JVMSerialTypeInfo, varIndexStart: Int): Boolean {
    val serializer = sti.serializer ?: return false
    return stackValueSerializerInstance(codegen, sti.property.module, sti.property.type, serializer, this, sti.property.genericIndex) { idx ->
        load(varIndexStart + idx, kSerializerType)
    }
}

internal fun InstructionAdapter.stackValueSerializerInstanceFromSerializer(codegen: ClassBodyCodegen, sti: JVMSerialTypeInfo): Boolean {
    val serializer = sti.serializer ?: return false
    return stackValueSerializerInstance(codegen, sti.property.module, sti.property.type, serializer, this, sti.property.genericIndex) { idx ->
        load(0, kSerializerType)
        getfield(codegen.typeMapper.mapClass(codegen.descriptor).internalName, "$typeArgPrefix$idx", kSerializerType.descriptor)
    }
}

// returns false is cannot not use serializer
// use iv == null to check only (do not emit serializer onto stack)
internal fun stackValueSerializerInstance(codegen: ClassBodyCodegen, module: ModuleDescriptor, kType: KotlinType, maybeSerializer: ClassDescriptor?,
                                          iv: InstructionAdapter?, genericIndex: Int? = null, genericSerializerFieldGetter: (InstructionAdapter.(Int) -> Unit)? = null): Boolean {
    if (genericIndex != null) {
        // get field from serializer object
        iv?.run { genericSerializerFieldGetter?.invoke(this, genericIndex) }
        return true
    }
    val serializer = requireNotNull(maybeSerializer)
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
        val argSerializer = findTypeSerializerOrContext(module, argType, codegen.typeMapper.mapType(argType)) ?: return false
        // check if it can be properly serialized with its args recursively
        if (!stackValueSerializerInstance(codegen, module, argType, argSerializer, null, argType.genericIndex, genericSerializerFieldGetter))
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
        when (serializer.classId) {
            enumSerializerId, contextSerializerId -> {
                // a special way to instantiate enum -- need a enum KClass reference
                aconst(codegen.typeMapper.mapType(kType))
                AsmUtil.wrapJavaClassIntoKClass(this)
                signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
            }
            referenceArraySerializerId -> {
                // a special way to instantiate reference array serializer -- need an element KClass reference
                aconst(codegen.typeMapper.mapType(kType.arguments[0].type, null, TypeMappingMode.GENERIC_ARGUMENT))
                AsmUtil.wrapJavaClassIntoKClass(this)
                signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
            }
        }
        // all serializers get arguments with serializers of their generic types
        argSerializers.forEach { (argType, argSerializer) ->
            assert(stackValueSerializerInstance(codegen, module, argType, argSerializer, this, argType.genericIndex, genericSerializerFieldGetter))
            // wrap into nullable serializer if argType is nullable
            if (argType.isMarkedNullable) {
                invokestatic("kotlinx/serialization/internal/NullableSerializerKt", "makeNullable", // todo: extract?
                             "(" + kSerializerType.descriptor + ")" + kSerializerType.descriptor, false)

            }
            signature.append(kSerializerType.descriptor)
        }
        signature.append(")V")
        // invoke constructor
        invokespecial(serializerType.internalName, "<init>", signature.toString(), false)
    }
    return true
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

fun getSerialTypeInfo(property: SerializableProperty, type: Type): JVMSerialTypeInfo {
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
                // primitive elements are not supported yet
                }
            }
            return JVMSerialTypeInfo(property, Type.getType("Ljava/lang/Object;"),
                                     if (property.type.isMarkedNullable) "Nullable" else "", serializer)
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
                    ?: findTypeSerializerOrContext(property.module, property.type, type)
            return JVMSerialTypeInfo(property, Type.getType("Ljava/lang/Object;"),
                                     if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
        else -> throw AssertionError("Unexpected sort  for $type") // should not happen
    }
}

fun findTypeSerializerOrContext(module: ModuleDescriptor, kType: KotlinType, asmType: Type): ClassDescriptor? {
    return findTypeSerializer(module, kType)
            ?: findStandardAsmTypeSerializer(module, asmType) // otherwise see if there is a standard serializer
            ?: module.findClassAcrossModuleDependencies(contextSerializerId)
}

fun findStandardAsmTypeSerializer(module: ModuleDescriptor, asmType: Type): ClassDescriptor? {
    val name = asmType.standardSerializer ?: return null
    return module.findClassAcrossModuleDependencies(ClassId(internalPackageFqName, Name.identifier(name)))
}

internal val org.jetbrains.org.objectweb.asm.Type.standardSerializer: String? get() = when (this.descriptor) {
    "Lkotlin/Unit;" -> "UnitSerializer"
    "Z", "Ljava/lang/Boolean;" -> "BooleanSerializer"
    "B", "Ljava/lang/Byte;" -> "ByteSerializer"
    "S", "Ljava/lang/Short;" -> "ShortSerializer"
    "I", "Ljava/lang/Integer;" -> "IntSerializer"
    "J", "Ljava/lang/Long;" -> "LongSerializer"
    "F", "Ljava/lang/Float;" -> "FloatSerializer"
    "D", "Ljava/lang/Double;" -> "DoubleSerializer"
    "C", "Ljava/lang/Character;" -> "CharSerializer"
    "Ljava/lang/String;" -> "StringSerializer"
    "Ljava/util/Collection;", "Ljava/util/List;", "Ljava/util/ArrayList;" -> "ArrayListSerializer"
    "Ljava/util/Set;", "Ljava/util/LinkedHashSet;" -> "LinkedHashSetSerializer"
    "Ljava/util/HashSet;" -> "HashSetSerializer"
    "Ljava/util/Map;", "Ljava/util/LinkedHashMap;" -> "LinkedHashMapSerializer"
    "Ljava/util/HashMap;" -> "HashMapSerializer"
    "Ljava/util/Map\$Entry;" -> "MapEntrySerializer"
    "Lkotlin/Pair;" -> "PairSerializer"
    "Lkotlin/Triple;" -> "TripleSerializer"
    else -> null
}
