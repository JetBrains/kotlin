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

package kotlinx.cinterop

import java.util.concurrent.ConcurrentHashMap
import java.util.function.LongConsumer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.reflect

internal fun createStablePointer(any: Any): COpaquePointer = newGlobalRef(any).toCPointer()!!

internal fun disposeStablePointer(pointer: COpaquePointer) = deleteGlobalRef(pointer.toLong())

@PublishedApi
internal fun derefStablePointer(pointer: COpaquePointer): Any = derefGlobalRef(pointer.toLong())

private fun getFieldCType(type: KType): CType<*> {
    val classifier = type.classifier
    if (classifier is KClass<*> && classifier.isSubclassOf(CStructVar::class)) {
        return getStructCType(classifier)
    }

    return getArgOrRetValCType(type)
}

private fun getVariableCType(type: KType): CType<*>? {
    val classifier = type.classifier
    return when (classifier) {
        !is KClass<*> -> null
        ByteVarOf::class -> SInt8
        ShortVarOf::class -> SInt16
        IntVarOf::class -> SInt32
        LongVarOf::class -> SInt64
        CPointerVarOf::class -> Pointer
        // TODO: floats, enums.
        else -> if (classifier.isSubclassOf(CStructVar::class)) {
            getStructCType(classifier)
        } else {
            null
        }
    }
}

private val structTypeCache = ConcurrentHashMap<Class<*>, CType<*>>()

