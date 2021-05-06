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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

interface DeclarationMapper {
    fun getKotlinClassForPointed(structDecl: StructDecl): Classifier
    fun isMappedToStrict(enumDef: EnumDef): Boolean
    fun getKotlinNameForValue(enumDef: EnumDef): String
    fun getPackageFor(declaration: TypeDeclaration): String

    val useUnsignedTypes: Boolean

    fun getKotlinClassForManaged(structDecl: StructDecl): Classifier
}

fun DeclarationMapper.isMappedToSigned(integerType: IntegerType): Boolean = integerType.isSigned || !useUnsignedTypes

fun DeclarationMapper.getKotlinClassFor(
        objCClassOrProtocol: ObjCClassOrProtocol,
        isMeta: Boolean = false
): Classifier {
    val pkg = if (objCClassOrProtocol.isForwardDeclaration) {
        when (objCClassOrProtocol) {
            is ObjCClass -> "objcnames.classes"
            is ObjCProtocol -> "objcnames.protocols"
        }
    } else {
        this.getPackageFor(objCClassOrProtocol)
    }
    val className = objCClassOrProtocol.kotlinClassName(isMeta)
    return Classifier.topLevel(pkg, className)
}

fun PrimitiveType.getKotlinType(declarationMapper: DeclarationMapper): KotlinClassifierType = when (this) {
    is CharType -> KotlinTypes.byte

    is BoolType -> KotlinTypes.boolean

// TODO: C primitive types should probably be generated as type aliases for Kotlin types.
    is IntegerType -> if (declarationMapper.isMappedToSigned(this)) {
        when (this.size) {
            1 -> KotlinTypes.byte
            2 -> KotlinTypes.short
            4 -> KotlinTypes.int
            8 -> KotlinTypes.long
            else -> TODO(this.toString())
        }
    } else {
        when (this.size) {
            1 -> KotlinTypes.uByte
            2 -> KotlinTypes.uShort
            4 -> KotlinTypes.uInt
            8 -> KotlinTypes.uLong
            else -> TODO(this.toString())
        }
    }

    is FloatingType -> when (this.size) {
        4 -> KotlinTypes.float
        8 -> KotlinTypes.double
        else -> TODO(this.toString())
    }

    is VectorType -> {
        /// @todo assert elementType and size here
        KotlinTypes.vector128
    }
    else -> throw NotImplementedError()
}

private fun PrimitiveType.getBridgedType(declarationMapper: DeclarationMapper): BridgedType {
    val kotlinType = this.getKotlinType(declarationMapper)
    return BridgedType.values().single {
        it.kotlinType == kotlinType
    }
}

internal val ObjCPointer.isNullable: Boolean
    get() = this.nullability != ObjCPointer.Nullability.NonNull

/**
 * Describes the Kotlin types used to represent some C type.
 */
sealed class TypeMirror(val pointedType: KotlinClassifierType, val info: TypeInfo) {
    /**
     * Type to be used in bindings for argument or return value.
     */
    abstract val argType: KotlinType

    /**
     * Mirror for C type to be represented in Kotlin as by-value type.
     */
    class ByValue(
            pointedType: KotlinClassifierType,
            info: TypeInfo,
            val valueType: KotlinType,
            val nullable: Boolean = (info is TypeInfo.Pointer)
    ) : TypeMirror(pointedType, info) {

        override val argType: KotlinType
            get() = valueType.makeNullableAsSpecified(nullable)
    }

    /**
     * Mirror for C type to be represented in Kotlin as by-ref type.
     */
    class ByRef(pointedType: KotlinClassifierType, info: TypeInfo) : TypeMirror(pointedType, info) {
        override val argType: KotlinType get() = KotlinTypes.cValue.typeWith(pointedType)
    }
    /**
     * Mirror for C++ Managed type.
     */

    class Managed(
            pointedType: KotlinClassifierType,
            info: TypeInfo
    ) : TypeMirror(pointedType, info) {

        override val argType: KotlinType
            get() = KotlinTypes.cPointer.typeWith(pointedType)
    }
}

/**
 * Describes various type conversions for [TypeMirror].
 */
sealed class TypeInfo {
    /**
     * The conversion from [TypeMirror.argType] to [bridgedType].
     */
    abstract fun argToBridged(expr: KotlinExpression): KotlinExpression

    /**
     * The conversion from [bridgedType] to [TypeMirror.argType].
     */
    abstract fun argFromBridged(
            expr: KotlinExpression,
            scope: KotlinScope,
            nativeBacked: NativeBacked
    ): KotlinExpression

