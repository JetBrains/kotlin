import kotlin.test.*
import kotlin.experimental.*
import kotlin.native.concurrent.*
import kotlin.native.ref.*
import kotlin.native.runtime.*
import kotlinx.cinterop.*

@ExperimentalForeignApi
class Data {
    // Primitives
    var boolean = false
    var char = 'a'
    var byte = 0.toByte()
    var short = 0.toShort()
    var int = 0
    var long = 0L
    var float = 0f
    var double = 0.0
    var intVector = vectorOf(0, 1, 2, 3)
    var floatVector = vectorOf(0f, 1f, 2f, 3f)
    var nativePtr = 128L.toCPointer<CPointed>().rawValue

    // Nullables
    var booleanOrNull: Boolean? = false
    var charOrNull: Char? = 'a'
    var byteOrNull: Byte? = 0.toByte()
    var shortOrNull: Short? = 0.toShort()
    var intOrNull: Int? = 0
    var lonOrNull: Long? = 0L
    var floatOrNull: Float? = 0f
    var doubleOrNull: Double? = 0.0
    var intVectorOrNull: Vector128? = vectorOf(0, 1, 2, 3)
    var floatVectorOrNull: Vector128? = vectorOf(0f, 1f, 2f, 3f)
    var nativePtrOrNull: NativePtr? = 128L.toCPointer<CPointed>().rawValue

    // Arrays
    var booleanArray = booleanArrayOf(false, true)
    var charArray = charArrayOf('a', 'b')
    var byteArray = byteArrayOf(0, 1)
    var shortArray = shortArrayOf(0, 1)
    var intArray = intArrayOf(0, 1)
    var longArray = longArrayOf(0L, 1L)
    var floatArray = floatArrayOf(0f, 1f)
    var doubleArray = doubleArrayOf(0.0, 1.0)
    var string = "foo"
    var array = arrayOf("foo", "bar")

    // Objects
    var any = Any()
    var anyObject = object : Any() {}
}

@ExperimentalForeignApi
val global = Data()

@ExperimentalForeignApi
@ThreadLocal
val threadLocal = Data()

@ExperimentalForeignApi
@ExperimentalNativeApi
val weakReference = WeakReference(Data())

@Test
@OptIn(ExperimentalNativeApi::class, NativeRuntimeApi::class, ExperimentalForeignApi::class)
fun dumpToStdOut() {
    val local = Data()
    assertTrue(Debugging.dumpMemory(1))
}
