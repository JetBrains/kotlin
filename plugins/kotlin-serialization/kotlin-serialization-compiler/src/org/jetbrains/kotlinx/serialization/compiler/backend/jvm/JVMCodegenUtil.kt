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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsTypeProjectionsInTopLevelArguments
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializableProperty
import org.jetbrains.kotlinx.serialization.compiler.resolve.isInternalSerializable
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor
import org.jetbrains.kotlinx.serialization.compiler.resolve.typeSerializer
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 *  @author Leonid Startsev
 *          sandwwraith@gmail.com
 */

internal val internalPackageFqName = FqName("kotlinx.serialization.internal")
internal val descType = Type.getObjectType("kotlinx/serialization/KSerialClassDesc")
internal val descImplType = Type.getObjectType("kotlinx/serialization/internal/SerialClassDescImpl")
internal val kOutputType = Type.getObjectType("kotlinx/serialization/KOutput")
internal val kInputType = Type.getObjectType("kotlinx/serialization/KInput")


internal val kSerialSaverType = Type.getObjectType("kotlinx/serialization/KSerialSaver")
internal val kSerialLoaderType = Type.getObjectType("kotlinx/serialization/KSerialLoader")
internal val kSerializerType = Type.getObjectType("kotlinx/serialization/KSerializer")
internal val kSerializerArrayType = Type.getObjectType("[Lkotlinx/serialization/KSerializer;")

internal val serializationExceptionName = "kotlinx/serialization/SerializationException"
internal val serializationExceptionMissingFieldName = "kotlinx/serialization/MissingFieldException"

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