    abstract val bridgedType: BridgedType

    open fun cFromBridged(
            expr: NativeExpression,
            scope: NativeScope,
            nativeBacked: NativeBacked
    ): NativeExpression = expr

    open fun cToBridged(expr: NativeExpression): NativeExpression = expr

    /**
     * If this info is for [TypeMirror.ByValue], then this method describes how to
     * construct pointed-type from value type.
     */
    abstract fun constructPointedType(valueType: KotlinType): KotlinClassifierType

    class Primitive(override val bridgedType: BridgedType, val varClass: Classifier) : TypeInfo() {

        override fun argToBridged(expr: KotlinExpression) = expr
        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) = expr

        override fun constructPointedType(valueType: KotlinType) = varClass.typeWith(valueType)
    }

    class Boolean : TypeInfo() {
        override fun argToBridged(expr: KotlinExpression) = "$expr.toByte()"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                "$expr.toBoolean()"

        override val bridgedType: BridgedType get() = BridgedType.BYTE

        override fun cFromBridged(expr: NativeExpression, scope: NativeScope, nativeBacked: NativeBacked) =
                "($expr) ? 1 : 0"

        override fun cToBridged(expr: NativeExpression) = "($expr) ? 1 : 0"

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.booleanVarOf.typeWith(valueType)
    }

    class Enum(val clazz: Classifier, override val bridgedType: BridgedType) : TypeInfo() {
        override fun argToBridged(expr: KotlinExpression) = "$expr.value"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                scope.reference(clazz) + ".byValue($expr)"

        override fun constructPointedType(valueType: KotlinType) =
                clazz.nested("Var").type // TODO: improve

    }

    class Pointer(val pointee: KotlinType, val cPointee: Type) : TypeInfo() {
        override fun argToBridged(expr: String) = "$expr.rawValue"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                "interpretCPointer<${pointee.render(scope)}>($expr)"

        override val bridgedType: BridgedType
            get() = BridgedType.NATIVE_PTR

        override fun cFromBridged(expr: NativeExpression, scope: NativeScope, nativeBacked: NativeBacked) =
                "(${getPointerTypeStringRepresentation(cPointee)})$expr"

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.cPointerVarOf.typeWith(valueType)
    }

    class ObjCPointerInfo(val kotlinType: KotlinType, val type: ObjCPointer) : TypeInfo() {
        override fun argToBridged(expr: String) = "$expr.objcPtr()"

        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                "interpretObjCPointerOrNull<${kotlinType.render(scope)}>($expr)" +
                        if (type.isNullable) "" else "!!"

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        override fun constructPointedType(valueType: KotlinType) = KotlinTypes.objCObjectVar.typeWith(valueType)
    }

    class ObjCBlockPointerInfo(val kotlinType: KotlinFunctionType, val type: ObjCBlockPointer) : TypeInfo() {

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        // When passing Kotlin function as block pointer from Kotlin to native,
        // it first gets wrapped by a holder in [argToBridged],
        // and then converted to block in [cFromBridged].

        override fun argToBridged(expr: KotlinExpression): KotlinExpression = "createKotlinObjectHolder($expr)"

        override fun cFromBridged(
                expr: NativeExpression,
                scope: NativeScope,
                nativeBacked: NativeBacked
        ): NativeExpression {
            val mappingBridgeGenerator = scope.mappingBridgeGenerator

            val blockParameters = type.parameterTypes.mapIndexed { index, it ->
                "p$index" to it.getStringRepresentation()
            }.joinToString { "${it.second} ${it.first}" }

            val blockReturnType = type.returnType.getStringRepresentation()

            val kniFunction = "kniFunction"

            val codeBuilder = NativeCodeBuilder(scope)

            return buildString {
                append("({ ") // Statement expression begins.
                append("id $kniFunction = $expr; ") // Note: it gets captured below.
                append("($kniFunction == nil) ? nil : ")
                append("(id)") // Cast the block to `id`.
                append("^$blockReturnType($blockParameters) {") // Block begins.

                // As block body, generate the code which simply bridges to Kotlin and calls the Kotlin function:
                mappingBridgeGenerator.nativeToKotlin(
                        codeBuilder,
                        nativeBacked,
                        type.returnType,
                        type.parameterTypes.mapIndexed { index, it ->
                            TypedNativeValue(it, "p$index")
                        } + TypedNativeValue(ObjCIdType(ObjCPointer.Nullability.Nullable, emptyList()), kniFunction)
                ) { kotlinValues ->
                    val kotlinFunctionType = kotlinType.render(this.scope)
                    val kotlinFunction = "unwrapKotlinObjectHolder<$kotlinFunctionType>(${kotlinValues.last()})"
                    "$kotlinFunction(${kotlinValues.dropLast(1).joinToString()})"
                }.let {
                    codeBuilder.out("return $it;")
                }

                codeBuilder.lines.joinTo(this, separator = " ")

                append(" };") // Block ends.
                append(" })") // Statement expression ends.
            }
        }

        // When passing block pointer as Kotlin function from native to Kotlin,
        // it is converted to Kotlin function in [cFromBridged].

        override fun cToBridged(expr: NativeExpression): NativeExpression = expr

        override fun argFromBridged(
                expr: KotlinExpression,
                scope: KotlinScope,
                nativeBacked: NativeBacked
        ): KotlinExpression {
            val mappingBridgeGenerator = scope.mappingBridgeGenerator

            val funParameters = type.parameterTypes.mapIndexed { index, _ ->
                "p$index" to kotlinType.parameterTypes[index]
            }.joinToString { "${it.first}: ${it.second.render(scope)}" }

            val funReturnType = kotlinType.returnType.render(scope)

            val codeBuilder = KotlinCodeBuilder(scope)
            val kniBlockPtr = "kniBlockPtr"


            // Build the anonymous function expression:
            val anonymousFun = buildString {
                append("fun($funParameters): $funReturnType {\n") // Anonymous function begins.

                // As function body, generate the code which simply bridges to native and calls the block:
                mappingBridgeGenerator.kotlinToNative(
                        codeBuilder,
                        nativeBacked,
                        type.returnType,
                        type.parameterTypes.mapIndexed { index, it ->
                            TypedKotlinValue(it, "p$index")
                        } + TypedKotlinValue(PointerType(VoidType), "interpretCPointer<COpaque>($kniBlockPtr)"),
                        independent = true

                ) { nativeValues ->
                    val type = type
                    val blockType = blockTypeStringRepresentation(type)
                    val objCBlock = "((__bridge $blockType)${nativeValues.last()})"
                    "$objCBlock(${nativeValues.dropLast(1).joinToString()})"
                }.let {
                    codeBuilder.returnResult(it)
                }

                codeBuilder.build().joinTo(this, separator = "\n")
                append("}") // Anonymous function ends.
            }

            val nullOutput = if (type.isNullable) "null" else "throw NullPointerException()"

            return "$expr.let { $kniBlockPtr -> if (kniBlockPtr == nativeNullPtr) $nullOutput else $anonymousFun }"
        }

        override fun constructPointedType(valueType: KotlinType): KotlinClassifierType {
            return Classifier.topLevel("kotlinx.cinterop", "ObjCBlockVar").typeWith(valueType)
        }
    }

    class ByRef(val pointed: KotlinType) : TypeInfo() {
        override fun argToBridged(expr: String) = error(pointed)
        override fun argFromBridged(expr: KotlinExpression, scope: KotlinScope, nativeBacked: NativeBacked) =
                error(pointed)
        override val bridgedType: BridgedType get() = error(pointed)
        override fun cFromBridged(expr: NativeExpression, scope: NativeScope, nativeBacked: NativeBacked) =
                error(pointed)
        override fun cToBridged(expr: String) = error(pointed)

        // TODO: this method must not exist.
        override fun constructPointedType(valueType: KotlinType): KotlinClassifierType = error(pointed)
    }
}

