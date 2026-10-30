// MODULE: main
// FILE: example/Types.kt
package example

typealias Names = List<String?>

interface Types<T> {
    val alias: Names

    val any: Any

    val array: Array<String?>

    val boolean: Boolean

    val byte: Byte

    val char: Char

    fun definitelyNotNull(value: T & Any): T & Any

    val double: Double

    val float: Float

    val function: (T) -> String

    val int: Int

    val long: Long

    val nested: Map<String, List<T?>>

    val nothing: Nothing

    val nullable: String?

    val nullableFunction: ((T) -> String)?

    val nullableNothing: Nothing?

    val nullableParameter: T?

    val parameter: T

    val primitiveArray: IntArray

    val projections: MutableMap<in String, out List<*>>

    val short: Short

    val star: List<*>

    val string: String

    val unit: Unit
}