private fun getStructCType(structClass: KClass<*>): CType<*> = structTypeCache.computeIfAbsent(structClass.java) {
    // Note that struct classes are not supposed to be user-defined,
    // so they don't require to be checked strictly.

    val annotations = structClass.annotations
    val cNaturalStruct = annotations.filterIsInstance<CNaturalStruct>().firstOrNull() ?:
            error("struct ${structClass.simpleName} has custom layout")

    val propertiesByName = structClass.declaredMemberProperties.groupBy { it.name }

    val fields = cNaturalStruct.fieldNames.map {
        propertiesByName[it]!!.single()
    }

    val fieldCTypes = mutableListOf<CType<*>>()

    for (field in fields) {
        val lengthAnnotation = field.annotations.filterIsInstance<CLength>().firstOrNull()
        if (lengthAnnotation == null) {
            val fieldType = getFieldCType(field.returnType)
            fieldCTypes.add(fieldType)
        } else {
            assert(field.returnType.classifier == CPointer::class)
            val length = lengthAnnotation.value
            if (length != 0) {
                val pointed = field.returnType.arguments.single().type!!
                val pointedCType = getVariableCType(pointed) ?: TODO("array element type '$pointed'")

                // Represent array field as repeated element-typed fields:
                repeat(length) {
                    fieldCTypes.add(pointedCType)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    val structType = structClass.companionObjectInstance as CVariable.Type

    Struct(structType.size, structType.align, fieldCTypes)
}

private fun getStructValueCType(type: KType): CType<*> {
    val structClass = type.arguments.singleOrNull()?.type?.classifier as? KClass<*> ?:
            error("'$type' type is incomplete")

    return getStructCType(structClass)
}

private fun getEnumCType(classifier: KClass<*>): CEnumType? {
    val rawValueType = classifier.declaredMemberProperties.single().returnType

    val rawValueCType = when (rawValueType.classifier) {
        Byte::class -> SInt8
        Short::class -> SInt16
        Int::class -> SInt32
        Long::class -> SInt64
        else -> error("'${classifier.simpleName}' has unexpected value type '$rawValueType'")
    }

    @Suppress("UNCHECKED_CAST")
    return CEnumType(rawValueCType as CType<Any>)
}

private fun getArgOrRetValCType(type: KType): CType<*> {
    val classifier = type.classifier

    val result = when (classifier) {
        !is KClass<*> -> null
        Unit::class -> Void
        Byte::class -> SInt8
        Short::class -> SInt16
        Int::class -> SInt32
        Long::class -> SInt64
        CPointer::class -> Pointer
        // TODO: floats
        CValue::class -> getStructValueCType(type)
        else -> if (classifier.isSubclassOf(@Suppress("DEPRECATION") CEnum::class)) {
            getEnumCType(classifier)
        } else {
            null
        }
    } ?: error("$type is not supported in callback signature")

    if (type.isMarkedNullable != (classifier == CPointer::class)) {
        if (type.isMarkedNullable) {
            error("$type must not be nullable when used in callback signature")
        } else {
            error("$type must be nullable when used in callback signature")
        }
    }

    return result
}

private fun createStaticCFunction(function: Function<*>): CPointer<CFunction<*>> {
    val errorMessage = "staticCFunction must take an unbound, non-capturing function"

    if (!isStatic(function)) {
        throw IllegalArgumentException(errorMessage)
    }

    val kFunction = function as? KFunction<*> ?: function.reflect() ?:
            throw IllegalArgumentException(errorMessage)

    val returnType = getArgOrRetValCType(kFunction.returnType)
    val paramTypes = kFunction.parameters.map { getArgOrRetValCType(it.type) }

    @Suppress("UNCHECKED_CAST")
    return interpretCPointer(createStaticCFunctionImpl(returnType as CType<Any?>, paramTypes, function))!!
}

/**
 * Returns `true` if given function is *static* as defined in [staticCFunction].
 */
private fun isStatic(function: Function<*>): Boolean {
    // TODO: revise
    try {
        with(function.javaClass.getDeclaredField("INSTANCE")) {
            if (!java.lang.reflect.Modifier.isStatic(modifiers) || !java.lang.reflect.Modifier.isFinal(modifiers)) {
                return false
            }

            isAccessible = true // TODO: undo

            return get(null) == function

            // If the class has static final "INSTANCE" field, and only the value of this field is accepted,
            // then each class is handled at most once, so these checks prevent memory leaks.
        }
    } catch (e: NoSuchFieldException) {
        return false
    }
}

private val createdStaticFunctions = ConcurrentHashMap<Class<*>, CPointer<CFunction<*>>>()

@Suppress("UNCHECKED_CAST")
internal fun <F : Function<*>> staticCFunctionImpl(function: F) =
        createdStaticFunctions.computeIfAbsent(function.javaClass) {
            createStaticCFunction(function)
        } as CPointer<CFunction<F>>

private val invokeMethods = (0 .. 22).map { arity ->
    Class.forName("kotlin.jvm.functions.Function$arity").getMethod("invoke",
            *Array<Class<*>>(arity) { java.lang.Object::class.java })
}

private fun createStaticCFunctionImpl(
        returnType: CType<Any?>,
        paramTypes: List<CType<*>>,
        function: Function<*>
): NativePtr {
    val ffiCif = ffiCreateCif(returnType.ffiType, paramTypes.map { it.ffiType })

    val arity = paramTypes.size
    val pt = paramTypes.toTypedArray()

    @Suppress("UNCHECKED_CAST")
    val impl: FfiClosureImpl = when (arity) {
        0 -> {
            val f = function as () -> Any?
            ffiClosureImpl(returnType) { _ ->
                f()
            }
        }
        1 -> {
            val f = function as (Any?) -> Any?
            ffiClosureImpl(returnType) { args ->
                f(pt.read(args, 0))
            }
        }
        2 -> {
            val f = function as (Any?, Any?) -> Any?
            ffiClosureImpl(returnType) { args ->
                f(pt.read(args, 0), pt.read(args, 1))
            }
        }
        3 -> {
            val f = function as (Any?, Any?, Any?) -> Any?
            ffiClosureImpl(returnType) { args ->
                f(pt.read(args, 0), pt.read(args, 1), pt.read(args, 2))
            }
        }
        4 -> {
            val f = function as (Any?, Any?, Any?, Any?) -> Any?
            ffiClosureImpl(returnType) { args ->
                f(pt.read(args, 0), pt.read(args, 1), pt.read(args, 2), pt.read(args, 3))
            }
        }
        5 -> {
            val f = function as (Any?, Any?, Any?, Any?, Any?) -> Any?
            ffiClosureImpl(returnType) { args ->
                f(pt.read(args, 0), pt.read(args, 1), pt.read(args, 2), pt.read(args, 3), pt.read(args, 4))
            }
        }
        else -> {
            val invokeMethod = invokeMethods[arity]
            ffiClosureImpl(returnType) { args ->
                val arguments = Array(arity) { pt.read(args, it) }
                invokeMethod.invoke(function, *arguments)
            }
        }
    }
    return ffiCreateClosure(ffiCif, impl)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Array<CType<*>>.read(args: CArrayPointer<COpaquePointerVar>, index: Int) =
    this[index].read(args[index].rawValue)

private inline fun ffiClosureImpl(
        returnType: CType<Any?>,
        crossinline invoke: (args: CArrayPointer<COpaquePointerVar>) -> Any?
): FfiClosureImpl {
    // Called through [ffi_fun] when a native function created with [ffiCreateClosure] is invoked.
    return LongConsumer {  retAndArgsRaw ->
        val retAndArgs = retAndArgsRaw.toCPointer<CPointerVar<*>>()!!

        // Pointer to memory to be filled with return value of the invoked native function:
        val ret = retAndArgs[0]!!

        // Pointer to array of pointers to arguments passed to the invoked native function:
        val args = retAndArgs[1]!!.reinterpret<COpaquePointerVar>()

        val result = invoke(args)

        returnType.write(ret.rawValue, result)
    }
}

/**
 * Describes the bridge between Kotlin type `T` and the corresponding C type of a function's parameter or return value.
 * It is supposed to be constructed using the primitive types (such as [SInt32]), the [Struct] combinator
 * and the [CEnumType] wrapper.
 *
 * This description omits the details that are irrelevant for the ABI.
 */
private abstract class CType<T> internal constructor(val ffiType: ffi_type) {
    internal constructor(ffiTypePtr: Long) : this(interpretPointed<ffi_type>(ffiTypePtr))
    abstract fun read(location: NativePtr): T
    abstract fun write(location: NativePtr, value: T): Unit
}

private object Void : CType<Any?>(ffiTypeVoid()) {
    override fun read(location: NativePtr) = throw UnsupportedOperationException()

    override fun write(location: NativePtr, value: Any?) {
        // nothing to do.
    }
}

private object SInt8 : CType<Byte>(ffiTypeSInt8()) {
    override fun read(location: NativePtr) = interpretPointed<ByteVar>(location).value
    override fun write(location: NativePtr, value: Byte) {
        interpretPointed<ByteVar>(location).value = value
    }
}

private object SInt16 : CType<Short>(ffiTypeSInt16()) {
    override fun read(location: NativePtr) = interpretPointed<ShortVar>(location).value
    override fun write(location: NativePtr, value: Short) {
        interpretPointed<ShortVar>(location).value = value
    }
}

private object SInt32 : CType<Int>(ffiTypeSInt32()) {
    override fun read(location: NativePtr) = interpretPointed<IntVar>(location).value
    override fun write(location: NativePtr, value: Int) {
        interpretPointed<IntVar>(location).value = value
    }
}

private object SInt64 : CType<Long>(ffiTypeSInt64()) {
    override fun read(location: NativePtr) = interpretPointed<LongVar>(location).value
    override fun write(location: NativePtr, value: Long) {
        interpretPointed<LongVar>(location).value = value
    }
}
private object Pointer : CType<CPointer<*>?>(ffiTypePointer()) {
    override fun read(location: NativePtr) = interpretPointed<CPointerVar<*>>(location).value
    override fun write(location: NativePtr, value: CPointer<*>?) {
        interpretPointed<CPointerVar<*>>(location).value = value
    }
}

private class Struct(val size: Long, val align: Int, elementTypes: List<CType<*>>) : CType<CValue<*>>(
        ffiTypeStruct(
                elementTypes.map { it.ffiType }
        )
) {
    override fun read(location: NativePtr) = interpretPointed<ByteVar>(location).readValue<CStructVar>(size, align)

    override fun write(location: NativePtr, value: CValue<*>) = value.write(location)
}

@Suppress("DEPRECATION")
private class CEnumType(private val rawValueCType: CType<Any>) : CType<CEnum>(rawValueCType.ffiType) {

    override fun read(location: NativePtr): CEnum {
        TODO("enum-typed callback parameters")
    }

    override fun write(location: NativePtr, value: CEnum) {
        rawValueCType.write(location, value.value)
    }
}

private typealias FfiClosureImpl = LongConsumer
private typealias UserData = FfiClosureImpl

private val topLevelInitializer = loadKonanLibrary("callbacks")


/**
 * Reference to `ffi_type` struct instance.
 */
internal class ffi_type(rawPtr: NativePtr) : COpaque(rawPtr)

/**
 * Reference to `ffi_cif` struct instance.
 */
internal class ffi_cif(rawPtr: NativePtr) : COpaque(rawPtr)

private external fun ffiTypeVoid(): Long
private external fun ffiTypeUInt8(): Long
private external fun ffiTypeSInt8(): Long
private external fun ffiTypeUInt16(): Long
private external fun ffiTypeSInt16(): Long
private external fun ffiTypeUInt32(): Long
private external fun ffiTypeSInt32(): Long
private external fun ffiTypeUInt64(): Long
private external fun ffiTypeSInt64(): Long
private external fun ffiTypePointer(): Long

private external fun ffiTypeStruct0(elements: Long): Long

/**
 * Allocates and initializes `ffi_type` describing the struct.
 *
 * @param elements types of the struct elements
 */
private fun ffiTypeStruct(elementTypes: List<ffi_type>): ffi_type {
    val elements = nativeHeap.allocArrayOfPointersTo(*elementTypes.toTypedArray(), null)
    val res = ffiTypeStruct0(elements.rawValue)
    if (res == 0L) {
        throw OutOfMemoryError()
    }
    return interpretPointed(res)
}

private external fun ffiCreateCif0(nArgs: Int, rType: Long, argTypes: Long): Long

/**
 * Creates and prepares an `ffi_cif`.
 *
 * @param returnType native function return value type
 * @param paramTypes native function parameter types
 *
 * @return the initialized `ffi_cif`
 */
private fun ffiCreateCif(returnType: ffi_type, paramTypes: List<ffi_type>): ffi_cif {
    val nArgs = paramTypes.size
    val argTypes = nativeHeap.allocArrayOfPointersTo(*paramTypes.toTypedArray(), null)
    val res = ffiCreateCif0(nArgs, returnType.rawPtr, argTypes.rawValue)

    when (res) {
        0L -> throw OutOfMemoryError()
        -1L -> throw Error("FFI_BAD_TYPEDEF")
        -2L -> throw Error("FFI_BAD_ABI")
        -3L -> throw Error("libffi error occurred")
    }

    return interpretPointed(res)
}

private external fun ffiCreateClosure0(ffiCif: Long, userData: Any): Long

/**
 * Uses libffi to allocate a native function which will call [impl] when invoked.
 *
 * @param ffiCif describes the type of the function to create
 */
private fun ffiCreateClosure(ffiCif: ffi_cif, impl: FfiClosureImpl): NativePtr {
    val res = ffiCreateClosure0(ffiCif.rawPtr, userData = impl)

    when (res) {
        0L -> throw OutOfMemoryError()
        -1L -> throw Error("libffi error occurred")
    }

    return res
}

private external fun newGlobalRef(any: Any): Long
private external fun derefGlobalRef(ref: Long): Any
private external fun deleteGlobalRef(ref: Long)