fun InstructionAdapter.genKOutputMethodCall(property: SerializableProperty, classCodegen: ImplementationBodyCodegen, expressionCodegen: ExpressionCodegen, propertyOwnerType: Type, ownerVar: Int) {
    val propertyType = classCodegen.typeMapper.mapType(property.type)
    val sti = getSerialTypeInfo(property, propertyType)
    val useSerializer = stackValueSerializerInstance(classCodegen, sti)
    if (!sti.unit) classCodegen.genPropertyOnStack(this, expressionCodegen.context, property.descriptor, propertyOwnerType, ownerVar)
    invokevirtual(kOutputType.internalName,
                  "write" + sti.nn + (if (useSerializer) "Serializable" else "") + "ElementValue",
                  "(" + descType.descriptor + "I" +
                  (if (useSerializer) kSerialSaverType.descriptor else "") +
                  (if (sti.unit) "" else sti.type.descriptor) + ")V", false)
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
    constructorDesc.append("Lkotlinx/serialization/SerializationConstructorMarker;)V")
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


internal val enumSerializerId = ClassId(internalPackageFqName, Name.identifier("EnumSerializer"))
internal val polymorphicSerializerId = ClassId(internalPackageFqName, Name.identifier("PolymorphicSerializer"))
internal val referenceArraySerializerId = ClassId(internalPackageFqName, Name.identifier("ReferenceArraySerializer"))

// returns false is property should not use serializer
internal fun InstructionAdapter.stackValueSerializerInstance(codegen: ClassBodyCodegen, sti: SerialTypeInfo): Boolean {
    val serializer = sti.serializer ?: return false
    return stackValueSerializerInstance(codegen, sti.property.module, sti.property.type, serializer, this)
}

// returns false is cannot not use serializer
// use iv == null to check only (do not emit serializer onto stack)
internal fun stackValueSerializerInstance(codegen: ClassBodyCodegen, module: ModuleDescriptor, kType: KotlinType, serializer: ClassDescriptor,
                                          iv: InstructionAdapter?): Boolean {
    if (serializer.kind == ClassKind.OBJECT) {
        // singleton serializer -- just get it
        if (iv != null)
            StackValue.singleton(serializer, codegen.typeMapper).put(kSerializerType, iv)
        return true
    }
    // serializer is not singleton object and shall be instantiated
    val argSerializers = kType.arguments.map { projection ->
        // bail out from stackValueSerializerInstance if any type argument is not serializable
        val argSerializer = findTypeSerializer(module, projection.type, codegen.typeMapper.mapType(projection.type)) ?: return false
        // check if it can be properly serialized with its args recursively
        if (!stackValueSerializerInstance(codegen, module, projection.type, argSerializer, null))
            return false
        Pair(projection.type, argSerializer)
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
            enumSerializerId -> {
                // a special way to instantiate enum -- need a enum KClass reference
                aconst(codegen.typeMapper.mapType(kType))
                AsmUtil.wrapJavaClassIntoKClass(this)
                signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
            }
            referenceArraySerializerId -> {
                // a special way to instantiate reference array serializer -- need an element java.lang.Class reference
                aconst(codegen.typeMapper.mapType(kType.arguments[0].type, null, TypeMappingMode.GENERIC_ARGUMENT))
                signature.append("Ljava/lang/Class;")
            }
        }
        // all serializers get arguments with serializers of their generic types
        argSerializers.forEach { (argType, argSerializer) ->
            assert(stackValueSerializerInstance(codegen, module, argType, argSerializer, this))
            // wrap into nullable serializer if argType is nullable
            if (argType.isMarkedNullable) {
                invokestatic("kotlinx/serialization/internal/BuiltinSerializersKt", "makeNullable",
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


class SerialTypeInfo(
        val property: SerializableProperty,
        val type: Type,
        val nn: String,
        val serializer: ClassDescriptor? = null,
        val unit: Boolean = false
)

fun getSerialTypeInfo(property: SerializableProperty, type: Type): SerialTypeInfo {
    when (type.sort) {
        BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR -> {
            val name = type.className
            return SerialTypeInfo(property, type, Character.toUpperCase(name[0]) + name.substring(1))
        }
        ARRAY -> {
            // check for explicit serialization annotation on this property
            var serializer = property.serializer.toClassDescriptor
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
            return SerialTypeInfo(property, Type.getType("Ljava/lang/Object;"),
                                  if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
        OBJECT -> {
            // no explicit serializer for this property. Check other built in types
            if (KotlinBuiltIns.isString(property.type))
                return SerialTypeInfo(property, Type.getType("Ljava/lang/String;"), "String")
            if (KotlinBuiltIns.isUnit(property.type))
                return SerialTypeInfo(property, Type.getType("Lkotlin/Unit;"), "Unit", unit = true)
            // todo: more efficient enum support here, but only for enums that don't define custom serializer
            // otherwise, it is a serializer for some other type
            val serializer = findTypeSerializer(property.module, property.type, type)
            return SerialTypeInfo(property, Type.getType("Ljava/lang/Object;"),
                                  if (property.type.isMarkedNullable) "Nullable" else "", serializer)
        }
        else -> throw AssertionError("Unexpected sort  for $type") // should not happen
    }
}

fun findTypeSerializer(module: ModuleDescriptor, kType: KotlinType, asmType: Type): ClassDescriptor? {
    return if (kType.requiresPolymorphism()) findPolymorphicSerializer(module)
    else kType.typeSerializer.toClassDescriptor // check for serializer defined on the type
         ?: findStandardAsmTypeSerializer(module, asmType) // otherwise see if there is a standard serializer
         ?: findStandardKotlinTypeSerializer(module, kType)
}

fun KotlinType.requiresPolymorphism(): Boolean {
    return this.toClassDescriptor?.getSuperClassNotAny()?.isInternalSerializable == true
           || this.toClassDescriptor?.modality == Modality.OPEN
           || this.containsTypeProjectionsInTopLevelArguments() // List<*>
}

fun findPolymorphicSerializer(module: ModuleDescriptor): ClassDescriptor {
    return requireNotNull(module.findClassAcrossModuleDependencies(polymorphicSerializerId)) { "Can't locate polymorphic serializer definition" }
}

fun findStandardKotlinTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val classDescriptor = kType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    return if (classDescriptor.kind == ClassKind.ENUM_CLASS) module.findClassAcrossModuleDependencies(enumSerializerId) else null
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
    else -> null
}