fun mirrorPrimitiveType(type: PrimitiveType, declarationMapper: DeclarationMapper): TypeMirror.ByValue {
    val varClassName = when (type) {
        is CharType -> "ByteVar"
        is BoolType -> "BooleanVar"
        is IntegerType -> if (declarationMapper.isMappedToSigned(type)) {
            when (type.size) {
                1 -> "ByteVar"
                2 -> "ShortVar"
                4 -> "IntVar"
                8 -> "LongVar"
                else -> TODO(type.toString())
            }
        } else {
            when (type.size) {
                1 -> "UByteVar"
                2 -> "UShortVar"
                4 -> "UIntVar"
                8 -> "ULongVar"
                else -> TODO(type.toString())
            }
        }
        is FloatingType -> when (type.size) {
            4 -> "FloatVar"
            8 -> "DoubleVar"
            else -> TODO(type.toString())
        }
        is VectorType -> {
            "Vector128Var"
        }
        else -> TODO(type.toString())
    }

    val varClass = Classifier.topLevel("kotlinx.cinterop", varClassName)
    val varClassOf = Classifier.topLevel("kotlinx.cinterop", "${varClassName}Of")

    val info = if (type is BoolType) {
        TypeInfo.Boolean()
    } else {
        TypeInfo.Primitive(type.getBridgedType(declarationMapper), varClassOf)
    }
    return TypeMirror.ByValue(varClass.type, info, type.getKotlinType(declarationMapper))
}

