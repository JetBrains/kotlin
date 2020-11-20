// this is to avoid missing Kotlin/Native stdlib
package kotlinx.cinterop

// fake classes with the default constructor and no member scope
abstract class CStructVar
class CPointer<T>
@Suppress("FINAL_UPPER_BOUND") class UByteVarOf<T : UByte>
class UByte

// fake typealiases
typealias CArrayPointer<T> = CPointer<T>
typealias UByteVar = UByteVarOf<UByte>
