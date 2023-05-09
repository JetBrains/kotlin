
package kotlinx.cinterop.internal

import kotlin.native.internal.InternalForKotlinNative

@InternalForKotlinNative
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class CStruct(val spelling: String) {
    @Retention(AnnotationRetention.BINARY)
    @Target(
            AnnotationTarget.PROPERTY_GETTER,
            AnnotationTarget.PROPERTY_SETTER
    )
    annotation class MemberAt(val offset: Long)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY_GETTER)
    annotation class ArrayMemberAt(val offset: Long)

    @Retention(AnnotationRetention.BINARY)
    @Target(
            AnnotationTarget.PROPERTY_GETTER,
            AnnotationTarget.PROPERTY_SETTER
    )
    annotation class BitField(val offset: Long, val size: Int)

    @Retention(AnnotationRetention.BINARY)
    annotation class VarType(val size: Long, val align: Int)

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    annotation class CPlusPlusClass

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    annotation class ManagedType
}

@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@InternalForKotlinNative
@Retention(AnnotationRetention.BINARY)
public annotation class CCall(val id: String) {
    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.BINARY)
    annotation class CString

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.BINARY)
    annotation class WCString

    @Target(
            AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER,
            AnnotationTarget.PROPERTY_SETTER
    )
    @Retention(AnnotationRetention.BINARY)
    annotation class ReturnsRetained

    @Target(
            AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER,
            AnnotationTarget.PROPERTY_SETTER
    )
    @Retention(AnnotationRetention.BINARY)
    annotation class ConsumesReceiver

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.BINARY)
    annotation class Consumed

    @Target(AnnotationTarget.CONSTRUCTOR)
    @Retention(AnnotationRetention.BINARY)
    annotation class CppClassConstructor
}

/**
 * Collection of annotations that allow to store
 * constant values.
 */
@InternalForKotlinNative
public object ConstantValue {
    @Retention(AnnotationRetention.BINARY)
    annotation class Byte(val value: kotlin.Byte)
    @Retention(AnnotationRetention.BINARY)
    annotation class Short(val value: kotlin.Short)
    @Retention(AnnotationRetention.BINARY)
    annotation class Int(val value: kotlin.Int)
    @Retention(AnnotationRetention.BINARY)
    annotation class Long(val value: kotlin.Long)
    @Retention(AnnotationRetention.BINARY)
    annotation class UByte(val value: kotlin.UByte)
    @Retention(AnnotationRetention.BINARY)
    annotation class UShort(val value: kotlin.UShort)
    @Retention(AnnotationRetention.BINARY)
    annotation class UInt(val value: kotlin.UInt)
    @Retention(AnnotationRetention.BINARY)
    annotation class ULong(val value: kotlin.ULong)
    @Retention(AnnotationRetention.BINARY)
    annotation class Float(val value: kotlin.Float)
    @Retention(AnnotationRetention.BINARY)
    annotation class Double(val value: kotlin.Double)
    @Retention(AnnotationRetention.BINARY)
    annotation class String(val value: kotlin.String)
}

/**
 * Denotes property that is an alias to some enum entry.
 */
@Target(AnnotationTarget.CLASS)
@InternalForKotlinNative
@Retention(AnnotationRetention.BINARY)
public annotation class CEnumEntryAlias(val entryName: String)

/**
 * Stores instance size of the type T: CEnumVar.
 */
@Target(AnnotationTarget.CLASS)
@InternalForKotlinNative
@Retention(AnnotationRetention.BINARY)
public annotation class CEnumVarTypeSize(val size: Int)