private fun byRefTypeMirror(pointedType: KotlinClassifierType) : TypeMirror.ByRef {
    val info = TypeInfo.ByRef(pointedType)
    return TypeMirror.ByRef(pointedType, info)
}

private fun managedTypeMirror(pointedType: KotlinClassifierType) : TypeMirror.Managed {
    val info = TypeInfo.ByRef(pointedType) // These are all errors anyways.
    return TypeMirror.Managed(pointedType, info)
}

fun mirror(declarationMapper: DeclarationMapper, type: Type): TypeMirror = when (type) {
    is PrimitiveType -> mirrorPrimitiveType(type, declarationMapper)

    is RecordType -> byRefTypeMirror(declarationMapper.getKotlinClassForPointed(type.decl).type)

    is ManagedType -> managedTypeMirror(declarationMapper.getKotlinClassForManaged(type.decl).type)

    is EnumType -> {
        val pkg = declarationMapper.getPackageFor(type.def)
        val kotlinName = declarationMapper.getKotlinNameForValue(type.def)
                .let { mangleSimple(it) } // enum class requires additional mangling

        when {
            declarationMapper.isMappedToStrict(type.def) -> {
                val bridgedType = (type.def.baseType.unwrapTypedefs() as PrimitiveType).getBridgedType(declarationMapper)
                val clazz = Classifier.topLevel(pkg, kotlinName)
                val info = TypeInfo.Enum(clazz, bridgedType)
                TypeMirror.ByValue(clazz.nested("Var").type, info, clazz.type)
            }
            !type.def.isAnonymous -> {
                val baseTypeMirror = mirror(declarationMapper, type.def.baseType)
                TypeMirror.ByValue(
                        Classifier.topLevel(pkg, kotlinName + "Var").typeAbbreviation(baseTypeMirror.pointedType),
                        baseTypeMirror.info,
                        Classifier.topLevel(pkg, kotlinName).typeAbbreviation(baseTypeMirror.argType)
                )
            }
            else -> mirror(declarationMapper, type.def.baseType)
        }
    }

    is PointerType -> {
        val pointeeType = type.pointeeType
        val unwrappedPointeeType = pointeeType.unwrapTypedefs()
        if (unwrappedPointeeType is VoidType) {
            val info = TypeInfo.Pointer(KotlinTypes.cOpaque, pointeeType)
            TypeMirror.ByValue(KotlinTypes.cOpaquePointerVar, info, KotlinTypes.cOpaquePointer)
        } else if (unwrappedPointeeType is ArrayType) {
            mirror(declarationMapper, pointeeType)
        } else {
            val pointeeMirror = mirror(declarationMapper, pointeeType)
            val info = TypeInfo.Pointer(pointeeMirror.pointedType, pointeeType)
            TypeMirror.ByValue(
                    KotlinTypes.cPointerVar.typeWith(pointeeMirror.pointedType),
                    info,
                    KotlinTypes.cPointer.typeWith(pointeeMirror.pointedType)
            )
        }
    }

    is ArrayType -> {
        // TODO: array type doesn't exactly correspond neither to pointer nor to value.
        val elemTypeMirror = mirror(declarationMapper, type.elemType)
        if (type.elemType.unwrapTypedefs() is ArrayType) {
            elemTypeMirror
        } else {
            val info = TypeInfo.Pointer(elemTypeMirror.pointedType, type.elemType)
            TypeMirror.ByValue(
                    KotlinTypes.cArrayPointerVar.typeWith(elemTypeMirror.pointedType),
                    info,
                    KotlinTypes.cArrayPointer.typeWith(elemTypeMirror.pointedType)
            )
        }
    }

    is FunctionType -> byRefTypeMirror(KotlinTypes.cFunction.typeWith(getKotlinFunctionType(declarationMapper, type)))

    is Typedef -> {
        val baseType = mirror(declarationMapper, type.def.aliased)
        val pkg = declarationMapper.getPackageFor(type.def)

        val name = type.def.name
        when (baseType) {
            is TypeMirror.ByValue -> {
                val valueType = Classifier.topLevel(pkg, name).typeAbbreviation(baseType.valueType)
                val underlyingPointedType = if (baseType.info is TypeInfo.Pointer) {
                    KotlinTypes.cPointerVarOf.typeWith(valueType)
                } else {
                    baseType.pointedType
                }
                val pointedType = Classifier.topLevel(pkg, "${name}Var").typeAbbreviation(underlyingPointedType)
                TypeMirror.ByValue(
                        pointedType,
                        baseType.info,
                        valueType,
                        nullable = baseType.nullable)
            }

            is TypeMirror.ByRef -> TypeMirror.ByRef(
                    Classifier.topLevel(pkg, name).typeAbbreviation(baseType.pointedType),
                    baseType.info
            )

            is TypeMirror.Managed -> TypeMirror.Managed(
                    Classifier.topLevel(pkg, name).typeAbbreviation(baseType.pointedType),
                    baseType.info
            )
        }

    }

    is ObjCPointer -> objCPointerMirror(declarationMapper, type)

    else -> TODO(type.toString())
}

