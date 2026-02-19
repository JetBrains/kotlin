
// SNIPPET
import kotlin.reflect.KProperty

class CustomDelegate<T>(private var value: T) { operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value }

interface _DataFrameType { }

// SNIPPET

val List<_DataFrameType>.name: String by CustomDelegate("Hello")
val List<_DataFrameType>.age: Int by CustomDelegate(42)

val r1 = listOf<_DataFrameType>().name

// EXPECTED: r1 == "Hello"

// SNIPPET

val r2 = listOf<_DataFrameType>().age

// EXPECTED: r2 == 42
