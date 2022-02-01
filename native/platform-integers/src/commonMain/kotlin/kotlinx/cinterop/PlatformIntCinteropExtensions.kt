@file:Suppress(
    "NO_ACTUAL_FOR_EXPECT",
    "FINAL_UPPER_BOUND",
    "unused",
    "PHANTOM_CLASSIFIER",
    "LEAKING_PHANTOM_TYPE",
    "LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS"
)

package kotlinx.cinterop

expect class PlatformIntVarOf<T : PlatformInt> : CVariable
expect class PlatformUIntVarOf<T : PlatformUInt> : CVariable

expect inline fun <reified R : Any> PlatformInt.convert(): R
expect inline fun <reified R : Any> PlatformUInt.convert(): R

expect var <T : PlatformInt> PlatformIntVarOf<T>.value: T
expect var <T : PlatformUInt> PlatformUIntVarOf<T>.value: T

expect inline operator fun <T : PlatformIntVarOf<*>> CPointer<T>.plus(index: Int): CPointer<T>?
expect inline operator fun <T : PlatformIntVarOf<*>> CPointer<T>.plus(index: Long): CPointer<T>?
expect inline operator fun <T : PlatformUIntVarOf<*>> CPointer<T>.plus(index: Int): CPointer<T>?
expect inline operator fun <T : PlatformUIntVarOf<*>> CPointer<T>.plus(index: Long): CPointer<T>?

expect inline operator fun <T : PlatformInt> CPointer<PlatformIntVarOf<T>>.get(index: Int): T
expect inline operator fun <T : PlatformInt> CPointer<PlatformIntVarOf<T>>.get(index: Long): T
expect inline operator fun <T : PlatformUInt> CPointer<PlatformUIntVarOf<T>>.get(index: Int): T
expect inline operator fun <T : PlatformUInt> CPointer<PlatformUIntVarOf<T>>.get(index: Long): T

expect inline operator fun <T : PlatformInt> CPointer<PlatformIntVarOf<T>>.set(index: Int, value: T)
expect inline operator fun <T : PlatformInt> CPointer<PlatformIntVarOf<T>>.set(index: Long, value: T)
expect inline operator fun <T : PlatformUInt> CPointer<PlatformUIntVarOf<T>>.set(index: Int, value: T)
expect inline operator fun <T : PlatformUInt> CPointer<PlatformUIntVarOf<T>>.set(index: Long, value: T)

expect fun cValuesOf(vararg elements: PlatformInt): CValues<PlatformIntVarOf<PlatformInt>>
@Suppress("FORBIDDEN_VARARG_PARAMETER_TYPE")
expect fun cValuesOf(vararg elements: PlatformUInt): CValues<PlatformUIntVarOf<PlatformUInt>>