internal tailrec fun ObjCClass.isNSStringOrSubclass(): Boolean = when (this.name) {
    "NSMutableString", // fast path and handling for forward declarations.
    "NSString" -> true
    else -> {
        val baseClass = this.baseClass
        if (baseClass != null) {
            baseClass.isNSStringOrSubclass()
        } else {
            false
        }
    }
}

internal fun ObjCClass.isNSStringSubclass(): Boolean = this.baseClass?.isNSStringOrSubclass() == true

private fun objCPointerMirror(declarationMapper: DeclarationMapper, type: ObjCPointer): TypeMirror.ByValue {
    if (type is ObjCObjectPointer && type.def.isNSStringOrSubclass()) {
        val valueType = KotlinTypes.string
        return objCMirror(valueType, TypeInfo.ObjCPointerInfo(valueType, type), type.isNullable)
    }

    val valueType = when (type) {
        is ObjCIdType -> {
            type.protocols.firstOrNull()?.let { declarationMapper.getKotlinClassFor(it) }?.type
                    ?: KotlinTypes.any
        }
        is ObjCClassPointer -> KotlinTypes.objCClass.type
        is ObjCObjectPointer -> {
            when (type.def.name) {
                "NSArray" -> KotlinTypes.list.typeWith(StarProjection)
                "NSMutableArray" -> KotlinTypes.mutableList.typeWith(KotlinTypes.any.makeNullable())
                "NSSet" -> KotlinTypes.set.typeWith(StarProjection)
                "NSDictionary" -> KotlinTypes.map.typeWith(KotlinTypes.any.makeNullable(), StarProjection)
                else -> declarationMapper.getKotlinClassFor(type.def).type
            }
        }
        is ObjCInstanceType -> TODO(type.toString()) // Must have already been handled.
        is ObjCBlockPointer -> return objCBlockPointerMirror(declarationMapper, type)
    }

    return objCMirror(valueType, TypeInfo.ObjCPointerInfo(valueType, type), type.isNullable)
}

private fun objCBlockPointerMirror(declarationMapper: DeclarationMapper, type: ObjCBlockPointer): TypeMirror.ByValue {
    val returnType = if (type.returnType.unwrapTypedefs() is VoidType) {
        KotlinTypes.unit
    } else {
        mirror(declarationMapper, type.returnType).argType
    }
    val kotlinType = KotlinFunctionType(
            type.parameterTypes.map { mirror(declarationMapper, it).argType },
            returnType
    )

    val info = TypeInfo.ObjCBlockPointerInfo(kotlinType, type)
    return objCMirror(kotlinType, info, type.isNullable)
}

private fun objCMirror(valueType: KotlinType, info: TypeInfo, nullable: Boolean) = TypeMirror.ByValue(
        info.constructPointedType(valueType.makeNullableAsSpecified(nullable)),
        info,
        valueType.makeNullable(), // All typedefs to Objective-C pointers would be nullable for simplicity
        nullable
)

fun getKotlinFunctionType(declarationMapper: DeclarationMapper, type: FunctionType): KotlinFunctionType {
    val returnType = if (type.returnType.unwrapTypedefs() is VoidType) {
        KotlinTypes.unit
    } else {
        mirror(declarationMapper, type.returnType).argType
    }
    return KotlinFunctionType(
            type.parameterTypes.map { mirror(declarationMapper, it).argType },
            returnType,
            nullable = false
    )
}

